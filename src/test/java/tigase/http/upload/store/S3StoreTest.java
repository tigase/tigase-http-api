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
package tigase.http.upload.store;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.github.davidmoten.aws.lw.client.BaseUrlFactory;
import com.github.davidmoten.aws.lw.client.Client;
import com.github.davidmoten.aws.lw.client.HttpMethod;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.testcontainers.DockerClientFactory;
import tigase.xmpp.jid.BareJID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class S3StoreTest {

	private static S3Store store;
	private static BareJID uploaderJid;
	private static String slotId;
	private static String filename;
	private static byte[] data;
	private static S3MockContainer s3Mock = new S3MockContainer("4.5.0");
	private static Client s3Client;
	private static boolean dockerAvailable;

	@BeforeClass
	public static void setup() throws NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException {
		dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
		Assume.assumeTrue("Docker not available, skipping test", dockerAvailable);
		uploaderJid = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "test.com");
		slotId = UUID.randomUUID().toString();
		filename = generateFilename("test-");
		data = generateData();

		s3Mock.start();
		s3Client = Client.s3()
				.region(Optional.empty())
				.accessKey("test")
				.secretKey("test1")
				.baseUrlFactory(new BaseUrlFactory() {
					@Override
					public String create(String serviceName, Optional<String> region) {
						return s3Mock.getHttpEndpoint();
					}
				})
				.build();

		Logger.getGlobal().setLevel(Level.ALL);
		store = new S3Store();
		store.bucket = "test-bucket-wojtek1";
		store.autocreateBucket = true;
		store.accessKeyId = "test";
		store.secretKey = "test1";
		store.endpointUrl = s3Mock.getHttpEndpoint();
		store.beanConfigurationChanged(Collections.emptyList());
	}

	@AfterClass
	public static void cleanup() throws NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException {
		if (!dockerAvailable) {
			return;
		}
		store.s3.path(store.bucket)
				.query("list-type", "2")
				.responseAsXml()
				.childrenWithName("Contents")
				.stream()
				.map(x -> x.content("Key"))
				.forEach(key -> store.s3.path(store.bucket, key).method(HttpMethod.DELETE).execute());
		store.s3.path(store.bucket).method(HttpMethod.DELETE).execute();

		s3Mock.stop();
	}

	@Test
	public void test1_upload() throws IOException {
		store.setContent(uploaderJid, slotId, filename, data.length, Channels.newChannel(new ByteArrayInputStream(data)));
		assertTrue(s3Client.path(store.bucket, store.createKey(slotId, filename)).exists());
	}

	@Test
	public void test2_download() throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		WritableByteChannel tmp = Channels.newChannel(boas);
		ReadableByteChannel in = store.getContent(uploaderJid, slotId, filename);
		int read = 0;
		while (in.isOpen() && read >= 0) {
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			read = in.read(buffer);
			buffer.flip();
			tmp.write(buffer);
		}
		assertEquals(data.length, boas.size());
		assertArrayEquals(data, boas.toByteArray());
	}

	@Test
	public void test3_countFiles() throws IOException {
		assertEquals(1, store.count());
	}

	@Test
	public void test4_countSize() throws IOException {
		assertEquals(data.length, store.size());
	}

	@Test
	public void test5_delete() throws IOException {
		store.remove(uploaderJid, slotId);
		assertEquals(0, store.count());
	}

	static byte[] generateData() throws NoSuchAlgorithmException {
		SecureRandom random = SecureRandom.getInstanceStrong();
		byte[] data = new byte[random.nextInt(100 * 1024) + random.nextInt(1024)];
		random.nextBytes(data);
		return data;
	}

	static String generateFilename(String prefix) throws NoSuchAlgorithmException {
		StringBuilder sb = new StringBuilder(prefix);
		SecureRandom random = SecureRandom.getInstanceStrong();
		int limit = 8 + random.nextInt(8);
		return random.ints(48, 123)
				.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(limit)
				.collect(() -> sb, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}
}
