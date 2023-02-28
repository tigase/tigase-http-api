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
alter table tig_hfu_slots modify filename varchar(255) character set utf8mb4 not null;
-- QUERY END:

-- QUERY START:
drop procedure  if exists Tig_HFU_AllocateSlot;
-- QUERY END:

delimiter //

-- QUERY START:
create procedure  Tig_HFU_AllocateSlot(_slotId varchar(60) CHARSET utf8, _uploader varchar(2049) CHARSET utf8, _domain varchar(1024) CHARSET utf8, _res varchar(1024) CHARSET utf8, _filename1 varchar(255) CHARSET utf8mb4, _filesize bigint, _contentType varchar(128) CHARSET utf8)
begin
    insert into tig_hfu_slots( slot_id, uploader, domain, res, filename, filesize, content_type, ts, status )
        values (_slotId, _uploader, _domain, _res, _filename1, _filesize, _contentType, now(6), 0);

    select _slotId as slotId;
end //
-- QUERY END:

delimiter ;