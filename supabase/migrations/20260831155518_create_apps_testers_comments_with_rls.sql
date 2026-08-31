-- ---------------------------------------------------------------- tables

create table public.apps (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  name text not null check (length(trim(name)) between 1 and 100),
  google_groups_url text not null check (google_groups_url like 'https://%'),
  play_store_url text not null check (play_store_url like 'https://%'),
  opt_in_url text not null check (opt_in_url like 'https://%'),
  created_at timestamptz not null default now()
);
create index apps_owner_id_idx on public.apps(owner_id);

create table public.app_testers (
  app_id uuid not null references public.apps(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (app_id, user_id)
);
create index app_testers_user_id_idx on public.app_testers(user_id);

create table public.app_comments (
  id uuid primary key default gen_random_uuid(),
  app_id uuid not null references public.apps(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (length(trim(body)) between 1 and 2000),
  linked_app_id uuid references public.apps(id) on delete set null,
  created_at timestamptz not null default now()
);
create index app_comments_app_id_idx on public.app_comments(app_id);

-- Read model. Counting beats a denormalized column: it cannot drift out of sync.
-- security_invoker so the underlying RLS applies as the querying user.
create view public.apps_with_followers with (security_invoker = true) as
  select a.*, count(t.user_id) as follower_count
  from public.apps a
  left join public.app_testers t on t.app_id = a.id
  group by a.id;

comment on view public.apps_with_followers is 'Feed read model: apps plus their live tester count.';

-- ------------------------------------------------------------------- rls

alter table public.apps enable row level security;
alter table public.app_testers enable row level security;
alter table public.app_comments enable row level security;

-- profiles: world-readable, self-editable, but is_premium is off limits.
-- Column-level grants only bite once the table-level UPDATE is gone.
create policy profiles_select_all on public.profiles
  for select using (true);
create policy profiles_update_own on public.profiles
  for update using ((select auth.uid()) = id) with check ((select auth.uid()) = id);
revoke update on public.profiles from anon, authenticated;
grant update (display_name, avatar_url) on public.profiles to authenticated;

-- apps: world-readable, owner-writable. The insert check also enforces the
-- free-tier slot limit, so no RPC is needed to gate it.
create policy apps_select_all on public.apps
  for select using (true);
create policy apps_insert_own_within_slots on public.apps
  for insert with check (
    owner_id = (select auth.uid())
    and (
      (select is_premium from public.profiles where id = (select auth.uid()))
      or (select count(*) from public.apps where owner_id = (select auth.uid())) < 1
    )
  );
create policy apps_update_own on public.apps
  for update using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));
create policy apps_delete_own on public.apps
  for delete using (owner_id = (select auth.uid()));

-- app_testers: world-readable so follower counts are the same for everyone.
-- Joining is self-only, and never your own app.
create policy app_testers_select_all on public.app_testers
  for select using (true);
create policy app_testers_insert_self on public.app_testers
  for insert with check (
    user_id = (select auth.uid())
    and not exists (
      select 1 from public.apps
      where id = app_id and owner_id = (select auth.uid())
    )
  );
create policy app_testers_delete_self on public.app_testers
  for delete using (user_id = (select auth.uid()));

-- app_comments: world-readable, author-only writes. A linked app must be one
-- the author actually owns. No update policy: comments are immutable.
create policy app_comments_select_all on public.app_comments
  for select using (true);
create policy app_comments_insert_own on public.app_comments
  for insert with check (
    author_id = (select auth.uid())
    and (
      linked_app_id is null
      or exists (
        select 1 from public.apps
        where id = linked_app_id and owner_id = (select auth.uid())
      )
    )
  );
create policy app_comments_delete_own on public.app_comments
  for delete using (author_id = (select auth.uid()));
