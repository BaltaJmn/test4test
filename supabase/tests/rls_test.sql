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
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a1','00000000-0000-4000-8000-00000000a11c','Alice App','https://g/a','https://p/a','https://o/a')$q$), 'ALLOWED (1 rows)'),
  ('02 alice creates 2nd app over the free slot limit', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a2','00000000-0000-4000-8000-00000000a11c','Alice App 2','https://g/a2','https://p/a2','https://o/a2')$q$), 'BLOCKED'),
  ('03 bob creates his 1st app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000b1','00000000-0000-4000-8000-00000000b0b0','Bob App','https://g/b','https://p/b','https://o/b')$q$), 'ALLOWED (1 rows)'),
  ('04 bob creates an app owned by alice', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a3','00000000-0000-4000-8000-00000000a11c','Spoofed','https://g/x','https://p/x','https://o/x')$q$), 'BLOCKED'),
  ('05 non-https url', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set play_store_url = 'http://insecure' where id = '00000000-0000-4000-8000-0000000000b1'$q$), 'BLOCKED'),
  ('06 bob renames alice app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set name = 'Hacked' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'ALLOWED (0 rows)'),
  ('07 bob deletes alice app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$delete from public.apps where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'ALLOWED (0 rows)'),
  ('08 bob steals alice app via owner_id', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set owner_id = '00000000-0000-4000-8000-00000000b0b0' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'ALLOWED (0 rows)'),
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
  ('23 anon reads the feed', pg_temp.as_role(null, 'anon',
     $q$select 1 from public.apps_with_followers$q$), 'ALLOWED (2 rows)'),
  ('24 anon creates an app', pg_temp.as_role('00000000-0000-4000-8000-00000000a11c', 'anon',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000c1','00000000-0000-4000-8000-00000000a11c','Anon','https://g/c','https://p/c','https://o/c')$q$), 'BLOCKED');

-- The view must report the live tester count.
insert into results
select '25 follower count via the view', coalesce(follower_count, -1)::text, '1'
from public.apps_with_followers where id = '00000000-0000-4000-8000-0000000000a1';

-- Premium lifts the slot limit. Only service_role writes this column in production.
update public.profiles set is_premium = true where id = '00000000-0000-4000-8000-00000000a11c';
insert into results values
  ('26 premium alice creates a 2nd app', pg_temp.as_user('00000000-0000-4000-8000-00000000a11c',
     $q$insert into public.apps values ('00000000-0000-4000-8000-0000000000a9','00000000-0000-4000-8000-00000000a11c','Alice Premium','https://g/a9','https://p/a9','https://o/a9')$q$), 'ALLOWED (1 rows)');

-- Moderation: Bob is promoted the way the dashboard would do it.
update public.profiles set is_admin = true where id = '00000000-0000-4000-8000-00000000b0b0';
insert into public.app_comments (id, app_id, author_id, body)
values ('00000000-0000-4000-8000-0000000000c9','00000000-0000-4000-8000-0000000000b1',
        '00000000-0000-4000-8000-00000000a11c','abusive content');

insert into results values
  ('27 admin takes down a foreign app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$delete from public.apps where id = '00000000-0000-4000-8000-0000000000a9'$q$), 'ALLOWED (1 rows)'),
  ('28 admin takes down a foreign comment', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$delete from public.app_comments where id = '00000000-0000-4000-8000-0000000000c9'$q$), 'ALLOWED (1 rows)'),
  ('29 admin cannot edit a foreign app', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.apps set name = 'Moderated' where id = '00000000-0000-4000-8000-0000000000a1'$q$), 'ALLOWED (0 rows)'),
  ('30 admin cannot grant himself premium', pg_temp.as_user('00000000-0000-4000-8000-00000000b0b0',
     $q$update public.profiles set is_premium = true where id = '00000000-0000-4000-8000-00000000b0b0'$q$), 'BLOCKED');

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
