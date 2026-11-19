-- Build 11 hotfix: the installation registry uses service_role upsert.
-- Keep the grant server-only; authenticated clients retain their existing RLS model.
grant insert, update on table public.device_tokens to service_role;
