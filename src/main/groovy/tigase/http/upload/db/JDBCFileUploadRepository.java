/*
 * Tigase HTTP API component - Tigase HTTP API component
 * Copyright (C) 2013 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.http.upload.db;

import tigase.db.DataRepository;
import tigase.db.Repository;
import tigase.db.TigaseDBException;
import tigase.db.util.RepositoryVersionAware;
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
@Repository.Meta(supportedUris = {"jdbc:[^:]+:.*"})
@Repository.SchemaId(id = Schema.HTTP_UPLOAD_SCHEMA_ID, name = Schema.HTTP_UPLOAD_SCHEMA_NAME)
public class JDBCFileUploadRepository
		implements FileUploadRepository<DataRepository>, RepositoryVersionAware {

	private static final String DEF_ALLOCATE_SLOT = "{ call Tig_HFU_AllocateSlot(?, ?, ?, ?, ?, ?, ?) }";
	//	private static final String DEF_GET_TRANSFER_USED = "{ call Tig_HFU_GetTransferUsed(?, ?, ?, ?) }";
	private static final String DEF_UPDATE_SLOT = "{ call Tig_HFU_UpdateSlot(?) }";
	private static final String DEF_GET_SLOT = "{ call Tig_HFU_GetSlot(?) }";
	private static final String DEF_LIST_EXPIRED_SLOTS = "{ call Tig_HFU_ListExpiredSlots(?,?,?) }";
	private static final String DEF_REMOVE_EXPIRED_SLOTS = "{ call Tig_HFU_RemoveExpiredSlots(?,?,?) }";

	private static final String DEF_COUNT_SPACE_USED_USER = "{ call Tig_HFU_UsedSpaceCountForUser(?) }";
	private static final String DEF_COUNT_SPACE_USED_DOMAIN = "{ call Tig_HFU_UsedSpaceCountForDomain(?) }";
	private static final String DEF_LIST_USER_SLOTS = "{ call Tig_HFU_UserSlotsQuery(?,?,?) }";
	private static final String DEF_LIST_DOMAIN_SLOTS = "{ call Tig_HFU_DomainSlotsQuery(?,?,?) }";
	private static final String DEF_REMOVE_SLOT = "{ call Tig_HFU_RemoveSlot(?) }";

	@ConfigField(desc = "Query to allocate slot", alias = "allocate-slot-query")
	private String ALLOCATE_SLOT_QUERY = DEF_ALLOCATE_SLOT;

	@ConfigField(desc = "Query to retrieve slot metadata", alias = "get-slot-query")
	private String GET_SLOT_QUERY = DEF_GET_SLOT;

//	@ConfigField(desc = "Query to calculate used transfer", alias = "get-transfer-used-query")
//	private String GET_TRANSFER_USED_QUERY = DEF_GET_TRANSFER_USED;
	@ConfigField(desc = "Query to list expired slots", alias = "list-expired-slots-query")
	private String LIST_EXPIRED_SLOTS_QUERY = DEF_LIST_EXPIRED_SLOTS;
	@ConfigField(desc = "Query to remove expired slots", alias = "remove-expired-slots-query")
	private String REMOVE_EXPIRED_SLOTS_QUERY = DEF_REMOVE_EXPIRED_SLOTS;
	@ConfigField(desc = "Query to update slot on file upload", alias = "update-slot-query")
	private String UPDATE_SLOT_QUERY = DEF_UPDATE_SLOT;

	@ConfigField(desc = "Query to count space used by user", alias = "count-space-used-user-query")
	private String COUNT_SPACE_USED_USER_QUERY = DEF_COUNT_SPACE_USED_USER;
	@ConfigField(desc = "Query to count space used by domain", alias = "count-space-used-domain-query")
	private String COUNT_SPACE_USED_DOMAIN_QUERY = DEF_COUNT_SPACE_USED_DOMAIN;
	@ConfigField(desc = "Query to list slots owned by user", alias = "list-user-slots-query")
	private String LIST_USER_SLOTS_QUERY = DEF_LIST_USER_SLOTS;
	@ConfigField(desc = "Query to list slots owner by domain", alias = "list-domain-slots-query")
	private String LIST_DOMAIN_SLOTS_QUERY = DEF_LIST_DOMAIN_SLOTS;
	@ConfigField(desc = "Query remove slot", alias = "remove-slot-query")
	private String REMOVE_SLOT_QUERY = DEF_REMOVE_SLOT;

	private DataRepository repo;

	@Override
	public Slot allocateSlot(JID sender, String slotId, String filename, long filesize, String contentType)
			throws TigaseDBException {
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
					repo.setTimestamp(stmt, 2,
									  new Timestamp(before.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
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
					repo.setTimestamp(stmt, 2,
									  new Timestamp(before.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
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
	public long getUsedSpaceForDomain(String domain) throws TigaseDBException {
		try {
			PreparedStatement stmt = repo.getPreparedStatement(BareJID.bareJIDInstanceNS(domain), COUNT_SPACE_USED_DOMAIN_QUERY);

			synchronized (stmt) {
				stmt.setString(1, domain);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return rs.getLong(1);
					}
					return -1;
				}
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not count used space for domain", ex);
		}
	}

	@Override
	public long getUsedSpaceForUser(BareJID user) throws TigaseDBException {
		try {
			PreparedStatement stmt = repo.getPreparedStatement(user, COUNT_SPACE_USED_USER_QUERY);

			synchronized (stmt) {
				stmt.setString(1, user.toString());
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return rs.getLong(1);
					}
					return -1;
				}
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not count used space for user", ex);
		}
	}

	@Override
	public List<Slot> querySlots(String domain, String afterId, int limit) throws TigaseDBException {
		try {
			PreparedStatement stmt = repo.getPreparedStatement(BareJID.bareJIDInstanceNS(domain), LIST_DOMAIN_SLOTS_QUERY);

			synchronized (stmt) {
				stmt.setString(1, domain);
				stmt.setString(2, afterId);
				stmt.setInt(3, limit);
				List<Slot> slots = new ArrayList<>();
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						String slotId = rs.getString(1);
						String filename = rs.getString(2);
						long size = rs.getLong(3);
						String contentType = rs.getString(4);
						Date ts = rs.getTimestamp(5);
						BareJID jid = BareJID.bareJIDInstanceNS(rs.getString(6));

						slots.add(new Slot(jid, slotId, filename, size, contentType, ts));
					}
				}
				return slots;
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not count used space for user", ex);
		}
	}

	@Override
	public List<Slot> querySlots(BareJID user, String afterId, int limit) throws TigaseDBException {
		try {
			PreparedStatement stmt = repo.getPreparedStatement(user, LIST_USER_SLOTS_QUERY);

			synchronized (stmt) {
				stmt.setString(1, user.toString());
				stmt.setString(2, afterId);
				stmt.setInt(3, limit);
				List<Slot> slots = new ArrayList<>();
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						String slotId = rs.getString(1);
						String filename = rs.getString(2);
						long size = rs.getLong(3);
						String contentType = rs.getString(4);
						Date ts = rs.getTimestamp(5);
						BareJID jid = BareJID.bareJIDInstanceNS(rs.getString(6));

						slots.add(new Slot(jid, slotId, filename, size, contentType, ts));
					}
				}
				return slots;
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not count used space for user", ex);
		}
	}

	@Override
	public void removeSlot(BareJID user, String slotId) throws TigaseDBException {
		try {
			PreparedStatement stmt = repo.getPreparedStatement(user, REMOVE_SLOT_QUERY);

			synchronized (stmt) {
				stmt.setString(1, slotId);
				stmt.executeUpdate();
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Could not remove slot", ex);
		}
	}
	
	@Override
	public void setDataSource(DataRepository dataSource) {
		try {
			dataSource.initPreparedStatement(ALLOCATE_SLOT_QUERY, ALLOCATE_SLOT_QUERY);
			dataSource.initPreparedStatement(UPDATE_SLOT_QUERY, UPDATE_SLOT_QUERY);
			dataSource.initPreparedStatement(GET_SLOT_QUERY, GET_SLOT_QUERY);
			dataSource.initPreparedStatement(LIST_EXPIRED_SLOTS_QUERY, LIST_EXPIRED_SLOTS_QUERY);
			dataSource.initPreparedStatement(REMOVE_EXPIRED_SLOTS_QUERY, REMOVE_EXPIRED_SLOTS_QUERY);
			dataSource.initPreparedStatement(COUNT_SPACE_USED_DOMAIN_QUERY, COUNT_SPACE_USED_DOMAIN_QUERY);
			dataSource.initPreparedStatement(COUNT_SPACE_USED_USER_QUERY,COUNT_SPACE_USED_USER_QUERY);
			dataSource.initPreparedStatement(LIST_USER_SLOTS_QUERY, LIST_USER_SLOTS_QUERY);
			dataSource.initPreparedStatement(LIST_DOMAIN_SLOTS_QUERY, LIST_DOMAIN_SLOTS_QUERY);
			dataSource.initPreparedStatement(REMOVE_SLOT_QUERY, REMOVE_SLOT_QUERY);
			repo = dataSource;
		} catch (SQLException ex) {
			throw new RuntimeException("Could not initialize repository", ex);
		}
	}
}
