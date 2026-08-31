create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  avatar_url text,
  is_premium boolean not null default false,
  created_at timestamptz not null default now()
);

comment on table public.profiles is 'User profile extending auth.users with Test4Test data.';
comment on column public.profiles.is_premium is 'Unlimited app slots. Source of truth, written only by the RevenueCat webhook Edge Function.';

-- Deny-by-default until the RLS policies land (issue #9).
alter table public.profiles enable row level security;

create function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name, avatar_url)
  values (
    new.id,
    coalesce(
      new.raw_user_meta_data ->> 'full_name',
      new.raw_user_meta_data ->> 'name',
      split_part(new.email, '@', 1)
    ),
    coalesce(
      new.raw_user_meta_data ->> 'avatar_url',
      new.raw_user_meta_data ->> 'picture'
    )
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
