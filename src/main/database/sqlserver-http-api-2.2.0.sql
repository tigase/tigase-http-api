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
SET QUOTED_IDENTIFIER ON
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_UsedSpaceCountForUser')
        DROP PROCEDURE [dbo].[Tig_HFU_UsedSpaceCountForUser]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_UsedSpaceCountForUser]
    @_jid nvarchar(2049)
AS
BEGIN
    select sum(filesize) from tig_hfu_slots where uploader = @_jid;
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_UsedSpaceCountForDomain')
        DROP PROCEDURE [dbo].[Tig_HFU_UsedSpaceCountForDomain]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_UsedSpaceCountForDomain]
    @_domain nvarchar(1024)
AS
BEGIN
    select sum(filesize) from tig_hfu_slots where domain = @_domain;
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_UserSlotsQuery')
        DROP PROCEDURE [dbo].[Tig_HFU_UserSlotsQuery]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_UserSlotsQuery]
    @_jid nvarchar(2049),
    @_afterId nvarchar(60),
    @_limit int
AS
BEGIN
    if @_afterId is null
        begin
        ;with results_cte as(select slot_id, filename, filesize, content_type, ts, uploader, row_number() over (order by ts asc) as row_num
        from tig_hfu_slots
        where
            uploader = @_jid
        )
        select * from results_cte where row_num < (1 + @_limit);
        end
    else
        begin
        ;with results_cte as(select slot_id, filename, filesize, content_type, ts, uploader, row_number() over (order by ts asc) as row_num
        from tig_hfu_slots
        where
            uploader = @_jid
            and ts > (
                select ts
                from tig_hfu_slots
                where slot_id = @_afterId
            )
        )
        select * from results_cte where row_num < (1 + @_limit);
        end
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_DomainSlotsQuery')
DROP PROCEDURE [dbo].[Tig_HFU_DomainSlotsQuery]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_DomainSlotsQuery]
    @_domain nvarchar(1024),
    @_afterId nvarchar(60),
    @_limit int
AS
BEGIN
    if @_afterId is null
        begin
        ;with results_cte as(select slot_id, filename, filesize, content_type, ts, uploader, row_number() over (order by ts asc) as row_num
        from tig_hfu_slots
        where
            domain = @_domain
        )
        select * from results_cte where row_num < (1 + @_limit);
        end
    else
        begin
        ;with results_cte as(select slot_id, filename, filesize, content_type, ts, uploader, row_number() over (order by ts asc) as row_num
        from tig_hfu_slots
        where
            domain = @_domain
            and ts > (
                select ts
                from tig_hfu_slots
                where slot_id = @_afterId
            )
        )
        select * from results_cte where row_num < (1 + @_limit);
        end
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_RemoveSlot')
DROP PROCEDURE [dbo].[Tig_HFU_RemoveSlot]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_RemoveSlot]
    @_slotId nvarchar(60)
AS
BEGIN
    delete from tig_hfu_slots where slot_id = @_slotId;
END
-- QUERY END:
GO