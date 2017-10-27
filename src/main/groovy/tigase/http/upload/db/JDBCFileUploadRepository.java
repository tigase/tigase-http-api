/*
 * Tigase HTTP API
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.http.upload.db;

import tigase.db.DataRepository;
import tigase.db.Repository;
import tigase.db.TigaseDBException;
import tigase.http.db.Schema;
import tigase.kernel.beans.config.ConfigField;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by andrzej on 07.08.2016.
 */
@Repository.Meta( supportedUris = { "jdbc:[^:]+:.*" } )
@Repository.SchemaId(id = Schema.HTTP_UPLOAD_SCHEMA_ID, name = Schema.HTTP_UPLOAD_SCHEMA_NAME)
public class JDBCFileUploadRepository implements FileUploadRepository<DataRepository> {

	private static final String DEF_ALLOCATE_SLOT = "{ call Tig_HFU_AllocateSlot(?, ?, ?, ?, ?, ?, ?) }";
//	private static final String DEF_GET_TRANSFER_USED = "{ call Tig_HFU_GetTransferUsed(?, ?, ?, ?) }";
	private static final String DEF_UPDATE_SLOT = "{ call Tig_HFU_UpdateSlot(?) }";
	private static final String DEF_GET_SLOT = "{ call Tig_HFU_GetSlot(?) }";
	private static final String DEF_LIST_EXPIRED_SLOTS = "{ call Tig_HFU_ListExpiredSlots(?,?,?) }";
	private static final String DEF_REMOVE_EXPIRED_SLOTS = "{ call Tig_HFU_RemoveExpiredSlots(?,?,?) }";

	@ConfigField(desc = "Query to allocate slot", alias = "allocate-slot-query")
	private String ALLOCATE_SLOT_QUERY = DEF_ALLOCATE_SLOT;

	@ConfigField(desc = "Query to retrieve slot metadata", alias = "get-slot-query")
	private String GET_SLOT_QUERY = DEF_GET_SLOT;

//	@ConfigField(desc = "Query to calculate used transfer", alias = "get-transfer-used-query")
//	private String GET_TRANSFER_USED_QUERY = DEF_GET_TRANSFER_USED;

	@ConfigField(desc = "Query to update slot on file upload", alias = "update-slot-query")
	private String UPDATE_SLOT_QUERY = DEF_UPDATE_SLOT;

	@ConfigField(desc = "Query to list expired slots", alias = "list-expired-slots-query")
	private String LIST_EXPIRED_SLOTS_QUERY = DEF_LIST_EXPIRED_SLOTS;

	@ConfigField(desc = "Query to remove expired slots", alias = "remove-expired-slots-query")
	private String REMOVE_EXPIRED_SLOTS_QUERY = DEF_REMOVE_EXPIRED_SLOTS;

	private DataRepository repo;

//	@Override
//	public long[] getTransferUsed(BareJID userJid, Date from, Date to) throws TigaseDBException {
//		long[] result = { 0, 0 };
//		try {
//			PreparedStatement stmt = repo.getPreparedStatement(userJid, GET_TRANSFER_USED_QUERY);
//			ResultSet rs = null;
//			synchronized (stmt) {
//				try {
//					stmt.setString(1, userJid.toString());
//					stmt.setString(2, userJid.getDomain());
//					stmt.setTimestamp(3, from != null ? new Timestamp(from.getTime()) : null);
//					stmt.setTimestamp(4, to != null ? new Timestamp(to.getTime()) : null);
//
//					rs = stmt.executeQuery();
//					if (rs.next()) {
//						result[0] = rs.getLong(1);
//						result[1] = rs.getLong(2);
//					}
//				} finally {
//					repo.release(null, rs);
//				}
//			}
//		} catch (SQLException ex) {
//			throw new TigaseDBException("Could not calculated used transfer", ex);
//		}
//		return result;
//	}

	@Override
	public Slot allocateSlot(JID sender, String slotId, String filename, long filesize, String contentType) throws TigaseDBException {
		BareJID bareJid = sender.getBareJID();
		try {
			PreparedStatement stmt = repo.getPreparedStatement(bareJid, ALLOCATE_SLOT_QUERY);
			ResultSet rs = null;
			synchronized (stmt) {
				try {
					stmt.setString(1, slotId);
					stmt.setString(2, bareJid.toString());
					stmt.setString(3, bareJid.getDomain());
					stmt.setString(4, sender.getResource());
					stmt.setString(5, filename);
					stmt.setLong(6, filesize);
					stmt.setString(7, contentType);

					rs = stmt.executeQuery();
					if (rs.next()) {
						String id = rs.getString(1);
						return new Slot(bareJid, slotId, filename, filesize, contentType, new Date());
					}
				} finally {
					repo.release(null, rs);
				}
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not allocate slot", ex);
		}
		return null;
	}

	@Override
	public void updateSlot(BareJID userJid, String slotId) throws TigaseDBException {
		try {
			PreparedStatement stmt = repo.getPreparedStatement(userJid, UPDATE_SLOT_QUERY);
			ResultSet rs = null;
			synchronized (stmt) {
				try {
					stmt.setString(1, slotId);

					stmt.executeUpdate();
				} finally {
					repo.release(null, rs);
				}
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not calculated used transfer", ex);
		}
	}

	@Override
	public Slot getSlot(BareJID sender, String slotId) throws TigaseDBException {
		try {
			PreparedStatement stmt = repo.getPreparedStatement(sender, GET_SLOT_QUERY);
			ResultSet rs = null;
			synchronized (stmt) {
				try {
					stmt.setString(1, slotId);

					rs = stmt.executeQuery();
					if (rs.next()) {
						BareJID jid = BareJID.bareJIDInstanceNS(rs.getString(1));
						String filename = rs.getString(3);
						long filesize = rs.getLong(4);
						String contentType = rs.getString(5);
						Date ts = repo.getTimestamp(rs, 6);

						return new Slot(jid, slotId, filename, filesize, contentType, ts);
					}
				} finally {
					repo.release(null, rs);
				}
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not allocate slot", ex);
		}
		return null;
	}

	@Override
	public List<Slot> listExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException {
		List<Slot> results = new ArrayList<>();
		try {
			PreparedStatement stmt = repo.getPreparedStatement(domain, LIST_EXPIRED_SLOTS_QUERY);
			ResultSet rs = null;
			synchronized (stmt) {
				try {
					stmt.setString(1, domain.getDomain());
					repo.setTimestamp(stmt, 2, new Timestamp(before.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
					stmt.setInt(3, limit);

					rs = stmt.executeQuery();
					while (rs.next()) {
						BareJID jid = BareJID.bareJIDInstanceNS(rs.getString(1));
						String slotId = rs.getString(2);
						String filename = rs.getString(3);

						results.add(new Slot(jid, slotId, filename, -1, null, null));
					}
				} finally {
					repo.release(null, rs);
				}
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not allocate slot", ex);
		}
		return results;
	}

	@Override
	public void removeExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException {
		try {
			PreparedStatement stmt = repo.getPreparedStatement(domain, REMOVE_EXPIRED_SLOTS_QUERY);
			ResultSet rs = null;
			synchronized (stmt) {
				try {
					stmt.setString(1, domain.getDomain());
					repo.setTimestamp(stmt,2, new Timestamp(before.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
					stmt.setInt(3, limit);

					stmt.execute();
				} finally {
					repo.release(null, rs);
				}
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not allocate slot", ex);
		}
	}

	@Override
	public void setDataSource(DataRepository dataSource) {
		try {
			dataSource.checkSchemaVersion( this );
			dataSource.initPreparedStatement(ALLOCATE_SLOT_QUERY, ALLOCATE_SLOT_QUERY);
			dataSource.initPreparedStatement(UPDATE_SLOT_QUERY, UPDATE_SLOT_QUERY);
			dataSource.initPreparedStatement(GET_SLOT_QUERY, GET_SLOT_QUERY);
			dataSource.initPreparedStatement(LIST_EXPIRED_SLOTS_QUERY, LIST_EXPIRED_SLOTS_QUERY);
			dataSource.initPreparedStatement(REMOVE_EXPIRED_SLOTS_QUERY, REMOVE_EXPIRED_SLOTS_QUERY);
			repo = dataSource;
		} catch (SQLException ex) {
			throw new RuntimeException("Could not initialize repository", ex);
		}
	}
}
