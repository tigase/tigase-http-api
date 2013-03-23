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
package tigase.http.rest

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceListener
import org.osgi.framework.ServiceReference
import org.osgi.framework.ServiceRegistration
import tigase.http.HttpRegistrator
import tigase.http.HttpServer
import tigase.osgi.ModulesManager

import java.util.logging.Logger

class Activator implements BundleActivator, ServiceListener {

    private static final Logger log = Logger.getLogger(Activator.class.getCanonicalName())

    //private ServletContextHandler httpContext;
    private ServiceRegistration registration;

    private BundleContext context;
    private ModulesManager serviceManager;
    private ServiceReference serviceReference;

    private HttpRegistrator registrator;

    @Override
    void start(BundleContext context) throws Exception {
        // we are inside OSGi container
        HttpServer.setOSGi(true)

        ServiceReference sRef = context.getServiceReference(Server.class.getName());
        if (sRef != null) {
            log.info("starting Tigase HTTP Rest API")
            registrator = new HttpRegistrator() {
                @Override
                void registerHttpServletContext(ServletContextHandler ctx) {
                    def props = new Hashtable()
                    props.put("contextFilePath", "/tigase-http-context.xml");
                    registration = context.registerService(ContextHandler.class.getName(), ctx, props);
                }

                @Override
                void unregisterContext(ServletContextHandler ctx) {
                    ctx.stop()
                    registration.unregister();
                    registration = null;
                }
            };

            HttpServer.setOsgiHttpRegistrator(registrator);
            log.info("started Tigase HTTP Rest API")
        }

        // Add ApiMessaceReceiver
        this.context = context;
        context.addServiceListener(this, "(&(objectClass=" + ModulesManager.class.getName() + "))");
        serviceReference = context.getServiceReference(ModulesManager.class.getName());
        if (serviceReference != null) {
            serviceManager = (ModulesManager) context.getService(serviceReference);
            registerAddons();
        }
    }

    @Override
    void stop(BundleContext context) throws Exception {

        if (serviceManager != null) {
            // unregister component and release Jetty HTTP Server
            unregisterAddons();
            context.ungetService(serviceReference);
            serviceManager = null;
            serviceReference = null;
        }

        // we are stopped so clear this flag
        HttpServer.setOSGi(false)
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        // handle case when Jetty is started or stopped during lifetime of started Tigase HTTP API component
        if (event.getType() == ServiceEvent.REGISTERED) {
            if (serviceReference == null) {
                serviceReference = event.getServiceReference();
                serviceManager = (ModulesManager) context.getService(serviceReference);
                registerAddons();
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING) {
            if (serviceReference == event.getServiceReference()) {
                unregisterAddons();
                context.ungetService(serviceReference);
                serviceManager = null;
                serviceReference = null;
            }
        }
    }

    /**
     * Register ServerComponents
     */
    private void registerAddons() {
        if (serviceManager != null) {
            serviceManager.registerServerComponentClass(RestMessageReceiver.class);
            serviceManager.update();
        }
    }

    /**
     * Unregister ServerComponents
     */
    private void unregisterAddons() {
        if (serviceManager != null) {
            serviceManager.unregisterServerComponentClass(RestMessageReceiver.class);
            serviceManager.update();
        }
    }

}
