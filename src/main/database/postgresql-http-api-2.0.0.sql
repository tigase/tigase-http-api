--
-- Tigase HTTP API component - Tigase HTTP API component
-- Copyright (C) 2013 Tigase, Inc. (office@tigase.com)
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, version 3 of the License.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program. Look for COPYING file in the top folder.
-- If not, see http://www.gnu.org/licenses/.
--

-- QUERY START:
create table if not exists tig_hfu_slots (
        slot_id varchar(60) not null,
        uploader varchar(2049) not null,
        domain varchar(1024) not null,
        res varchar(1024) not null,
        filename varchar(255) not null,
        filesize bigint not null,
        content_type varchar(128),
        ts timestamp with time zone,
        status smallint,

        primary key (slot_id)
);
-- QUERY END:

-- QUERY START:
alter table tig_hfu_slots
    alter column ts type timestamp with time zone;
-- QUERY END:

-- QUERY START:
do $$
begin
if exists (select 1 where (select to_regclass('public.tig_hfu_slots_slot_id_index')) is null) then
        create index tig_hfu_slots_slot_id_index on tig_hfu_slots (slot_id);
end if;
end$$;
-- QUERY END:

-- QUERY START:
do $$
begin
if exists (select 1 where (select to_regclass('public.tig_hfu_slots_domain_ts_index')) is null) then
        create index tig_hfu_slots_domain_ts_index on tig_hfu_slots (domain, ts);
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_AllocateSlot(_slotId varchar(60), _uploader varchar(2049), _domain varchar(1024),
        _res varchar(1024), _filename varchar(255), _filesize bigint, _contentType varchar(128))
returns varchar(60) as $$
begin
    insert into tig_hfu_slots( slot_id, uploader, domain, res, filename, filesize, content_type, ts, status )
        values (_slotId, _uploader, _domain, _res, _filename, _filesize, _contentType, now(), 0);

    return _slotId;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_UpdateSlot(_slotId varchar(60))
returns void as $$
    update tig_hfu_slots
        set status = 1
        where slot_id = _slotId;
$$ LANGUAGE 'sql';
-- QUERY END:

-- QUERY START:
do $$
begin
if exists( select 1 from pg_proc where proname = lower('Tig_HFU_GetSlot') and pg_get_function_result(oid) = 'TABLE(uploader character varying, slot_id character varying, filename character varying, filesize bigint, content_type character varying, ts timestamp without time zone)') then
    drop function Tig_HFU_GetSlot(character varying);
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_GetSlot(_slotId varchar(60))
returns table( uploader varchar(2049), slot_id varchar(60), filename varchar(255), filesize bigint, content_type varchar(128), ts timestamp with time zone ) as $$
    select uploader, slot_id, filename, filesize, content_type, ts
        from tig_hfu_slots
        where slot_id = _slotId;
$$ LANGUAGE 'sql';
-- QUERY END:

-- QUERY START:
do $$
begin
if exists( select 1 from pg_proc where proname = lower('Tig_HFU_ListExpiredSlots') and pg_get_function_result(oid) = 'TABLE(uploader character varying, slot_id character varying, filename character varying, filesize bigint, content_type character varying, ts timestamp without time zone)') then
    drop function Tig_HFU_ListExpiredSlots(character varying, timestamp without time zone, integer);
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_ListExpiredSlots(_domain varchar(1024), _ts timestamp with time zone, _limit int)
returns table( uploader varchar(2049), slot_id varchar(60), filename varchar(255), filesize bigint, content_type varchar(128), ts timestamp with time zone) as $$
    select uploader, slot_id, filename, filesize, content_type, ts
        from tig_hfu_slots
        where domain = _domain
            and ts < _ts
        order by ts limit _limit;
$$ LANGUAGE 'sql';
-- QUERY END:

-- QUERY START:
do $$
begin
if exists( select 1 from pg_proc where proname = lower('Tig_HFU_RemoveExpiredSlots') and pg_get_function_arguments(oid) = '_domain character varying, _ts timestamp without time zone, _limit integer') then
    drop function Tig_HFU_RemoveExpiredSlots(character varying, timestamp without time zone, integer);
end if;
end$$;
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_RemoveExpiredSlots(_domain varchar(1024), _ts timestamp with time zone, _limit int)
returns void as $$
begin
    drop table if exists tig_hfu_expired_slots_temp;
    create temporary table tig_hfu_expired_slots_temp as
        select slot_id
            from tig_hfu_slots
            where domain = _domain
                and ts < _ts
            order by ts limit _limit;

    delete from tig_hfu_slots
        where exists (
            select 1
                from tig_hfu_expired_slots_temp t1
                where t1.slot_id = tig_hfu_slots.slot_id
        );

    drop table tig_hfu_expired_slots_temp;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
select TigSetComponentVersion('http-api', '2.0.0');
-- QUERY END:
