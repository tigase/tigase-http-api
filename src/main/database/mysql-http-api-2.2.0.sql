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

--

-- QUERY START:
drop procedure if exists Tig_HFU_UsedSpaceCountForUser;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_HFU_UsedSpaceCountForDomain;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_HFU_UserSlotsQuery;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_HFU_DomainSlotsQuery;
-- QUERY END:

-- QUERY START:
drop procedure if exists Tig_HFU_RemoveSlot;
-- QUERY END:

delimiter //

-- looks like usage of _filename is restriced on MySQL
-- QUERY START:
create procedure  Tig_HFU_UsedSpaceCountForUser(_jid varchar(2049) CHARSET utf8)
begin
    select sum(filesize) from tig_hfu_slots where uploader = _jid;
end //
-- QUERY END:

-- QUERY START:
create procedure  Tig_HFU_UsedSpaceCountForDomain(_domain varchar(1024) CHARSET utf8)
begin
    select sum(filesize) from tig_hfu_slots where domain = _domain;
end //
-- QUERY END:

-- QUERY START:
create procedure  Tig_HFU_UserSlotsQuery(_jid varchar(2049) CHARSET utf8, _afterId varchar(60), _limit int)
begin
    if _afterId is null then
        select slot_id, filename, filesize, content_type, ts, uploader
        from tig_hfu_slots
        where
            uploader = _jid
        order by ts asc
        limit _limit;
    else
        select slot_id, filename, filesize, content_type, ts, uploader
        from tig_hfu_slots
        where
            uploader = _jid
            and ts > (
                select ts
                from tig_hfu_slots
                where slot_id = _afterId
            )
        order by ts asc
        limit _limit;
    end if;
end //
-- QUERY END:

-- QUERY START:
create procedure  Tig_HFU_DomainSlotsQuery(_domain varchar(1024) CHARSET utf8, _afterId varchar(60), _limit int)
begin
    if _afterId is null then
        select slot_id, filename, filesize, content_type, ts, uploader
        from tig_hfu_slots
        where
                domain = _domain
        order by ts asc
        limit _limit;
    else
        select slot_id, filename, filesize, content_type, ts, uploader
        from tig_hfu_slots
        where
                domain = _domain
          and ts > (
            select ts
            from tig_hfu_slots
            where slot_id = _afterId
        )
        order by ts asc
        limit _limit;
    end if;
end //
-- QUERY END:

-- QUERY START:
create procedure  Tig_HFU_RemoveSlot(_slotId varchar(60) CHARSET utf8)
begin
    delete from tig_hfu_slots where slot_id = _slotId;
end //
-- QUERY END:

delimiter ;