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
create procedure Tig_HFU_UsedSpaceCountForUser(jid varchar(2049))
    PARAMETER STYLE JAVA
    LANGUAGE JAVA
    READS SQL DATA
    DYNAMIC RESULT SETS 1
    EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.usedSpaceCountForUser';
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_UsedSpaceCountForDomain(domain varchar(1024))
    PARAMETER STYLE JAVA
    LANGUAGE JAVA
    READS SQL DATA
    DYNAMIC RESULT SETS 1
    EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.usedSpaceCountForDomain';
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_UserSlotsQuery(jid varchar(2049), afterId varchar(60), maxLimit int)
    PARAMETER STYLE JAVA
    LANGUAGE JAVA
    READS SQL DATA
    DYNAMIC RESULT SETS 1
    EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.userSlotsQuery';
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_DomainSlotsQuery(domain varchar(1024), afterId varchar(60), maxLimit int)
    PARAMETER STYLE JAVA
    LANGUAGE JAVA
    READS SQL DATA
    DYNAMIC RESULT SETS 1
    EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.domainSlotsQuery';
-- QUERY END:

-- QUERY START:
create procedure Tig_HFU_RemoveSlot(slotId varchar(60))
    PARAMETER STYLE JAVA
    LANGUAGE JAVA
    MODIFIES SQL DATA
    EXTERNAL NAME 'tigase.http.upload.db.derby.StoredProcedures.removeSlot';
-- QUERY END: