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
package tigase.http.modules.rest

import tigase.http.rest.Handler

import java.util.logging.Level
import java.util.logging.Logger

public class HandlersLoader {

    private static final Logger log = Logger.getLogger(HandlersLoader.class.getCanonicalName());
    //private static final def groovyClassLoader = new GroovyClassLoader();

    private static HandlersLoader instance;

    public static synchronized HandlersLoader getInstance() {
        if (instance == null) instance = new HandlersLoader();
        return instance;
    }

    public def loadHandler(GroovyClassLoader classLoader, File file) {
        Class cls = classLoader.parseClass(file);
        Object scriptInstance = cls.newInstance()
        Handler handler = (Handler) scriptInstance;
		handler.pathName = file.getAbsolutePath();
		return handler;
    }

    public def loadHandlers(List<File> scripts) {
        def classLoader = new GroovyClassLoader(this.getClass().getClassLoader());
        def newHandlers = [];

        scripts.each { file ->
            try {
                log.info("loading handler from file = " + file.getCanonicalPath())
                newHandlers.add(loadHandler(classLoader, file))
                log.info("handler loaded");
            }
            catch (Throwable ex) {
                log.log(Level.SEVERE, "Exception loading handler from script = " + file.getAbsolutePath(), ex);
            }
        }

        return newHandlers;
    }

    public def loadHandlersFromDirectory(File dir) {
        log.info("loading handlers from scripts from = " + dir.getCanonicalPath())
        def scriptFiles = dir.listFiles().findAll { it.getName().endsWith(".groovy") }
        def handlers = loadHandlers(scriptFiles);
        return handlers;
    }

}
