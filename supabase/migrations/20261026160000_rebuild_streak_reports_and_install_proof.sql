-- The original project (fhsrjyuzkrbfuvuzplvy) is gone, and with it everything
-- that had been applied to it by hand without landing in this directory:
-- reports, the 14-day streak, owner reciprocity and the column-limited UPDATE
-- on apps. This migration rebuilds all of it from what the client reads, and
-- adds what is new: proof that a tester really has the app installed, reads
-- restricted to signed-in users, stricter links and in-app account deletion.

-- ------------------------------------------------------------- read access
-- Nothing in the app is visible without signing in, and the anon key ships
-- inside the APK: left at "using (true)" anyone could dump profiles with curl.
alter policy profiles_select_all on public.profiles to authenticated;
alter policy apps_select_all on public.apps to authenticated;
alter policy app_testers_select_all on public.app_testers to authenticated;
alter policy app_comments_select_all on public.app_comments to authenticated;

-- And writes. A real anon token carries no sub, so the policies already refused
-- it, but they were written for every role and anon kept the table grants: the
-- RLS suite could create an app as anon with a forged claim.
revoke insert, update, delete on all tables in schema public from anon;
alter default privileges in schema public revoke insert, update, delete on tables from anon;
alter policy apps_insert_own_within_slots on public.apps to authenticated;
alter policy apps_update_own on public.apps to authenticated;
alter policy apps_delete_own on public.apps to authenticated;
alter policy apps_delete_admin on public.apps to authenticated;
alter policy app_testers_insert_self on public.app_testers to authenticated;
alter policy app_testers_delete_self on public.app_testers to authenticated;
alter policy app_comments_insert_own on public.app_comments to authenticated;
alter policy app_comments_delete_own on public.app_comments to authenticated;
alter policy app_comments_delete_admin on public.app_comments to authenticated;
alter policy profiles_update_own on public.profiles to authenticated;

-- ------------------------------------------------------------------- apps
-- Same shapes the client validates in Rules.kt. A bad opt-in link gives a 404
-- to everyone who opens it, so the database refuses it too instead of trusting
-- the form.
alter table public.apps
  drop constraint apps_google_groups_url_check,
  drop constraint apps_play_store_url_check,
  drop constraint apps_opt_in_url_check,
  add constraint apps_google_groups_url_check
    check (google_groups_url like 'https://groups.google.com/%'),
  add constraint apps_play_store_url_check
    check (play_store_url ~ '^https://play\.google\.com/store/apps/details\?(.*&)?id=[A-Za-z0-9_.]+'),
  add constraint apps_opt_in_url_check
    check (opt_in_url ~ '^https://play\.google\.com/apps/testing/[A-Za-z0-9_.]+');

-- The package is what the tester's phone is checked against. Derived, so it
-- can never disagree with the link people actually open.
alter table public.apps
  add column package_name text generated always as
    (substring(opt_in_url from '^https://play\.google\.com/apps/testing/([A-Za-z0-9_.]+)')) stored;

alter table public.apps
  add constraint apps_same_package
    check (substring(play_store_url from '[?&]id=([A-Za-z0-9_.]+)') = package_name);

-- Owners edit the name and the links, nothing else: not owner_id, not the
-- streak start below.
revoke update on public.apps from anon, authenticated;
grant update (name, google_groups_url, play_store_url, opt_in_url) on public.apps to authenticated;

-- So the client never sends owner_id at all: an edit that carried it, even
-- unchanged, is refused by the column grant above.
alter table public.apps alter column owner_id set default auth.uid();

-- ---------------------------------------------------------- 14-day streak
-- Play wants 12 testers held for 14 days in a row. full_since starts when the
-- count reaches 12 and clears as soon as it drops below, so the client only
-- needs the day number the view derives from it.
alter table public.apps add column full_since timestamptz;

-- SECURITY DEFINER: the row that fires this belongs to the tester, who has no
-- UPDATE right on somebody else's app.
create function public.sync_full_since()
returns trigger language plpgsql security definer set search_path = '' as $$
declare target uuid := coalesce(new.app_id, old.app_id);
begin
  update public.apps a
     set full_since = case
       when (select count(*) from public.app_testers t where t.app_id = target) >= 12
         then coalesce(a.full_since, now())
       end
   where a.id = target;
  return null;
end $$;
revoke execute on function public.sync_full_since() from public, anon, authenticated;

create trigger app_testers_sync_full_since
  after insert or delete on public.app_testers
  for each row execute function public.sync_full_since();

-- ------------------------------------------------------ proof of install
-- "I joined on Play" is a claim. The Android client checks that the package is
-- installed and that Play installed it (a closed test cannot be installed from
-- Play without the opt-in), and stamps the time here. The owner then sees who
-- really has the app, not just who pressed a button.
alter table public.app_testers add column verified_at timestamptz;
revoke update on public.app_testers from authenticated;

-- Only through this function, and always with the server clock: there is no
-- UPDATE grant on app_testers, so the timestamp cannot be forged by PATCH.
create function public.confirm_installed(app_ids uuid[])
returns void language sql security definer set search_path = '' as $$
  update public.app_testers set verified_at = now()
   where user_id = (select auth.uid()) and app_id = any(app_ids);
$$;
revoke execute on function public.confirm_installed(uuid[]) from public, anon;
grant execute on function public.confirm_installed(uuid[]) to authenticated;

-- Seen within the last three days counts as installed. Longer than that, the
-- tester either uninstalled or stopped opening Test4Test; either way the owner
-- should ask.
create view public.app_tester_status with (security_invoker = true) as
  select t.app_id, t.user_id, t.joined_at, t.verified_at,
         t.verified_at > now() - interval '3 days' as installed_recently
    from public.app_testers t;

-- ---------------------------------------------------------------- reports
-- Play policy for user-generated content: anyone can flag, admins review it
-- from the dashboard. Exactly one target per row, one report per person.
create table public.app_reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  app_id uuid references public.apps(id) on delete cascade,
  comment_id uuid references public.app_comments(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint app_reports_target check (num_nonnulls(app_id, comment_id) = 1),
  constraint app_reports_once unique nulls not distinct (reporter_id, app_id, comment_id)
);
create index app_reports_app_id_idx on public.app_reports(app_id);
create index app_reports_comment_id_idx on public.app_reports(comment_id);

alter table public.app_reports enable row level security;
create policy app_reports_insert_self on public.app_reports
  for insert to authenticated with check (reporter_id = (select auth.uid()));
create policy app_reports_select_admin on public.app_reports
  for select to authenticated using (public.is_admin());
create policy app_reports_delete_admin on public.app_reports
  for delete to authenticated using (public.is_admin());

-- -------------------------------------------------------- account removal
-- Play requires deleting the account from inside the app. Cascades through
-- profiles to apps, tester rows and comments. An active subscription is not
-- touched: Play owns it and the app says so before confirming.
create function public.delete_my_account()
returns void language sql security definer set search_path = '' as $$
  delete from auth.users where id = (select auth.uid());
$$;
revoke execute on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;

-- ----------------------------------------------------------- feed read model
-- Adds to the live tester count: how many of them were seen with the app
-- installed, the day of the streak in progress (1 on the day it starts, so
-- 14 days are complete only once it reads 15), and the owner's reciprocity.
-- The reciprocity bar is least(3, apps by other people) so a tiny network
-- does not demand more than exists.
drop view public.apps_with_followers;
create view public.apps_with_followers with (security_invoker = true) as
  select a.*,
         c.follower_count,
         c.verified_count,
         case when a.full_since is null then 0
              else (current_date - a.full_since::date) + 1 end as full_days,
         o.testing_count as owner_testing_count,
         o.testing_required as owner_testing_required,
         o.testing_count >= o.testing_required as owner_reciprocates
    from public.apps a
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
  'Feed read model: apps plus live tester count, installs seen, streak day and owner reciprocity. Keep security_invoker.';
