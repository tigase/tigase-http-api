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

import com.github.davidmoten.aws.lw.client.BaseUrlFactory;
import com.github.davidmoten.aws.lw.client.Client;
import com.github.davidmoten.aws.lw.client.HttpMethod;
import com.github.davidmoten.aws.lw.client.ServiceException;
import com.github.davidmoten.aws.lw.client.xml.builder.Xml;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.xmpp.jid.BareJID;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "store", active = true, exportable = true)
public class S3Store implements Store, ConfigurationChangedAware {

	private static Logger log = Logger.getLogger(S3Store.class.getCanonicalName());

	protected Client s3;

	@ConfigField(desc = "AWS region")
	private String region;
	@ConfigField(desc = "S3 bucket")
	protected String bucket;
	@ConfigField(desc = "S3 bucket key prefix")
	private String bucketKeyPrefix;
	@ConfigField(desc = "Autocreate bucket")
	protected boolean autocreateBucket = false;
	@ConfigField(desc = "S3 access key id")
	protected String accessKeyId;
	@ConfigField(desc = "S3 secret key")
	protected String secretKey;
	@ConfigField(desc = "S3 endpoint url")
	protected String endpointUrl;

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	@Override
	public long count() throws IOException {
		try {
			return s3.path(bucket)
					.query("list-type", "2")
					.query("prefix", bucketKeyPrefix)
					.responseAsXml()
					.childrenWithName("Contents")
					.size();
		} catch (RuntimeException ex) {
			throw new IOException("Could not count files", ex);
		}
	}

	@Override
	public long size() throws IOException {
		try {
			return s3.path(bucket)
					.query("list-type", "2")
					.query("prefix", bucketKeyPrefix)
					.responseAsXml()
					.childrenWithName("Contents")
					.stream()
					.map(x -> x.content("Size"))
					.mapToLong(Long::parseLong)
					.sum();
		} catch (RuntimeException ex) {
			throw new IOException("Could not count files", ex);
		}
	}

	@Override
	public ReadableByteChannel getContent(BareJID uploader, String slotId, String filename) throws IOException {
		try {
			// encode filename to make GET request URI
			return Channels.newChannel(s3.path(bucket, createKey(slotId, URLEncoder.encode(filename, StandardCharsets.UTF_8)))
											   .responseInputStream());
		} catch (RuntimeException ex) {
			throw new IOException("Could not download the file " + slotId + " from S3", ex);
		}
	}

	@Override
	public void setContent(BareJID uploader, String slotId, String filename, long size, ReadableByteChannel source)
			throws IOException {
		var data = Channels.newInputStream(source).readAllBytes();
		try {
			// encode filename to make PUT request URI
			s3.path(bucket, createKey(slotId, URLEncoder.encode(filename, StandardCharsets.UTF_8))).method(HttpMethod.PUT).requestBody(data).execute();;
		} catch (RuntimeException ex) {
			throw new IOException("Could not upload the file " + slotId + " to S3", ex);
		}
	}

	@Override
	public void remove(BareJID uploader, String slotId) throws IOException {
		try {
			List<String> toRemove = s3.path(bucket)
					.query("list-type", "2")
					.query("prefix", createKeyPrefix(slotId))
					.responseAsXml()
					.childrenWithName("Contents")
					.stream()
					.map(x -> x.content("Key"))
					.toList();
			if (!toRemove.isEmpty()) {
				s3.path(bucket, toRemove.getFirst()).method(HttpMethod.DELETE).execute();
			}
		} catch (RuntimeException ex) {
			throw new IOException("Could not remove file " + slotId + " from S3", ex);
		}
	}

	@Override
	public void beanConfigurationChanged(Collection<String> collection) {
		log.log(Level.CONFIG, "Initiating S3 storage at " + region + ", collection: " + collection);
		s3 = createClient();
		log.log(Level.CONFIG, "Initiated S3 storage at " + s3.region() + " with " + s3);

		try {
			if (!s3.path(bucket).exists()) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "S3 bucket " + bucket + " does not exist");
				}
				if (!autocreateBucket) {
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "S3 bucket " + bucket +
								" does not exist and automatic creation of bucket is not enabled. File storage in S3 will not work!");
					}
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE,
						        "S3 bucket " + bucket + " does not exist, trying automatic creation of a bucket..");
					}
					try {
						String createXml = Xml.create("CreateBucketConfiguration")
								.a("xmlns", "http://s3.amazonaws.com/doc/2006-03-01/")
								.e("LocationConstraint")
								.content(region)
								.toString();
						s3.path(bucket).method(HttpMethod.PUT).requestBody(createXml).execute();
					} catch (RuntimeException ex) {
						if (log.isLoggable(Level.WARNING)) {
							log.log(Level.WARNING, "Automatic creation of S3 bucket " + bucket +
									" failed. File storage in S3 will not work!", ex);
						}
					}
				}
			}
		} catch (ServiceException ex) {
			log.warning("Failed to checked if S3 bucket exist, skipping bucket " + bucket + " automatic creation - possible misconfiguration or issue with accessing S3 storage!");
		}
	}

	protected Client createClient() {
		Client.Builder2 builder2 = configureRegion(Client.s3());
		Client.Builder4 builder4 = configureCredentials(builder2);
		if (endpointUrl != null) {
			builder4 = builder4.baseUrlFactory(new BaseUrlFactory() {
				@Override
				public String create(String serviceName, Optional<String> region) {
					return endpointUrl;
				}
			});
		}
		return builder4.build();
	}

	private Client.Builder4 configureCredentials(Client.Builder2 builder2) {
		if (accessKeyId != null && secretKey != null) {
			return builder2.accessKey(accessKeyId).secretKey(secretKey);
		} else {
			return builder2.credentialsFromEnvironment();
		}
	}

	protected Client.Builder2 configureRegion(Client.Builder builder) {
		if (region == null) {
			if (endpointUrl != null) {
				return builder.region("us-east-1");
			}
			return builder.regionFromEnvironment();
		} else {
			return builder.region(region);
		}
	}

	protected String createKeyPrefix(String slotId) {
		if (bucketKeyPrefix == null || bucketKeyPrefix.isBlank()) {
			return slotId;
		} else {
			return bucketKeyPrefix + "/" + slotId;
		}
	}

	protected String createKey(String slotId, String filename) {
		return createKeyPrefix(slotId) + "/" + filename;
	}
}
