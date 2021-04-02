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

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import tigase.db.AbstractDataSourceAwareTestCase;
import tigase.db.DataSource;
import tigase.db.DataSourceAware;
import tigase.db.TigaseDBException;
import tigase.xmpp.jid.JID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractFileUploadRepositoryTest<DS extends DataSource> extends AbstractDataSourceAwareTestCase<DS,FileUploadRepository> {

	protected static final String PROJECT_ID = "http-api";
	protected static final String VERSION = "2.2.0-SNAPSHOT";
	private static String filename;
	private static long filesize;
	private static String slotId;
	private static LocalDateTime testStart;
	private static JID uploader = null;

	@BeforeClass
	public static void setupFileUploadTest() {
		uploader = JID.jidInstanceNS("ua-" + UUID.randomUUID(), "test", "tigase-1");
		slotId = UUID.randomUUID().toString();
		filename = "test1.jpg";
		filesize = 12345;
		testStart = LocalDateTime.now();
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
	public void test_4_countUsedSpaceUser() throws TigaseDBException {
		assertEquals(filesize, repo.getUsedSpaceForUser(uploader.getBareJID()));
	}

	@Test
	public void test_4_countUsedSpaceDomain() throws TigaseDBException {
		assertEquals(filesize, repo.getUsedSpaceForDomain(uploader.getDomain()));
	}

	@Test
	public void test_4_querySlotsUser() throws TigaseDBException {
		List<FileUploadRepository.Slot> slots = repo.querySlots(uploader.getBareJID(), null , 10);
		assertEquals(1, slots.size());
		FileUploadRepository.Slot slot = slots.get(0);
		assertEquals(uploader.getBareJID(), slot.uploader);
		assertEquals(filename, slot.filename);
		assertEquals(filesize, slot.filesize);
		assertEquals(slotId, slot.slotId);
	}

	@Test
	public void test_4_querySlotsDomain() throws TigaseDBException {
		List<FileUploadRepository.Slot> slots = repo.querySlots(uploader.getDomain(), null , 10);
		assertEquals(1, slots.size());
		FileUploadRepository.Slot slot = slots.get(0);
		assertEquals(uploader.getBareJID(), slot.uploader);
		assertEquals(filename, slot.filename);
		assertEquals(filesize, slot.filesize);
		assertEquals(slotId, slot.slotId);
	}

	@Test
	public void test_4_querySlotsUserAfter() throws TigaseDBException {
		List<FileUploadRepository.Slot> slots = repo.querySlots(uploader.getBareJID(), slotId , 10);
		assertEquals(0, slots.size());
	}

	@Test
	public void test_4_querySlotsDomainAfter() throws TigaseDBException {
		List<FileUploadRepository.Slot> slots = repo.querySlots(uploader.getDomain(), slotId , 10);
		assertEquals(0, slots.size());
	}
	
	@Test
	public void test_4_listExpiredSlots() throws TigaseDBException, InterruptedException {
		Thread.sleep(1000);
		List<FileUploadRepository.Slot> slots = repo.listExpiredSlots(uploader.getBareJID(), LocalDateTime.now(), 10);
		assertEquals(1, slots.size());
	}

	@Test
	public void test_5_removeExpiredSlots() throws TigaseDBException {
		repo.removeExpiredSlots(uploader.getBareJID(), LocalDateTime.now(), 10);
		List<FileUploadRepository.Slot> slots = repo.listExpiredSlots(uploader.getBareJID(), LocalDateTime.now(), 10);
		assertEquals(0, slots.size());
	}

	@Test
	public void test_6_removeSlot() throws TigaseDBException {
		FileUploadRepository.Slot slot = repo.allocateSlot(uploader, slotId, filename, filesize, null);
		assertNotNull(slot);

		repo.removeSlot(uploader.getBareJID(), slotId);
		assertNull(repo.getSlot(uploader.getBareJID(), slotId));
	}

	@Override
	protected Class<? extends DataSourceAware> getDataSourceAwareIfc() {
		return FileUploadRepository.class;
	}
	
}
