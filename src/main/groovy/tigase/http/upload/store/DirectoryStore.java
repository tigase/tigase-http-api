/*
 * DirectoryStore.java
 *
 * Tigase HTTP API
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

import tigase.http.upload.FileUploadComponent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.xmpp.jid.BareJID;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by andrzej on 08.08.2016.
 */
@Bean(name = "store", parent = FileUploadComponent.class, active = true, exportable = true)
public class DirectoryStore
		implements Store {

	private static final Logger log = Logger.getLogger(DirectoryStore.class.getCanonicalName());
	@ConfigField(desc = "Group user slots in directories", alias = "group-by-user")
	private boolean groupByUser = false;
	@ConfigField(desc = "Path for data storage", alias = "path")
	private String path = "data/upload";
	private Path root = Paths.get(path);

	public void setPath(String path) {
		this.path = path;
		root = Paths.get(path);
	}

	@Override
	public long count() throws IOException {
		if (!groupByUser) {
			try (Stream<Path> stream = Files.list(root)) {
				return stream.count();
			}
		} else {
			try (Stream<Path> stream = Files.list(root)) {
				return stream.mapToLong(f -> {
					try (Stream<Path> s = Files.list(f)) {
						return s.count();
					} catch (IOException ex) {
						return 0;
					}
				}).sum();
			}
		}
	}

	@Override
	public long size() throws IOException {
		try (Stream<Path> stream = Files.walk(root)) {
			return stream.filter(f -> Files.isRegularFile(f)).mapToLong(f -> {
				try {
					return Files.size(f);
				} catch (IOException ex) {
					return 0;
				}
			}).sum();
		}
	}

	@Override
	public ReadableByteChannel getContent(BareJID uploader, String slotId, String filename) throws IOException {
		Path slot = prepareSlotPath(uploader, slotId);
		Path file = slot.resolve(filename);

		if (!Files.exists(file)) {
			return null;
		}

		return FileChannel.open(file, StandardOpenOption.READ);
	}

	@Override
	public void setContent(BareJID uploader, String slotId, String filename, long size, ReadableByteChannel source)
			throws IOException {
		Path slot = prepareSlotPath(uploader, slotId);
		Path file = slot.resolve(filename);

		Files.createDirectories(slot);

		try (FileChannel destination = FileChannel.open(file, StandardOpenOption.CREATE_NEW,
														StandardOpenOption.WRITE)) {
			destination.transferFrom(source, 0, size);
		}
	}

	@Override
	public void remove(BareJID uploader, String slotId) {
		Path slot = prepareSlotPath(uploader, slotId);

		removeWithContent(slot);
	}

	protected Path prepareSlotPath(BareJID uploader, String slotId) {
		Path path = groupByUser ? root.resolve(uploader.toString()) : root;
		return path.resolve(slotId);
	}

	protected void removeWithContent(Path path) {
		try {
			if (Files.exists(path)) {
				if (Files.isDirectory(path)) {
					Files.list(path).forEach(p1 -> removeWithContent(p1));
				}

				Files.delete(path);
			}
		} catch (IOException ex) {
			log.log(Level.WARNING, "Could not remove " + path.toString(), ex);
		}
	}
}
