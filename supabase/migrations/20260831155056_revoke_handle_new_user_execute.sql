-- Trigger-only function: nobody should be able to reach it through /rest/v1/rpc.
-- The trigger itself keeps working; EXECUTE is checked at trigger creation, not per fire.
revoke execute on function public.handle_new_user() from public, anon, authenticated;
