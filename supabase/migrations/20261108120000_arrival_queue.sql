-- Arrival queue (1.10). The feed stops spreading testers thin ("fewest testers
-- first", where with a small network no app ever reached 12) and becomes a line:
-- every app still under 12 waits in order of arrival, so everyone pushes the one
-- at the head until it fills. Apps with 12 or more go after the whole line and
-- come back to their old place if a tester leaves.
--
-- Inside the line, subscribers go first (shown as "Featured"; the EU requires
-- paid ranking to be disclosed), then owners who test other people's apps, then
-- arrival. Whoever pays skips the reciprocity rule: that is what they pay for.
--
-- queue_rank orders every row; for an app under 12 it is also its place in the
-- line, because the full ones all rank after the last app waiting.
-- ponytail: an abandoned app keeps the head of the line until it fills; if that
-- happens, rank owners with no activity in 7 days after the rest.
drop view public.apps_with_followers;
create view public.apps_with_followers with (security_invoker = true) as
  select a.*,
         c.follower_count,
         c.verified_count,
         case when a.full_since is null then 0
              else (current_date - a.full_since::date) + 1 end as full_days,
         o.testing_count as owner_testing_count,
         o.testing_required as owner_testing_required,
         o.testing_count >= o.testing_required as owner_reciprocates,
         p.is_premium as owner_premium,
         row_number() over (
           order by c.follower_count >= 12,
                    p.is_premium desc,
                    o.testing_count >= o.testing_required desc,
                    a.created_at,
                    a.id
         )::int as queue_rank
    from public.apps a
    join public.profiles p on p.id = a.owner_id
    cross join lateral (
      select count(*) as follower_count,
             count(*) filter (where t.verified_at > now() - interval '3 days') as verified_count
        from public.app_testers t
       where t.app_id = a.id
    ) c
    cross join lateral (
      select (select count(*) from public.app_testers t
                join public.apps x on x.id = t.app_id
               where t.user_id = a.owner_id and x.owner_id <> a.owner_id)::int as testing_count,
             least(3, (select count(*) from public.apps x where x.owner_id <> a.owner_id))::int as testing_required
    ) o;

comment on view public.apps_with_followers is
  'Feed read model: apps plus live tester count, installs seen, streak day, owner reciprocity and place in the arrival queue. Keep security_invoker.';
