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

abstract class Handler {

    public static class Result {
        public String contentType;
        public byte[] data;
    }

	String pathName = null;
	def getSourceFile = { 
		return new File(pathName);
	}
	
    /**
     * true if requests require authentication
     */
    def authRequired = { api_key -> return requiredRole != null }

    /**
     * false if handler requires plain content
     */
    def decodeContent = true;

    /**
     * regular expression to match request
     */
    def regex = null;

	/**
	 * true if requests require valid API-KEY
	 */
	def apiKey = true;

    /**
     * name of role required to execute
     */
    String requiredRole = null;

    /**
     * true if requests will be handler in asynchroniously
     */
    def isAsync = false;

    // all functions below should return value directly by calling callback

    /**
     * Retrive item closure
     *
     * @param service instance
     * @param callback closure with results to return
     * @param user jid - passed only if authentication is required, if not then parameter is omitted
     * @param... additional parameters
     */
    def execGet = null;

    /**
     * Insert item closure
     *
     * @param service instance
     * @param callback closure with results to return
     * @param user jid - passed only if authentication is required, if not then parameter is omitted
     * @param request content
     * @param... additional parameters
     */
    def execPut = null;
    /**
     * Update item closure
     *
     * @param service instance
     * @param callback closure with results to return
     * @param user jid - passed only if authentication is required, if not then parameter is omitted
     * @param request content
     * @param... additional parameters
     */
    def execPost = null;
    /**
     * Remove item closure
     *
     * @param service instance
     * @param callback closure with results to return
     * @param user jid - passed only if authentication is required, if not then parameter is omitted
     * @param... additional parameters
     */
    def execDelete = null;

	def regexDescription = null;
	def description = null;
}
