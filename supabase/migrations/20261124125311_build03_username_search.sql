create or replace function public.search_public_profiles(
    search_query text,
    result_limit integer default 20
)
returns table (id uuid, full_name text, username text, bio text, avatar_url text)
language plpgsql stable security definer set search_path = ''
as $function$
declare normalized_query text; safe_limit integer;
begin
    if auth.uid() is null then raise exception 'Authentication required' using errcode = '42501'; end if;
    normalized_query := lower(btrim(coalesce(search_query, '')));
    if char_length(normalized_query) < 2 then return; end if;
    if char_length(normalized_query) > 30 then normalized_query := left(normalized_query, 30); end if;
    safe_limit := greatest(1, least(coalesce(result_limit, 20), 20));
    return query
    select p.id, p.full_name, p.username, p.bio, p.avatar_url
    from public.profiles p
    where p.id <> auth.uid() and p.username is not null and p.username <> ''
      and strpos(lower(p.username), normalized_query) > 0
    order by case when lower(p.username) = normalized_query then 0 else 1 end, lower(p.username)
    limit safe_limit;
end;
$function$;
revoke all on function public.search_public_profiles(text, integer) from public;
revoke all on function public.search_public_profiles(text, integer) from anon;
revoke all on function public.search_public_profiles(text, integer) from authenticated;
grant execute on function public.search_public_profiles(text, integer) to authenticated;
comment on function public.search_public_profiles(text, integer) is 'Authenticated username discovery. Returns limited public profile preview fields, excludes the caller, and requires a 2+ character query.';
