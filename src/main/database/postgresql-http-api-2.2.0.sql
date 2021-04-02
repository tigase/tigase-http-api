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
create or replace function Tig_HFU_UsedSpaceCountForUser(_jid varchar(2049))
              returns int as $$
begin
    return (select sum(filesize) from tig_hfu_slots where uploader = _jid);
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_UsedSpaceCountForDomain(_domain varchar(1024))
              returns int as $$
begin
    return (select sum(filesize) from tig_hfu_slots where domain = _domain);
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_UserSlotsQuery(_jid varchar(2049), _afterId varchar(60), _limit int) returns table(
    slot_id varchar(60), filename varchar(255), filesize bigint, content_type varchar(128), ts timestamp with time zone, uploader varchar(2049)
) as $$
begin
    if _afterId is null then
        return query select s.slot_id, s.filename, s.filesize, s.content_type, s.ts, s.uploader
        from tig_hfu_slots s
        where
            s.uploader = _jid
        order by s.ts asc
        limit _limit;
    else
        return query select s.slot_id, s.filename, s.filesize, s.content_type, s.ts, s.uploader
        from tig_hfu_slots s
        where
            s.uploader = _jid
            and s.ts > (
                select s1.ts
                from tig_hfu_slots s1
                where s1.slot_id = _afterId
            )
        order by s.ts asc
        limit _limit;
    end if;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_DomainSlotsQuery(_domain varchar(1024), _afterId varchar(60), _limit int) returns table(
    slot_id varchar(60), filename varchar(255), filesize bigint, content_type varchar(128), ts timestamp with time zone, uploader varchar(2049)
) as $$
begin
    if _afterId is null then
        return query select s.slot_id, s.filename, s.filesize, s.content_type, s.ts, s.uploader
        from tig_hfu_slots s
        where
                s.domain = _domain
        order by s.ts asc
        limit _limit;
    else
        return query select s.slot_id, s.filename, s.filesize, s.content_type, s.ts, s.uploader
        from tig_hfu_slots s
        where
                s.domain = _domain
          and s.ts > (
            select s1.ts
            from tig_hfu_slots s1
            where s1.slot_id = _afterId
        )
        order by s.ts asc
        limit _limit;
    end if;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END:

-- QUERY START:
create or replace function Tig_HFU_RemoveSlot(_slotId varchar(60)) returns void as $$
begin
    delete from tig_hfu_slots where slot_id = _slotId;
end;
$$ LANGUAGE 'plpgsql';
-- QUERY END: