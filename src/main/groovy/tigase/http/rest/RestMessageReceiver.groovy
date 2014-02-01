/*
 * Tigase HTTP API
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
package tigase.http.rest

import tigase.db.AuthRepository
import tigase.db.UserRepository
import tigase.http.HttpServer
import tigase.db.comp.ComponentRepository;
import tigase.server.AbstractMessageReceiver
import tigase.server.Packet
import tigase.server.Permissions
import tigase.xmpp.Authorization
import tigase.xmpp.BareJID
import tigase.xmpp.JID

import javax.script.Bindings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import org.eclipse.jetty.servlet.ServletContextHandler;
import tigase.http.security.TigasePlainLoginService;
import org.eclipse.jetty.servlet.ServletHolder;

public class RestMessageReceiver extends AbstractMessageReceiver implements  Service {

    private static final String DEFAULT_CONTEXT_VAL = "/rest";
    private static final String DEFAULT_REST_SCRIPTS_DIRECTORY_VAL = "scripts/rest";	
	
    private static final String CONTEXT_KEY = "context";	
    private static final String REST_SCRIPTS_DIRECTORY_KEY = "rest-scripts-dir";	
	
    private ScheduledExecutorService scheduler;
    private ConcurrentHashMap pendingRequest = new ConcurrentHashMap();

    private UserRepository user_repo_impl = null;
    private AuthRepository auth_repo_impl = null;
	private ApiKeyRepository apiKeyRepository = null;

	private HttpServer httpServer = new HttpServer();
	private ServletContextHandler httpContext = null;
	
    private static String context = DEFAULT_CONTEXT_VAL;	
    private static String scriptsDir = DEFAULT_REST_SCRIPTS_DIRECTORY_VAL;

    @Override
    void processPacket(Packet packet) {
        String uuid = packet.getTo().getResource();
        String id = packet.getAttribute("id");
        String key = generateKey(uuid, id);

        // check if we send this request and if we are waiting for response process it
        def request = pendingRequest.remove(key);
        if (request) {
            request.future.cancel(false);
            request.closure(packet);
        }
        else {
            // we can only process response we are waiting for so return error if packet is not expected
            addOutPacket(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, false));
        }
    }

    @Override
    public void start() {
        scheduler = Executors.newScheduledThreadPool(2);
        super.start();
    }

    public void stop() {
        scheduler.shutdown();
        super.stop();
		deinitializeContext();
        httpServer.stop();
    }

    @Override
    public void sendPacket(Packet packet, Long timeout, Closure closure) {
        String uuid = UUID.randomUUID().toString();
        JID from = getComponentId().copyWithResource(uuid);
        if (packet.getStanzaFrom() == null) {
            packet.initVars(from, packet.getStanzaTo());
        }
        packet.setPacketFrom(from);

        if (packet.getStanzaFrom() != null && !this.isLocalDomainOrComponent(packet.getStanzaFrom().toString())) {
            if (this.isAdmin(packet.getStanzaFrom())) {
                packet.setPermissions(Permissions.ADMIN);
            }
            else {
                packet.setPermissions(Permissions.AUTH);
            }
        }

        String id = packet.getAttribute("id");
        String key = generateKey(uuid, id);

        if (closure != null) {
            // queue callback if we need result
            if (timeout == null || timeout < 0) timeout = 30;

            def request = new Request();
            request.closure = closure;
            request.future = scheduler.schedule({
                requestTimedOut(key);
            }, timeout, TimeUnit.SECONDS);

            pendingRequest.put(key, request);
        }

        // send packet
        addOutPacket(packet);
    }

    @Override
    public void initBindings(Bindings init) {
        super.initBindings(init);
        init.put("restMessageReceiver", this);
		init.put(ComponentRepository.COMP_REPO_BIND, apiKeyRepository);		
    }

    public void reloadRestHandlers() {
//        httpServer.stop();
//        httpServer.start();
		initializeContext(null);
    }

    @Override
    public UserRepository getUserRepository() {
        return user_repo_impl;
    }

    @Override
    public AuthRepository getAuthRepository() {
        return auth_repo_impl;
    }

	@Override
	public boolean isAllowed(String key, String host, String path) {
		return apiKeyRepository.isAllowed(key, host, path);
	}
	
    @Override
    public boolean isAdmin(BareJID user) {
        return isAdmin(JID.jidInstance(user));
    }

    public void requestTimedOut(String key) {
        def request = pendingRequest.remove(key);
        if (!request) return;

        request.closure(null);
    }

    @Override
    public Map<String,Object> getDefaults(Map<String,Object> params) {
        Map<String,Object> props = super.getDefaults(params);
		if (apiKeyRepository) {
			apiKeyRepository.getDefaults(props, params);
		}
		else {
			ApiKeyRepository apiKeyRepo = new ApiKeyRepository();
			apiKeyRepo.getDefaults(props, params);			
		}
		props.putAll(httpServer.getDefaults());
        return props;
    }

    @Override
    public void setProperties(Map<String,Object> props) {
        super.setProperties(props);

        if (props.size() == 1) return;

        user_repo_impl = props.get(SHARED_USER_REPO_PROP_KEY);
        auth_repo_impl = props.get(SHARED_AUTH_REPO_PROP_KEY);

		if (apiKeyRepository == null) {
			apiKeyRepository = new ApiKeyRepository();
		}
		apiKeyRepository.setRepoUser(BareJID.bareJIDInstanceNS(getName()));
		apiKeyRepository.setProperties(props);
		
        httpServer.setProperties(props);
        httpServer.start();
		
		initializeContext(props);
    }

	private void initializeContext(Map<String,Object> props) {
		if (props.get(CONTEXT_KEY)) {
			context = props.get(CONTEXT_KEY);
		}
		
		if (props.get(REST_SCRIPTS_DIRECTORY_KEY)) {
			scriptsDir = props.get(REST_SCRIPTS_DIRECTORY_KEY);
		}
		
		if (httpContext) {
			deinitializeContext();
		}
		
        httpContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        httpContext.setSecurityHandler(httpContext.getDefaultSecurityHandlerClass().newInstance());
        httpContext.getSecurityHandler().setLoginService(new TigasePlainLoginService());
        httpContext.setContextPath(context);

        File scriptsDirFile = new File(scriptsDir);
        File[] scriptDirFiles = scriptsDirFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        if (scriptDirFiles != null) {
            for (File dirFile : scriptDirFiles) {
                startRestServletForDirectory(httpContext, dirFile);
            }
        }

        httpServer.registerContext(httpContext);		
	}
	
	private void deinitializeContext() {
		if (httpContext) {
			httpServer.unregisterContext(httpContext);
			httpContext = null;
		}		
	}

    private void startRestServletForDirectory(ServletContextHandler httpContext, File scriptsDirFile) {
        File[] scriptFiles = scriptsDirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith("groovy");
            }
        });

        if (scriptFiles != null) {
            RestSerlvet restServlet = new RestSerlvet();
			restServlet.setService(this);
            httpContext.addServlet(new ServletHolder(restServlet),"/" + scriptsDirFile.getName() + "/*");
            restServlet.loadHandlers(scriptFiles);
        }
    }		
	
    /**
     * Generates key for setting use in pending requests map
     *
     * @param uuid
     * @param key
     * @return
     */
    private String generateKey(String uuid, String key) {
        return uuid + "-" + key;
    }

    private class Request {
        def future;
        def closure;
    }
}
