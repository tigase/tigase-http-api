/*
 * Tigase HTTP API
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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
package tigase.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import tigase.http.rest.RestMessageReceiver;
import tigase.http.rest.RestSerlvet;
import tigase.http.rest.Service;
import tigase.http.security.TigasePlainLoginService;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServer {

    private static final Logger log = Logger.getLogger(HttpServer.class.getCanonicalName());

    private static final String PORT_KEY = "port";
    private static final String CONTEXT_KEY = "context";
    private static final String CONTEXT_FILE_PATH_KEY = "context-file-path";
    private static final String USE_LOCAL_SERVER_KEY = "use-local-server";
    private static final String REST_SCRIPTS_DIRECTORY_KEY = "rest-scripts-dir";

    private static final int DEFAULT_PORT_VAL = 8080;
    private static final String DEFAULT_CONTEXT_VAL = "/rest";
    private static final String DEFAULT_CONTEXT_FILE_PATH_VAL = "etc/tigase-http-context.xml";
    private static final String DEFAULT_REST_SCRIPTS_DIRECTORY_VAL = "scripts/rest";

    private static Server localServer;

    private static int port = DEFAULT_PORT_VAL;
    private static String context = DEFAULT_CONTEXT_VAL;
    private static String contextFilePath = DEFAULT_CONTEXT_FILE_PATH_VAL;
    private static boolean useLocal = true;
    private static String scriptsDir = DEFAULT_REST_SCRIPTS_DIRECTORY_VAL;

    private static boolean osgi = false;
    private static HttpRegistrator osgiHttpRegistrator;
    private static HttpRegistrator localHttpRegistrator;

    public static void setOSGi(boolean osgi_) {
        osgi = osgi_;
    }

    public static void setOsgiHttpRegistrator(HttpRegistrator registrator) {
        osgiHttpRegistrator = registrator;
    }

    public static Map<String,Object> getDefaults(Map<String,Object> params, Map<String,Object> props) {
        props.put(PORT_KEY, DEFAULT_PORT_VAL);
        props.put(CONTEXT_KEY, DEFAULT_CONTEXT_VAL);
        props.put(CONTEXT_FILE_PATH_KEY, DEFAULT_CONTEXT_FILE_PATH_VAL);
        props.put(USE_LOCAL_SERVER_KEY, !osgi);
        props.put(REST_SCRIPTS_DIRECTORY_KEY, DEFAULT_REST_SCRIPTS_DIRECTORY_VAL);

        return props;
    }

    public static void setProperties(Map<String,Object> props) {
        useLocal = (Boolean) props.get(USE_LOCAL_SERVER_KEY);
        port = (Integer) props.get(PORT_KEY);
        contextFilePath = (String) props.get(CONTEXT_FILE_PATH_KEY);
        context = (String) props.get(CONTEXT_KEY);
        scriptsDir = (String) props.get(REST_SCRIPTS_DIRECTORY_KEY);

        (useLocal ? localHttpRegistrator : osgiHttpRegistrator).setContextFilePath(contextFilePath);
    }

    public static void start() {
        HttpRegistrator registrator = osgiHttpRegistrator;
        if (useLocal) {
            localServer = new Server(port);
            localHttpRegistrator = new HttpRegistrator() {
                @Override
                public void registerHttpServletContext(ServletContextHandler ctx) {
                    localServer.setHandler(ctx);
                }

                @Override
                public void unregisterContext(ServletContextHandler ctx) {
                    try {
                        ctx.stop();
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Exception stopping servlet context", e);
                    }
                }
            };
            registrator = localHttpRegistrator;
        }

        try {
            start(registrator);
        }
        catch (Exception ex) {
            log.log(Level.SEVERE, "Exception creating context", ex);
        }

        if (useLocal) {
            try {
                localServer.start();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception starting http server", e);
            }
        }
    }

    public static void stop() {
        stop(useLocal ? localHttpRegistrator : osgiHttpRegistrator);
        if (useLocal) {
            try {
                localServer.stop();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception stopping http server", e);
            }
            httpContext = null;
        }
    }

    private static ServletContextHandler httpContext = null;

    private static void start(HttpRegistrator registrator) throws Exception{
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

        registrator.registerHttpServletContext(httpContext);
    }

    private static void startRestServletForDirectory(ServletContextHandler httpContext, File scriptsDirFile) {
        File[] scriptFiles = scriptsDirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith("groovy");
            }
        });

        if (scriptFiles != null) {
            RestSerlvet restServlet = new RestSerlvet();
            httpContext.addServlet(new ServletHolder(restServlet),"/" + scriptsDirFile.getName() + "/*");
            restServlet.loadHandlers(scriptFiles);
        }
    }

    private static void stop(HttpRegistrator registrator) {
        registrator.unregisterContext(httpContext);
    }

    private static RestMessageReceiver restComponent;

    public static void setService(RestMessageReceiver component) {
        restComponent = component;
    }

    public static Service getService() {
        return restComponent;
    }

}
