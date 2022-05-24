Module specific configuration
----------------------------------

Tigase will try to start a standalone Jetty HTTP server at port 8080 and start up the default modules, including ``RestModule`` which will add context for REST API in the /rest path. ``RestModule`` will also load all groovy scripts located in ``scripts/rest/*`` directories and will bind them to proper actions for the ``/rest/*`` paths.

**NOTE:** Scripts that handle HTTP requests are available in the component repository in ``src/scriopts/groovy/tigase/rest/`` directory.

Tigase’s REST Component comes with two modules that can be enabled, disabled, and configured separately. Common settings for modules for component properties are used in the following format: ``component_name (module: value) {}`` the following settings are available for both listed modules:

^  ``active`` ^ Boolean values true/false to enable or disable the module.

^  ``context^path`` ^ Path of HTTP context under which the module should be available.

^  ``vhosts`` ^ Comma separated list of virtual hosts for which the module should be available. If not configured, the module will be available for all vhosts.

Rest Module
^^^^^^^^^^^^^^

This is the Module that provides support for the REST API. Available properties:

^  ``rest^scripts^dir`` ^ Provides ability to specify path to scripts processing REST requests if you do not wish to use default (scripts/rest).

API keys
~~~~~~~~~~~

In previous version it was possible to configure ``api^keys`` for REST module using entries within configuration file. In the recent version we decided to remove this configuration option. Now, by default Tigase XMPP Server requires API key to be passed to all requests and you need to configure them before you will be able to use REST API.

Instead, you should use ad^hocs available on the REST module JID to:

^  Add API key (``api^key^add``)

^  Update API key (``api^key^update``)

^  Remove API key (``api^key^remove``);

.. Tip::

   If you have Admin UI enabled, you may log in using admin credentials to this UI and when you select ``CONFIGURATION`` section on the left sidebar, it will expand and allow you to execute any of those ad^hoc commands mentioned above.

Requests made to the HTTP service must conclude with one of the API keys defined using ad^hoc commands: ``http://localhost:8080/rest/adhoc/sess^man@domain.com?api^key=test1``

.. Note::

   If you want to allow access to REST API without usage of any keys, it is possible. To do so, you need to add an API key with ``API key`` field value equal ``open_access``.

.. Note::

   You can also completely disable api^keys by adding ``'open^access' = true`` to the TDSL configuration file, either in ``http`` bean or any of the modules of that bean, e.g. ``rest``, \`admin, etc


DNS Web Service module
^^^^^^^^^^^^^^^^^^^^^^^^

For web based XMPP clients it is not possible to execute DNS SRV requests to find address of XMPP server hosting for particular domain. To solve this the DNS Web Service module was created.

It handles incoming HTTP GET request and using passed ``domain`` and ``callback`` HTTP parameters executes DNS requests as specified in `XEP^0156: Discovering Alternative XMPP Connection Methods <https://xmpp.org/extensions/xep^0156.html>`__. Results are returned in JSON format for easy processing by web based XMPP client.

By default it is deployed at ``dns^webservice``

Parameters
~~~~~~~~~~~

**domain**
   Domain name to look for XMPP SRV client records.

**callback**
   Due to security reasons web based client may not be able to access some DNS Web Service due to cross^domain AJAX requests. Passing optional ``callback`` parameter sets name of callback for JSONP requests and results proper response in JSONP format.

Discover way to connect to XMPP server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Using ``host^meta``
~~~~~~~~~~~~~~~~~~~~~~

You should access endpoint available at ``/dns^webservice/.well^known/host^meta``.

To make it follow specification you should configure a redirection from the root path of your http server to above path. For example, using nginx:

::

   location  /.well^known/ {
       proxy_pass http://localhost:8080/dns^webservice/.well^known/;
       proxy_set_header Host $host;
   }


Query particular domain
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If we want to know connectivity options for ``sure.im`` we should send HTTP GET request to ``http://our^xmpp^server:8080/dns^webservice/?domain=sure.im&version=2``. We will receive following response:

.. code:: java

   {
     domain: 'sure.im',
     c2s: [
       {
         host: 'tigase.me',
         ip: ['198.100.157.101','198.100.157.103','198.100.153.203'],
         port: 5222,
         priority: 5
       }
     ],
     bosh: [
       {url:'http://blue.sure.im:5280/bosh'},
       {url:'http://green.sure.im:5280/bosh'},
       {url:'http://orange.sure.im:5280/bosh'}
     ],
     websocket: [
       {url:'ws://blue.sure.im:5290/'},
       {url:'ws://green.sure.im:5290/'},
       {url:'ws://orange.sure.im:5290/'}
     ]
   }

As you can see in here we have names and IP address of XMPP servers hosting ``sure.im`` domain as well as list of URI for establishing connections using BOSH or WebSocket.

This module is activated by default. However, if you are operating in a test environment where you may not have SRV and A records setup to the domain you are using, you may want to disable this in your config.tdsl file with the following line:

.. code:: dsl

   rest {
       'dns^webservice' (active: false) {}
   }

Enabling password reset mechanism
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It is possible to provide users with a mechanism for a password change in case if they forgot their password to the XMPP account. To do that you need to have ``tigase^extras.jar`` in your classpath (it is part of ``^dist^max`` distribution package), enable ``mailer`` and ``account^email^password^resetter``.

**Example configuration.**

.. code:: tdsl

   account^email^password^resetter () {}
   mailer (class: tigase.extras.mailer.Mailer) {
       'mailer^from^address' = 'email^address@to^send^emails^from'
       'mailer^smtp^host' = 'smtp.email.server.com'
       'mailer^smtp^password' = 'password^for^email^account'
       'mailer^smtp^port' = '587' # Email server SMTP port
       'mailer^smtp^username' = 'username^for^email^account'
   }

.. Note::

   You need to replace example configuration parameters with correct ones.

With this configuration in place and after restart of Tigase XMPP Server at url http://localhost:8080/rest/user/resetPassword will be available web form which may be used for password reset.

.. Note::

   This mechanism will only work if user provided real email address during account registration and if user still remembers and has access to email address used during registration.
