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

import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceHelper;
import tigase.db.TigaseDBException;
import tigase.db.beans.MDRepositoryBean;
import tigase.http.upload.FileUploadComponent;
import tigase.kernel.beans.Bean;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by andrzej on 10.08.2016.
 */
@Bean(name = "repositoryPool", parent = FileUploadComponent.class, active = true, exportable = true)
public class FileUploadRepositoryPool<R extends FileUploadRepository<DataSource>>
		extends MDRepositoryBean<R>
		implements FileUploadRepository {

	@Override
	public Slot allocateSlot(BareJID sender, String slotId, String filename, long filesize, String contentType)
			throws TigaseDBException {
		return getRepository(sender.getDomain()).allocateSlot(sender, slotId, filename, filesize, contentType);
	}

	@Override
	public Slot allocateSlot(JID sender, String slotId, String filename, long filesize, String contentType)
			throws TigaseDBException {
		return getRepository(sender.getDomain()).allocateSlot(sender, slotId, filename, filesize, contentType);
	}

	@Override
	public void updateSlot(BareJID sender, String slotId) throws TigaseDBException {
		getRepository(sender.getDomain()).updateSlot(sender, slotId);
	}

	@Override
	public Slot getSlot(BareJID sender, String slotId) throws TigaseDBException {
		if (sender != null) {
			return getRepository(sender.getDomain()).getSlot(sender, slotId);
		} else {
			for (FileUploadRepository repo : getRepositories().values()) {
				Slot slot = repo.getSlot(null, slotId);
				if (slot != null) {
					return slot;
				}
			}
		}
		return null;
	}

	@Override
	public List<Slot> listExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException {
		return getRepository(domain.getDomain()).listExpiredSlots(domain, before, limit);
	}

	@Override
	public void removeExpiredSlots(BareJID domain, LocalDateTime before, int limit) throws TigaseDBException {
		getRepository(domain.getDomain()).removeExpiredSlots(domain, before, limit);
	}

	@Override
	public long getUsedSpaceForDomain(String domain) throws TigaseDBException {
		return getRepository(domain).getUsedSpaceForDomain(domain);
	}

	@Override
	public long getUsedSpaceForUser(BareJID user) throws TigaseDBException {
		return getRepository(user.getDomain()).getUsedSpaceForUser(user);
	}

	@Override
	public List<Slot> querySlots(BareJID user, String afterId, int limit) throws TigaseDBException {
		return getRepository(user.getDomain()).querySlots(user, afterId, limit);
	}

	@Override
	public List<Slot> querySlots(String domain, String afterId, int limit) throws TigaseDBException {
		return getRepository(domain).querySlots(domain, afterId, limit);
	}

	@Override
	public void removeSlot(BareJID user, String slotId) throws TigaseDBException {
		getRepository(user.getDomain()).removeSlot(user, slotId);
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		// nothing to do
	}

	@Override
	public Class<?> getDefaultBeanClass() {
		return FileUploadRepositoryConfigBean.class;
	}

	@Override
	protected Class findClassForDataSource(DataSource dataSource) throws DBInitException {
		return DataSourceHelper.getDefaultClass(FileUploadRepository.class, dataSource.getResourceUri());
	}

	public static class FileUploadRepositoryConfigBean
			extends MDRepositoryConfigBean<FileUploadRepository<DataSource>> {

	}
}
