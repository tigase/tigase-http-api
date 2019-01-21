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
create table tig_hfu_slots (
        slot_id varchar(60) not null,
        uploader varchar(2049) not null,
        domain varchar(1024) not null,
        res varchar(1024) not null,
        filename varchar(255) not null,
        filesize bigint not null,
        content_type varchar(128),
        ts timestamp,
        status smallint,

        primary key (slot_id)
);
-- QUERY END:

-- QUERY START:
create index tig_hfu_slots_slot_id_index on tig_hfu_slots (slot_id);
-- QUERY END:

-- QUERY START:
create index tig_hfu_slots_domain_ts_index on tig_hfu_slots (domain, ts);
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_AllocateSlot(slotId varchar(60), uploader varchar(2049), domain varchar(1024),
        res varchar(1024), filename varchar(255), filesize bigint, contentType varchar(128))
        PARAMETER STYLE JAVA
        LANGUAGE JAVA
        MODIFIES SQL DATA
        DYNAMIC RESULT SETS 1
        EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.allocateSlot';
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_UpdateSlot(slotId varchar(60))
        PARAMETER STYLE JAVA
        LANGUAGE JAVA
        MODIFIES SQL DATA
        EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.updateSlot';
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_GetSlot(slotId varchar(60))
        PARAMETER STYLE JAVA
        LANGUAGE JAVA
        READS SQL DATA
        DYNAMIC RESULT SETS 1
        EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.getSlot';
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_ListExpiredSlots(domain varchar(1024), ts timestamp, maxLimit int)
        PARAMETER STYLE JAVA
        LANGUAGE JAVA
        READS SQL DATA
        DYNAMIC RESULT SETS 1
        EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.listExpiredSlots';
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_RemoveExpiredSlots(domain varchar(1024), ts timestamp, maxLimit int)
        PARAMETER STYLE JAVA
        LANGUAGE JAVA
        MODIFIES SQL DATA
        EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.removeExpiredSlots';
-- QUERY END:

-- QUERY START:
call TigSetComponentVersion('http-api', '2.0.0');
-- QUERY END:
