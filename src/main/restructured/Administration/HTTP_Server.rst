HTTP Server
=============

Implementations
-------------------------------

Jetty HTTP Server *(default)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Jetty HTTP Server instance is provided as ``httpServer``. The server will only be active and enabled if either the HTTP API component or HTTP File Upload component is enabled. This project uses Jetty as default http server implementation.

Embedded HTTP server *(deprecated)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

HTTP server instance is provided as ``httpServer``. The server will only be active if manually enabled. Implementation of an http server is provided by `HttpServer <https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/package-summary.html>`__ found embedded in Java JDK.

.. Note::

   This implementation is good only for small installations of if there is **no requirement** for a high performance HTTP server. If this is do not match your requirements, it is recommended to use Jetty as the embedded HTTP server using `Tigase HTTP API - Jetty HTTP Server<jettyHttp>` project.

Configuration Properties
-------------------------------

The HTTP server can be configured using any of all of the following properties. Note that these settings only apply to the default implementation provided by Tigase HTTP API.

**ports**
   This property is used to configure on which ports on HTTP server should listen for incoming connections. If it is not set then default port ``8080`` will be used

**connections**
   It is used to group configurations passed to ports
   **{port}**
      For every ``{port}`` you can pass separate configuration. To do so you will need to replace ``{port}`` with port number, ie. ``8080``. For every port you can pass following properties:

      **socket**
         Sets type of socket used for handling incoming connections. Accepted values are:
         
         -  ``plain`` - port will work in plain HTTP mode **(default)**

         -  ``ssl`` - port will work in HTTPS mode

      **domain**
         This property is used to configure domain name of SSL certificate which should be used by HTTP server running on this port (if ``socket`` is set to ``ssl``). If it is not set (or it will be omitted) then Tigase XMPP Server will try to use SSL certificate for the host to which client tries to connect. If there will be no SSL certificate for that domain name, then default SSL certificate of Tigase XMPP Server will be used.

      **use-http2**
        **(only Jetty)** This property is used to disable Jetty support of HTTP/2. If ``socket`` is set to ``ssl`` HTTP/2 support will be enabled by default. To disable it set ``use-http2`` to ``false``.

Additional properties of embedded HTTP server *(deprecated)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

With embedded HTTP server, you have a few additional properties within ``executor`` section, which you can pass to adjust this HTTP server.

**executor**
   Name of the subsection

   **threads**
      This property is used to configure the number of threads used to handle HTTP requests, ie. ``10``

   **request-timeout**
      Property used to set timeout for processing a single HTTP request (in milliseconds), ie. ``30000``

   **accept-timeout**
      Property used to set timeout for reading HTTP request headers (in milliseconds), ie. ``2000``

Examples
-----------------

Below are few examples for use in DSL based configuration format and older properties based format.

HTTPS on port 8443 with SSL certificate for example.com
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In configuration file ``httpServer`` related configuration should look like this:

.. code:: groovy

   httpServer {
       connections {
           8443 () {
               socket = ssl
               domain = 'example.com'
           }
       }
   }


Changing port from 8080 to 8081
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: groovy

   httpServer {
       connections {
           8080 (active: false) {}
           8081 () {}
       }
   }

Redirections from HTTP to HTTPS
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It’s beneficial to use HTTPS as much as possible, however often it requires adding redirection from ``http`` to ``https``. While it’s possible to have it done using external solutions (additional http servers like nginx or apache or some sort of load balancer with such feature) it’s convenient to have it build-in.

Feature implemented in Tigase XMPP Server allows specifying ``redirectUri`` which consists of destination hostname and optionally port and path. Specifying any query parameters IS NOT supported. ``redirectUri`` has support for ``{host}`` variable which can be used to keep original server name in the redirection from the original request, ie. ``redirectUri = 'https://{host}:8089'`` to redirect request to the same server but on port 8089 (original path URI and query string will be automatically appended to the redirection URL).

It’s also possible, that Tigase XMPP server handles on it’s plain socket port regular ``http`` request as well as ``https`` handled by load balancer/proxy that terminates HTTPS traffic and forwards the request using ``http`` protocol. In that case unconditional request would result in infinite redirection. Fortunately it’s possible to specify condition under which redirection should happen using ``redirectCondition`` option. It has to be set for the redirection to wrok. Currently following values are supported (they should be self-explanatory):

-  ``never``,

-  ``http``,

-  ``https``,

-  ``always``

.. code:: groovy

   httpServer {
       connections {
           8080 () {
               redirectCondition = 'http'
               redirectUri = 'https://{host}:443'
           }
       }
   }

.. _jettyHttp:

Usage of Jetty HTTP server as HTTP server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As mentioned before Jetty as HTTP server is used by default. Jetty API can be used in one of two forms: Standalone and OSGi.

Standalone *(default)*
~~~~~~~~~~~~~~~

In this case the Jetty instance is created and configured internally by Tigase HTTP API. This allows for the same configuration properties used as for default HTTP server configuration.

**Configuration with use of standalone Jetty HTTP Server.**

.. code:: text

   httpServer (class: tigase.http.jetty.JettyStandaloneHttpServer) {
       ...
   }

HTTP/2 and Jetty HTTP Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When Jetty HTTP server is used in standalone mode *(default)* and HTTP server is configured to serve data over encrypted (``ssl`` or ``tls``) connections then HTTP/2 will be enabled by default.

However it is possible to disable HTTP/2 by setting ``use-http2`` property of encrypted port to ``false``, ie. for port 8443:

.. code:: text

   httpServer (class: tigase.http.jetty.JettyStandaloneHttpServer) {
       ...
       '8443' () {
           'socket' = ssl
           'use-http2' = false
       }
   }


OSGi
~~~~~~~~

This can only be used when Tigase is running inside OSGi container. If this is used Tigase HTTP API will try to retrieve Jetty HTTP server from OSGi container and use it.

.. Note::

   Jetty HTTP server instance is not configured by Tigase. We would only use this instance for deployment.

**Configuration in OSGi mode with use of Jetty HTTP Server.**

.. code:: text

   httpServer (class: tigase.http.jetty.JettyOSGiHttpServer) {
       ...
   }
