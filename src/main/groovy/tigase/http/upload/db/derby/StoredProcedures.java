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
package tigase.http.upload.db.derby;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by andrzej on 10.08.2016.
 */
public class StoredProcedures {

	private static final Logger log = Logger.getLogger(StoredProcedures.class.getCanonicalName());

	public static void allocateSlot(String slotId, String uploader, String domain, String res, String filename,
									Long filesize, String contentType, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"insert into tig_hfu_slots( slot_id, uploader, domain, res, filename, filesize, content_type, ts, status ) " +
							"values (?, ?, ?, ?, ?, ?, ?, ?, 0)");
			stmt.setString(1, slotId);
			stmt.setString(2, uploader);
			stmt.setString(3, domain);
			stmt.setString(4, res);
			stmt.setString(5, filename);
			stmt.setLong(6, filesize);
			stmt.setString(7, contentType);
			stmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));

			stmt.execute();

			data[0] = conn.createStatement().executeQuery("select '" + slotId + "' as slot_id from SYSIBM.SYSDUMMY1");
		} finally {
			conn.close();
		}
	}

	public static void getSlot(String slotId, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"select uploader, slot_id, filename, filesize, content_type, ts " +
							"from tig_hfu_slots where slot_id = ?");
			stmt.setString(1, slotId);

			data[0] = stmt.executeQuery();
		} finally {
			conn.close();
		}
	}

	public static void listExpiredSlots(String domain, Timestamp ts, Integer max, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"select uploader, slot_id, filename, filesize, content_type, ts " + "from tig_hfu_slots " +
							"where domain = ? " + "and ts < ? " + "order by ts offset 0 rows fetch next ? rows only");
			stmt.setString(1, domain);
			stmt.setTimestamp(2, ts);
			stmt.setInt(3, max);

			data[0] = stmt.executeQuery();
		} finally {
			conn.close();
		}
	}

	public static void removeExpiredSlots(String domain, Timestamp ts, Integer max) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			List<String> slots = new ArrayList<>();
			PreparedStatement stmt = conn.prepareStatement(
					"select slot_id " + "from tig_hfu_slots " + "where domain = ? " + "and ts < ? " +
							"order by ts offset 0 rows fetch next ? rows only");
			stmt.setString(1, domain);
			stmt.setTimestamp(2, ts);
			stmt.setInt(3, max);

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				slots.add(rs.getString(1));
			}
			rs.close();

			if (!slots.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				sb.append("delete from tig_hfu_slots where slot_id IN (");

				boolean first = true;
				for (String slot : slots) {
					if (!first) {
						sb.append(',');
					} else {
						first = false;
					}
					sb.append('\'');
					sb.append(slot);
					sb.append('\'');
				}

				sb.append(")");

				conn.createStatement().execute(sb.toString());
			}
		} finally {
			conn.close();
		}
	}

	public static void updateSlot(String slotId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement("update tig_hfu_slots set status = 1 where slot_id = ?");
			stmt.setString(1, slotId);

			stmt.execute();
		} finally {
			conn.close();
		}
	}

}
