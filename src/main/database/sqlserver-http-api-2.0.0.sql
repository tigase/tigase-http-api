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
IF NOT EXISTS (select * from sysobjects where name='tig_hfu_slots' and xtype='U')
    CREATE TABLE [dbo].[tig_hfu_slots] (
        [slot_id] [nvarchar](60) NOT NULL,
        [uploader] [nvarchar](2049) NOT NULL,
        [domain] [nvarchar](1024) NOT NULL,
        [domain_fragment] AS CAST( [domain] AS NVARCHAR(255)),
        [res] [nvarchar](1024) NOT NULL,
        [filename] [nvarchar](255) NOT NULL,
        [filesize] [bigint] NOT NULL,
        [content_type] [nvarchar](128),
        [ts] [datetime] NOT NULL,
        [status] [smallint]

        PRIMARY KEY ( [slot_id] )
    );
-- QUERY END:
GO

-- QUERY START:
IF NOT EXISTS(SELECT * FROM sys.indexes WHERE object_id = object_id('dbo.tig_hfu_slots') AND NAME ='IX_tig_hfu_slots_slot_id')
        CREATE INDEX IX_tig_hfu_slots_slot_id ON [dbo].[tig_hfu_slots]([slot_id]);
-- QUERY END:
GO

-- QUERY START:
IF NOT EXISTS(SELECT * FROM sys.indexes WHERE object_id = object_id('dbo.tig_hfu_slots') AND NAME ='IX_tig_hfu_slots_domain_ts')
        CREATE INDEX IX_tig_hfu_slots_domain_ts ON [dbo].[tig_hfu_slots]([domain_fragment], [ts]);
-- QUERY END:
GO

-- ---------------------
-- Stored procedures
-- ---------------------

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_AllocateSlot')
        DROP PROCEDURE [dbo].[Tig_HFU_AllocateSlot]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_AllocateSlot]
    @_slotId nvarchar(60),
    @_uploader nvarchar(2049),
    @_domain nvarchar(1024),
    @_res nvarchar(1024),
    @_filename nvarchar(255),
    @_filesize bigint,
    @_contentType nvarchar(128)
AS
BEGIN
    INSERT INTO tig_hfu_slots( slot_id, uploader, domain, res, filename, filesize, content_type, ts, status )
        VALUES (@_slotId, @_uploader, @_domain, @_res, @_filename, @_filesize, @_contentType, GETUTCDATE(), 0);

    SELECT @_slotId as slot_id;
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_UpdateSlot')
        DROP PROCEDURE [dbo].[Tig_HFU_UpdateSlot]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_UpdateSlot]
    @_slotId nvarchar(60)
AS
BEGIN
    update tig_hfu_slots
        set status = 1
        where slot_id = @_slotId
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_GetSlot')
        DROP PROCEDURE [dbo].[Tig_HFU_GetSlot]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_GetSlot]
    @_slotId nvarchar(60)
AS
BEGIN
    SELECT uploader, slot_id, filename, filesize, content_type, ts
        FROM tig_hfu_slots
        WHERE slot_id = @_slotId;
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_ListExpiredSlots')
        DROP PROCEDURE [dbo].[Tig_HFU_ListExpiredSlots]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_ListExpiredSlots]
    @_domain nvarchar(1024),
    @_ts datetime,
    @_limit int
AS
BEGIN
    SELECT TOP (@_limit) uploader, slot_id, filename, filesize, content_type, ts
        FROM tig_hfu_slots
        WHERE domain_fragment = CAST(@_domain AS NVARCHAR(255))
            AND ts < @_ts
            AND domain = @_domain
        ORDER BY ts;
END
-- QUERY END:
GO

-- QUERY START:
IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'Tig_HFU_RemoveExpiredSlots')
        DROP PROCEDURE [dbo].[Tig_HFU_RemoveExpiredSlots]
-- QUERY END:
GO

-- QUERY START:
CREATE PROCEDURE [dbo].[Tig_HFU_RemoveExpiredSlots]
    @_domain nvarchar(1024),
    @_ts datetime,
    @_limit int
AS
BEGIN
    SELECT TOP (@_limit) slot_id
        INTO #tig_hfu_expired_slots_temp
        FROM tig_hfu_slots
        WHERE domain_fragment = CAST(@_domain AS NVARCHAR(255))
            AND ts < @_ts
            AND domain = @_domain
        ORDER BY ts;

    DELETE FROM tig_hfu_slots
        WHERE EXISTS (
            SELECT 1
                FROM #tig_hfu_expired_slots_temp t1
                WHERE t1.slot_id = tig_hfu_slots.slot_id
        );

    DROP TABLE #tig_hfu_expired_slots_temp;
END
-- QUERY END:
GO

-- QUERY START:
exec TigSetComponentVersion 'http-api', '2.0.0';
-- QUERY END:
GO
