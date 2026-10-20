-- Dragonfly sync: every synced row of every device, one table for all.
-- Run once in the Supabase SQL editor (or with `supabase db push`).
--
-- Each row belongs to the signed-in user (row level security), and the newest change wins:
-- an update carrying an older modified_at than the stored one is ignored.

create table if not exists public.sync_records (
    user_id     uuid        not null default auth.uid() references auth.users (id) on delete cascade,
    table_name  text        not null,
    id          text        not null,
    -- The row's columns, with references to other rows written as their ids; null once deleted.
    data        jsonb,
    deleted     boolean     not null default false,
    -- When the change was made on the device, in epoch milliseconds.
    modified_at bigint      not null,
    -- When the server stored it; devices download what changed since their last sync.
    updated_at  timestamptz not null default now(),
    primary key (user_id, table_name, id)
);

create index if not exists sync_records_user_updated_at on public.sync_records (user_id, updated_at);

alter table public.sync_records enable row level security;

drop policy if exists "Users read their own records" on public.sync_records;
create policy "Users read their own records" on public.sync_records
    for select to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "Users add their own records" on public.sync_records;
create policy "Users add their own records" on public.sync_records
    for insert to authenticated with check ((select auth.uid()) = user_id);

drop policy if exists "Users change their own records" on public.sync_records;
create policy "Users change their own records" on public.sync_records
    for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

-- The Data API only exposes tables with explicit grants.
grant select, insert, update on public.sync_records to authenticated;

-- Server time for the download cursor, and last write wins on the device's own time.
create or replace function public.sync_records_before_write() returns trigger
    language plpgsql
    set search_path = ''
as $$
begin
    if tg_op = 'UPDATE' and new.modified_at < old.modified_at then
        return old;
    end if;
    new.updated_at := clock_timestamp();
    return new;
end;
$$;

drop trigger if exists sync_records_before_write on public.sync_records;
create trigger sync_records_before_write
    before insert or update on public.sync_records
    for each row execute function public.sync_records_before_write();
