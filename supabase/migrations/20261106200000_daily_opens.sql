-- Daily round. Google turns production requests down with "testers were not
-- engaged": installing is not enough, it wants the app used during the 14 days.
-- The install proof only says the app is on the phone. This records the days a
-- tester opened it from Test4Test, so the owner sees who is really testing and
-- the tester has a reason to come back every day.

-- One row per tester, app and day. The day is the server's (UTC): a phone with
-- a wrong clock cannot invent a streak.
create table public.app_opens (
  app_id uuid not null references public.apps(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  day date not null default current_date,
  primary key (app_id, user_id, day)
);

alter table public.app_opens enable row level security;
-- Readable like app_testers, which is already public to signed-in users.
create policy app_opens_select_all on public.app_opens
  for select to authenticated using (true);
-- No write grants: only record_open() writes, with the server date.
revoke insert, update, delete on public.app_opens from anon, authenticated;

-- Only for a test you are in: an open of an app you never joined does not
-- count for anyone. A second open the same day changes nothing.
create function public.record_open(app uuid)
returns void language sql security definer set search_path = '' as $$
  insert into public.app_opens (app_id, user_id)
  select t.app_id, t.user_id from public.app_testers t
   where t.app_id = app and t.user_id = (select auth.uid())
  on conflict do nothing;
$$;
revoke execute on function public.record_open(uuid) from public, anon;
grant execute on function public.record_open(uuid) to authenticated;

-- The owner's list and the tester's own round read the same two facts: opened
-- today, and on how many of the last 14 days. New columns go last so 1.7
-- clients, which ignore them, keep working.
create or replace view public.app_tester_status with (security_invoker = true) as
  select t.app_id, t.user_id, t.joined_at, t.verified_at,
         t.verified_at > now() - interval '3 days' as installed_recently,
         exists (select 1 from public.app_opens o
                  where o.app_id = t.app_id and o.user_id = t.user_id and o.day = current_date) as opened_today,
         (select count(*) from public.app_opens o
           where o.app_id = t.app_id and o.user_id = t.user_id and o.day > current_date - 14)::int as open_days
    from public.app_testers t;
