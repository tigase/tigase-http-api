/**
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
package tigase.http;

import java.util.logging.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import tigase.osgi.ModulesManager;

public class Activator implements BundleActivator, ServiceListener {
	
    private static final Logger log = Logger.getLogger(Activator.class.getCanonicalName());

    private static BundleContext context;
    private ModulesManager serviceManager;
    private ServiceReference serviceReference;

	public static BundleContext getContext() {
		return context;
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		// Add ApiMessaceReceiver
		Activator.context = context;
        context.addServiceListener(this, "(&(objectClass=" + ModulesManager.class.getName() + "))");
        serviceReference = context.getServiceReference(ModulesManager.class.getName());
        if (serviceReference != null) {
            serviceManager = (ModulesManager) context.getService(serviceReference);
            registerAddons();
        }
	}

	@Override
	public void stop(BundleContext bc) throws Exception {
        if (serviceManager != null) {
            // unregister component and release Jetty HTTP Server
            unregisterAddons();
            context.ungetService(serviceReference);
            serviceManager = null;
            serviceReference = null;
        }	
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
            serviceManager.registerServerComponentClass(HttpMessageReceiver.class);
            serviceManager.update();
        }
    }

    /**
     * Unregister ServerComponents
     */
    private void unregisterAddons() {
        if (serviceManager != null) {
            serviceManager.unregisterServerComponentClass(HttpMessageReceiver.class);
            serviceManager.update();
        }
    }
	
}
