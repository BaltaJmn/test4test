-- Moderation. Reuses the profiles flag pattern: is_admin sits behind the same
-- column-level grant as is_premium, so a user cannot promote themselves.
alter table public.profiles add column is_admin boolean not null default false;

comment on column public.profiles.is_admin is 'Moderator. Set from the Supabase dashboard only; the column grant keeps users out.';

-- Plain SECURITY INVOKER: profiles is world-readable, so no elevated rights needed.
create function public.is_admin()
returns boolean language sql stable
set search_path = ''
as $$
  select coalesce((select is_admin from public.profiles where id = (select auth.uid())), false)
$$;

-- Permissive policies OR together, so these widen delete without touching the
-- owner rules above. Takedown only: no admin insert or update anywhere.
create policy apps_delete_admin on public.apps
  for delete using (public.is_admin());

create policy app_comments_delete_admin on public.app_comments
  for delete using (public.is_admin());
