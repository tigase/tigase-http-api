package tigase.http.rest

/**
 * Created with IntelliJ IDEA.
 * User: andrzej
 * Date: 09.02.13
 * Time: 22:07
 * To change this template use File | Settings | File Templates.
 */
abstract class Handler {

    public static class Result {
        public String contentType;
        public byte[] data;
    }

    /**
     * true if requests require authentication
     */
    def authRequired = { return requiredRole != null }

    /**
     * false if handler requires plain content
     */
    def decodeContent = true;

    /**
     * regular expression to match request
     */
    def regex = null;

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

}
