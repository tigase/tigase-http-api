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

import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.MethodSorters;
import org.junit.runners.model.Statement;
import tigase.db.*;
import tigase.util.SchemaLoader;
import tigase.xmpp.JID;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JDBCFileUploadRepositoryTest {

	private static final String PROJECT_ID = "http-api";
	private static final String VERSION = "1.3.0";

	private static String uri = System.getProperty("testDbUri");

	private static JID uploader = null;
	private static String slotId;
	private static String filename;
	private static long filesize;

	private static LocalDateTime testStart;

	@ClassRule
	public static TestRule rule = new TestRule() {
		@Override
		public Statement apply(Statement stmnt, Description d) {
			if (uri == null) {
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						Assume.assumeTrue("Ignored due to not passed DB URI!", false);
					}
				};
			}
			return stmnt;
		}
	};

	private FileUploadRepository repo;

	@BeforeClass
	public static void loadSchema() {
		if (uri.startsWith("jdbc:")) {
			String dbType;
			String dbName = null;
			String dbHostname = null;
			String dbUser = null;
			String dbPass = null;

			int idx = uri.indexOf(":", 5);
			dbType = uri.substring(5, idx);
			if ("jtds".equals(dbType)) dbType = "sqlserver";

			String rest = null;
			switch (dbType) {
				case "derby":
					dbName = uri.substring(idx+1, uri.indexOf(";"));
					cleanDerby();
					break;
				case "sqlserver":
					idx = uri.indexOf("//", idx) + 2;
					rest = uri.substring(idx);
					for (String x : rest.split(";")) {
						if (!x.contains("=")) {
							dbHostname = x;
						} else {
							String p[] = x.split("=");
							switch (p[0]) {
								case "databaseName":
									dbName = p[1];
									break;
								case "user":
									dbUser = p[1];
									break;
								case "password":
									dbPass = p[1];
									break;
								default:
									// unknown setting
									break;
							}
						}
					}
					break;
				default:
					idx = uri.indexOf("//", idx) + 2;
					rest = uri.substring(idx);
					idx = rest.indexOf("/");
					dbHostname = rest.substring(0, idx);
					rest = rest.substring(idx+1);
					idx = rest.indexOf("?");
					dbName = rest.substring(0, idx);
					rest = rest.substring(idx + 1);
					for (String x : rest.split("&")) {
						String p[] = x.split("=");
						if (p.length < 2)
							continue;
						switch (p[0]) {
							case "user":
								dbUser = p[1];
								break;
							case "password":
								dbPass = p[1];
								break;
							default:
								break;
						}
					}
					break;
			}

			Properties props = new Properties();
			if (dbType != null)
				props.put("dbType", dbType);
			if (dbName != null)
				props.put("dbName", dbName);
			if (dbHostname != null)
				props.put("dbHostname", dbHostname);
			if (dbUser != null)
				props.put("rootUser", dbUser);
			if (dbPass != null)
				props.put("rootPass", dbPass);
			if (dbUser != null)
				props.put("dbUser", dbUser);
			if (dbPass != null)
				props.put("dbPass", dbPass);

			SchemaLoader loader = SchemaLoader.newInstance(props);
			loader.validateDBConnection(props);
			loader.validateDBExists(props);
			props.put("file", "database/" + dbType + "-" + PROJECT_ID + "-schema-" + VERSION + ".sql");
			Assert.assertEquals(SchemaLoader.Result.ok, loader.loadSchemaFile(props));
			loader.shutdown(props);
		}

		uploader = JID.jidInstanceNS("ua-" + UUID.randomUUID(), "test", "tigase-1");
		slotId = UUID.randomUUID().toString();
		filename = "test1.jpg";
		filesize = 12345;
		testStart = LocalDateTime.now();
	}

	@AfterClass
	public static void cleanDerby() {
		if (uri.contains("jdbc:derby:")) {
			File f = new File("derby_test");
			if (f.exists()) {
				if (f.listFiles() != null) {
					Arrays.asList(f.listFiles()).forEach(f2 -> {
						if (f2.listFiles() != null) {
							Arrays.asList(f2.listFiles()).forEach(f3 -> f3.delete());
						}
						f2.delete();
					});
				}
				f.delete();
			}
		}
	}

	@Before
	public void setup() throws DBInitException, InstantiationException, IllegalAccessException, SQLException, ClassNotFoundException {
		if (uri == null)
			return;

		DataRepository dataRepo = RepositoryFactory.getDataRepository(null, uri, new HashMap<>());
		repo = DataSourceHelper.getDefaultClass(FileUploadRepository.class, uri).newInstance();
		repo.setDataSource(dataRepo);
	}

	@After
	public void tearDown() {
		if (uri == null)
			return;

		repo = null;
	}

	@Test
	public void test_1_allocateSlot() throws TigaseDBException, InterruptedException {
		Thread.sleep(1000);

		FileUploadRepository.Slot slot = repo.allocateSlot(uploader, slotId, filename, filesize, null);
		assertNotNull(slot);
	}

	@Test
	public void test_2_getSlot() throws TigaseDBException {
		FileUploadRepository.Slot slot = repo.getSlot(uploader.getBareJID(), slotId);
		assertNotNull(slot);
		assertEquals(uploader.getBareJID(), slot.uploader);
		assertEquals(filename, slot.filename);
		assertEquals(filesize, slot.filesize);
		assertEquals(slotId, slot.slotId);
	}

	@Test
	public void test_3_updateSlot() throws TigaseDBException {
		repo.updateSlot(uploader.getBareJID(), slotId);
	}

	@Test
	public void test_4_listExpiredSlots() throws TigaseDBException {
		List<FileUploadRepository.Slot> slots = repo.listExpiredSlots(uploader.getBareJID(), testStart, 10);
		assertEquals(1, slots.size());
	}

	@Test
	public void test_5_removeExpiredSlots() throws TigaseDBException {
		repo.removeExpiredSlots(uploader.getBareJID(), testStart, 10);
		List<FileUploadRepository.Slot> slots = repo.listExpiredSlots(uploader.getBareJID(), testStart, 10);
		assertEquals(0, slots.size());
	}
}