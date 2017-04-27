/*
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
 *
 */

package tigase.http.modules.setup;

import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.Kernel;
import tigase.server.ServerComponent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 30.03.2017.
 */
public class SetupHelper {

	public static List<BeanDefinition> getAvailableComponents() {
		return getAvailableBeans(ServerComponent.class);
	}

	public static List<BeanDefinition> getAvailableProcessors(Class componentClazz, Class processorClazz) {
		return getAvailableBeans(processorClazz, componentClazz);
	}

	public static List<BeanDefinition> getAvailableBeans(Class processorClazz) {
		return getAvailableBeans(processorClazz, Kernel.class);
	}

	public static List<BeanDefinition> getAvailableBeans(Class processorClazz, Class componentClazz) {
		Kernel kernel = new Kernel();
		kernel.registerBean("beanSelector").asInstance(new ServerBeanSelector()).exportable().exec();
		return AbstractBeanConfigurator.getBeanClassesFromAnnotations(kernel, componentClazz)
				.entrySet()
				.stream()
				.filter(e -> processorClazz.isAssignableFrom(e.getValue()))
				.map(e -> e.getValue())
				.map(SetupHelper::convertToBeanDefinition)
				.collect(Collectors.toList());
	}

	public static BeanDefinition convertToBeanDefinition(Class<?> cls) {
		return new BeanDefinition(cls);
	}



}
