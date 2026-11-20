-- Keep notification topic preferences opt-in even when an insert omits subscribed.
-- Existing subscription choices are intentionally preserved.
alter table public.user_topic_subscriptions
  alter column subscribed set default false;
