-- Allow the server-side Edge Function service_role to read profile directory fields.
-- This does not grant profile access to client roles or change RLS policies.
GRANT SELECT ON TABLE public.profiles TO service_role;
