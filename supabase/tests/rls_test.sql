-- RLS regression suite. Runs entirely inside a transaction that is rolled back,
-- so it leaves no trace and is safe against any environment.
--
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/rls_test.sql
--
-- Every authorization rule in this app lives in RLS, because clients reach
-- PostgREST directly with the public anon key. If a policy regresses, this file
-- is what catches it.

begin;

-- Alice and Bob; the signup trigger creates their profiles.
insert into auth.users (id, instance_id, aud, role, email, raw_user_meta_data, created_at, updated_at)
values
  ('00000000-0000-4000-8000-00000000a11c', '00000000-0000-0000-0000-000000000000',
   'authenticated', 'authenticated', 'alice@test.invalid', '{"full_name":"Alice"}'::jsonb, now(), now()),
  ('00000000-0000-4000-8000-00000000b0b0', '00000000-0000-0000-0000-000000000000',
   'authenticated', 'authenticated', 'bob@test.invalid', '{"full_name":"Bob"}'::jsonb, now(), now());

-- RLS rejects INSERTs with an error but silently *filters* rows on UPDATE and
-- DELETE, so the row count matters as much as success.
create function pg_temp.as_role(who uuid, r text, stmt text)
returns text language plpgsql as $$
declare n integer;
begin
  execute format('set local role %I', r);
  execute format('set local request.jwt.claims = %L', json_build_object('sub', who)::text);
  execute stmt;
  get diagnostics n = row_count;
  execute 'reset role';
  return 'ALLOWED (' || n || ' rows)';
exception when others then
  execute 'reset role';
  return 'BLOCKED';
end $$;

create function pg_temp.as_user(who uuid, stmt text)
returns text language plpgsql as $$
begin return pg_temp.as_role(who, 'authenticated', stmt); end $$;

create temp table results (test text, got text, expected text);

insert into results values
  ('01 alice creates 1st app (free)', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000a11c','Alice App','https://groups.google.com/g/a','https://play.google.com/store/apps/details?id=com.alice','https://play.google.com/apps/testing/com.alice')$q$), 'ALLOWED (1 rows)'),
  ('02 alice creates 2nd app over the free slot limit', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a2','00000000-0000-4000-8000-00000000a11c','Alice App 2','https://groups.google.com/g/a2','https://play.google.com/store/apps/details?id=com.alice2','https://play.google.com/apps/testing/com.alice2')$q$), 'BLOCKED'),
  ('03 bob creates his 1st app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000b1','00000000-0000-4000-8000-00000000b0b0','Bob App','https://groups.google.com/g/b','https://play.google.com/store/apps/details?hl=es&id=com.bob','https://play.google.com/apps/testing/com.bob')$q$), 'ALLOWED (1 rows)'),
  ('04 bob creates an app owned by alice', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a3','00000000-0000-4000-8000-00000000a11c','Spoofed','https://groups.google.com/g/x','https://play.google.com/store/apps/details?id=com.x','https://play.google.com/apps/testing/com.x')$q$), 'BLOCKED'),
  ('05 non-https url', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set play_store_url = 'http://insecure' where id = '00000000-0000-4000-8000-0000000000b1'$q$), 'BLOCKED'),
  ('05b opt-in link outside Play testing', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set opt_in_url = 'https://example.com/com.bob' where id = '00000000-0000-4000-8000-0000000000b1'$q$), 'BLOCKED'),
  ('05c Play and opt-in links name different packages', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set opt_in_url = 'https://play.google.com/apps/testing/com.other' where id = '00000000-0000-4000-8000-0000000000b1'$q$), 'BLOCKED'),
  ('06 bob renames alice app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set name = 'Hacked' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'ALLOWED (0 rows)'),
  ('06b alice renames her own app', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$update public.apps set name = 'Alice App' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'ALLOWED (1 rows)'),
  ('07 bob deletes alice app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$delete from public.apps where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'ALLOWED (0 rows)'),
  ('08 bob steals alice app via owner_id', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set owner_id = '00000000-0000-4000-8000-00000000b0b0' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'BLOCKED'),
  ('08b an edit that carries owner_id, even unchanged', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$update public.apps set owner_id = '00000000-0000-4000-8000-00000000a11c', name = 'Same' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'BLOCKED'),
  ('08c alice backdates her own streak', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$update public.apps set full_since = now() - interval '20 days' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'BLOCKED'),
  ('09 bob gives his own app away', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set owner_id = '00000000-0000-4000-8000-00000000a11c' where id = '00000000-0000-4000-8000-0000000000b1'$q$), 'BLOCKED'),
  ('10 alice tests her own app', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$insert into public.app_testers (app_id, user_id) values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000a11c')$q$), 'BLOCKED'),
  ('11 bob tests alice app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_testers (app_id, user_id) values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000b0b0')$q$), 'ALLOWED (1 rows)'),
  ('12 bob tests alice app twice', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_testers (app_id, user_id) values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000b0b0')$q$), 'BLOCKED'),
  ('13 bob signs alice up as a tester', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_testers (app_id, user_id) values ('00000000-0000-4000-8000-0000000000b1','00000000-0000-4000-8000-00000000a11c')$q$), 'BLOCKED'),
  ('14 bob removes alice tester row', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$delete from public.app_testers where user_id = '00000000-0000-4000-8000-00000000a11c'$q$), 'ALLOWED (0 rows)'),
  ('15 alice self-grants premium', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$update public.profiles set is_premium = true where id = '00000000-0000-4000-8000-00000000a11c'$q$), 'BLOCKED'),
  ('16 alice self-grants admin', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$update public.profiles set is_admin = true where id = '00000000-0000-4000-8000-00000000a11c'$q$), 'BLOCKED'),
  ('17 alice edits her own display_name', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$update public.profiles set display_name = 'Alice Renamed' where id = '00000000-0000-4000-8000-00000000a11c'$q$), 'ALLOWED (1 rows)'),
  ('18 alice edits bob display_name', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$update public.profiles set display_name = 'Pwned' where id = '00000000-0000-4000-8000-00000000b0b0'$q$), 'ALLOWED (0 rows)'),
  ('19 bob comments linking an app he does not own', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_comments (app_id, author_id, body, linked_app_id) values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000b0b0','hi','00000000-0000-4000-8000-0000000000a1')$q$), 'BLOCKED'),
  ('20 bob comments linking his own app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_comments (app_id, author_id, body, linked_app_id) values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000b0b0','test mine too','00000000-0000-4000-8000-0000000000b1')$q$), 'ALLOWED (1 rows)'),
  ('21 bob comments as alice', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_comments (app_id, author_id, body) values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000a11c','impersonated')$q$), 'BLOCKED'),
  ('22 whitespace-only comment body', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_comments (app_id, author_id, body) values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000b0b0','   ')$q$), 'BLOCKED'),
  -- The anon key ships in the APK: without a session there is nothing to read.
  ('23 anon reads the feed', pg_temp.as_role(null, 'anon',
     $q$select 1 from public.apps_with_followers$q$), 'ALLOWED (0 rows)'),
  ('23b anon reads profiles', pg_temp.as_role(null, 'anon',
     $q$select 1 from public.profiles$q$), 'ALLOWED (0 rows)'),
  ('24 anon creates an app', pg_temp.as_role('00000000-0000-4000-8000-00000000a11c', 'anon',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000c1','00000000-0000-4000-8000-00000000a11c','Anon','https://groups.google.com/g/c','https://play.google.com/store/apps/details?id=com.c','https://play.google.com/apps/testing/com.c')$q$), 'BLOCKED');

-- The view must report the live tester count, and derive the package.
insert into results
select '25 follower count via the view', coalesce(follower_count, -1)::text, '1'
from public.apps_with_followers where id = '00000000-0000-4000-8000-0000000000a1';
insert into results
select '25b package derived from the opt-in link', package_name, 'com.bob'
from public.apps_with_followers where id = '00000000-0000-4000-8000-0000000000b1';

-- Proof of install: only through confirm_installed, only for your own rows,
-- always with the server clock.
insert into results values
  ('26 bob writes verified_at by hand', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.app_testers set verified_at = now() + interval '1 year' where user_id = '00000000-0000-4000-8000-00000000b0b0'$q$), 'BLOCKED'),
  ('26b alice confirms an install on bob row', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$select public.confirm_installed(array['00000000-0000-4000-8000-0000000000a1']::uuid[])$q$), 'ALLOWED (1 rows)'),
  ('26c anon confirms an install', pg_temp.as_role(null, 'anon',
     $q$select public.confirm_installed(array['00000000-0000-4000-8000-0000000000a1']::uuid[])$q$), 'BLOCKED');
insert into results
select '26d alice could not stamp bob', (verified_at is null)::text, 'true'
from public.app_testers where user_id = '00000000-0000-4000-8000-00000000b0b0';
insert into results values
  ('26e bob confirms his own install', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$select public.confirm_installed(array['00000000-0000-4000-8000-0000000000a1']::uuid[])$q$), 'ALLOWED (1 rows)');
insert into results
select '26f the owner sees one install', verified_count::text, '1'
from public.apps_with_followers where id = '00000000-0000-4000-8000-0000000000a1';
insert into results
select '26g tester status says installed', installed_recently::text, 'true'
from public.app_tester_status where user_id = '00000000-0000-4000-8000-00000000b0b0';

-- Reports: anyone flags, once, as themselves; only admins read them.
insert into results values
  ('27 bob reports alice app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_reports (reporter_id, app_id) values ('00000000-0000-4000-8000-00000000b0b0','00000000-0000-4000-8000-0000000000a1')$q$), 'ALLOWED (1 rows)'),
  ('27b bob reports the same app twice', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_reports (reporter_id, app_id) values ('00000000-0000-4000-8000-00000000b0b0','00000000-0000-4000-8000-0000000000a1')$q$), 'BLOCKED'),
  ('27c bob files a report as alice', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.app_reports (reporter_id, app_id) values ('00000000-0000-4000-8000-00000000a11c','00000000-0000-4000-8000-0000000000b1')$q$), 'BLOCKED'),
  ('27d a report with two targets', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$insert into public.app_reports (reporter_id, app_id, comment_id) select '00000000-0000-4000-8000-00000000a11c','00000000-0000-4000-8000-0000000000b1', id from public.app_comments limit 1$q$), 'BLOCKED'),
  ('27e a regular user reads reports', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$select 1 from public.app_reports$q$), 'ALLOWED (0 rows)');

-- 14-day streak: it starts when the 12th tester joins and resets when one leaves.
insert into auth.users (id, instance_id, aud, role, email, created_at, updated_at)
select ('00000000-0000-4000-8000-0000000001' || lpad(n::text, 2, '0'))::uuid,
       '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated',
       'tester' || n || '@test.invalid', now(), now()
from generate_series(1, 11) n;
insert into public.app_testers (app_id, user_id)
select '00000000-0000-4000-8000-0000000000a1', ('00000000-0000-4000-8000-0000000001' || lpad(n::text, 2, '0'))::uuid
from generate_series(1, 11) n;
insert into results
select '28 the 12th tester starts the streak at day 1', full_days::text, '1'
from public.apps_with_followers where id = '00000000-0000-4000-8000-0000000000a1';
insert into results values
  ('28b a tester leaves', pg_temp.as_user('00000000-0000-4000-8000-000000000101',
     $q$delete from public.app_testers where user_id = '00000000-0000-4000-8000-000000000101'$q$), 'ALLOWED (1 rows)');
insert into results
select '28c dropping to 11 resets the streak', full_days::text, '0'
from public.apps_with_followers where id = '00000000-0000-4000-8000-0000000000a1';

-- Premium lifts the slot limit. Only service_role writes this column in production.
update public.profiles set is_premium = true where id = '00000000-0000-4000-8000-00000000a11c';
insert into results values
  ('29 premium alice creates a 2nd app', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a9','00000000-0000-4000-8000-00000000a11c','Alice Premium','https://groups.google.com/g/a9','https://play.google.com/store/apps/details?id=com.alice9','https://play.google.com/apps/testing/com.alice9')$q$), 'ALLOWED (1 rows)');

-- And an expired subscription closes it again for the next one.
update public.profiles set is_premium = false where id = '00000000-0000-4000-8000-00000000a11c';
insert into results values
  ('29b lapsed alice creates a 3rd app', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a8','00000000-0000-4000-8000-00000000a11c','Alice Lapsed','https://groups.google.com/g/a8','https://play.google.com/store/apps/details?id=com.alice8','https://play.google.com/apps/testing/com.alice8')$q$), 'BLOCKED');

-- Moderation: Bob is promoted the way the dashboard would do it.
update public.profiles set is_admin = true where id = '00000000-0000-4000-8000-00000000b0b0';
insert into public.app_comments (id, app_id, author_id, body)
values ('00000000-0000-4000-8000-0000000000c9','00000000-0000-4000-8000-0000000000b1',
        '00000000-0000-4000-8000-00000000a11c','abusive content');

insert into results values
  ('30 admin takes down a foreign app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$delete from public.apps where id = '00000000-0000-4000-8000-0000000000a9'$q$), 'ALLOWED (1 rows)'),
  ('31 admin takes down a foreign comment', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$delete from public.app_comments where id = '00000000-0000-4000-8000-0000000000c9'$q$), 'ALLOWED (1 rows)'),
  ('32 admin cannot edit a foreign app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set name = 'Moderated' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'ALLOWED (0 rows)'),
  ('33 admin cannot grant himself premium', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.profiles set is_premium = true where id = '00000000-0000-4000-8000-00000000b0b0'$q$), 'BLOCKED'),
  ('34 admin reads reports', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$select 1 from public.app_reports$q$), 'ALLOWED (1 rows)');

-- Account deletion, last because it takes Bob's rows with it.
insert into results values
  ('35 anon deletes an account', pg_temp.as_role(null, 'anon',
     $q$select public.delete_my_account()$q$), 'BLOCKED'),
  ('35b bob deletes his own account', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$select public.delete_my_account()$q$), 'ALLOWED (1 rows)');
insert into results
select '35c bob and everything he owned are gone',
       (select count(*) from public.profiles where id = '00000000-0000-4000-8000-00000000b0b0')::text || '/' ||
       (select count(*) from public.apps where owner_id = '00000000-0000-4000-8000-00000000b0b0')::text || '/' ||
       (select count(*) from public.app_testers where user_id = '00000000-0000-4000-8000-00000000b0b0')::text,
       '0/0/0';
insert into results
select '35d alice is untouched', count(*)::text, '1'
from public.profiles where id = '00000000-0000-4000-8000-00000000a11c';

do $$
declare failed integer; detail text;
begin
  select count(*), string_agg(format(E'\n  %s\n    expected: %s\n    got:      %s', test, expected, got), '')
    into failed, detail
  from results where got is distinct from expected;

  if failed > 0 then
    raise exception E'% RLS assertion(s) failed:%', failed, detail;
  end if;
  raise notice 'all % RLS assertions passed', (select count(*) from results);
end $$;

rollback;
