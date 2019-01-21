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
        ts timestamp(6),
        status smallint,

        primary key (slot_id)
)
ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;
-- QUERY END:

-- QUERY START:
drop procedure if exists TigExecuteIfNot;
-- QUERY END:
-- QUERY START:
drop procedure if exists TigAddColumnIfNotExists;
-- QUERY END:

-- QUERY START:
drop procedure if exists TigAddIndexIfNotExists;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure TigExecuteIfNot(cond int, query text)
begin
set @s = (select if (
        cond > 0,
'select 1',
query
));
prepare stmt from @s;
execute stmt;
deallocate prepare stmt;
end //
-- QUERY END:

-- QUERY START:
create procedure TigAddColumnIfNotExists(tab varchar(255), col varchar(255), def varchar(255))
begin
call TigExecuteIfNot((select count(1) from information_schema.COLUMNS where TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tab AND COLUMN_NAME = col),
        CONCAT('alter table ', tab, ' add `', col, '` ', def)
);
end //
-- QUERY END:

-- QUERY START:
create procedure TigAddIndexIfNotExists(tab varchar(255), ix_name varchar(255), uni smallint, def varchar(255))
begin
call TigExecuteIfNot((select count(1) from information_schema.STATISTICS where TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tab and INDEX_NAME = ix_name),
        CONCAT('create ', IF(uni=1, 'unique', ''), ' index ', ix_name, ' on ', tab, ' ', def)
);
end //
-- QUERY END:

delimiter ;

-- QUERY START:
call TigAddIndexIfNotExists('tig_hfu_slots', 'tig_hfu_slots_slot_id_index', 0, '(slot_id)');
-- QUERY END:

-- QUERY START:
call TigAddIndexIfNotExists('tig_hfu_slots', 'tig_hfu_slots_domain_ts_index', 0, '(domain(255), ts)');
-- QUERY END:


-- QUERY START:
drop procedure  if exists Tig_HFU_AllocateSlot;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_HFU_UpdateSlot;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_HFU_GetSlot;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_HFU_ListExpiredSlots;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_HFU_RemoveExpiredSlots;
-- QUERY END:

delimiter //

-- looks like usage of _filename is restriced on MySQL
-- QUERY START:
create procedure  Tig_HFU_AllocateSlot(_slotId varchar(60) CHARSET utf8, _uploader varchar(2049) CHARSET utf8, _domain varchar(1024) CHARSET utf8, _res varchar(1024) CHARSET utf8, _filename1 varchar(255) CHARSET utf8, _filesize bigint, _contentType varchar(128) CHARSET utf8)
begin
    insert into tig_hfu_slots( slot_id, uploader, domain, res, filename, filesize, content_type, ts, status )
        values (_slotId, _uploader, _domain, _res, _filename1, _filesize, _contentType, now(6), 0);

    select _slotId as slotId;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_UpdateSlot(_slotId varchar(60) CHARSET utf8)
begin
    update tig_hfu_slots
        set status = 1
        where slot_id = _slotId;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_GetSlot(_slotId varchar(60) CHARSET utf8)
begin
    select uploader, slot_id, filename, filesize, content_type, ts
        from tig_hfu_slots
        where slot_id = _slotId;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_ListExpiredSlots(_domain varchar(1024) CHARSET utf8, _ts timestamp(6), _limit int)
begin
    select uploader, slot_id, filename, filesize, content_type, ts
        from tig_hfu_slots
        where domain = _domain
            and ts < _ts
        order by ts limit _limit;
end //
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_RemoveExpiredSlots(_domain varchar(1024) CHARSET utf8, _ts timestamp(6), _limit int)
begin
    drop temporary table if exists tig_hfu_expired_slots_temp;
    create temporary table tig_hfu_expired_slots_temp
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

    drop temporary table tig_hfu_expired_slots_temp;
end //
-- QUERY END:

-- QUERY START:
call TigSetComponentVersion('http-api', '2.0.0');
-- QUERY END:
