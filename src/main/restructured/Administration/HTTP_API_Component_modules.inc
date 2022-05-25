
Available modules
--------------------

Admin UI module
^^^^^^^^^^^^^^^^^^^

This is very simple module for administration of Tigase XMPP Server using HTTP browser. It allows administrators to execute ad-hoc commands from HTTP browser allowing to change some configuration options at runtime. It can be accessed by pointing your browser to http://server.address:8080/admin/ and logging in with admin credentials.

Index module
^^^^^^^^^^^^^^^^^^^

This module is deployed at ``/`` by default and provides list of installed and available modules for the virtual host when requested.

.. _Rest_module:

REST module
^^^^^^^^^^^^^^^^^^^

This module provides REST-like API for accessing Tigase XMPP Server. It uses Groovy scripts to process HTTP requests and prepare responses.

Server status module
^^^^^^^^^^^^^^^^^^^^^

.. Warning::

    This module is still a work in progress!

This module is designed to present current server status and report possible issues.

Setup module
^^^^^^^^^^^^^^^^^^^^^

Module is created to act as a web based installer and configuration utility for Tigase XMPP Server. Allows you to modify basic Tigase XMPP Server settings, ie. related to database access. Changes may be saved to configuration file from this module.

Web UI module
^^^^^^^^^^^^^^^^^^^^^

This module contains full web client based on `Tigase JaXMPP <http://www.tigase.net/content/jaxmpp-library:>`__ client library allowing user to chat, manage contacts list (roster), browse message archive, etc. For more information on this module, consult the `Administration Guide <http://docs.tigase.org/tigase-server/snapshot/Administration_Guide/html/#_use_of_the_http_api>`__.

DNS Web Service module
^^^^^^^^^^^^^^^^^^^^^^^^^

For web based XMPP clients it is not possible to execute DNS SRV requests to find address of XMPP server hosting for particular domain. To solve this the DNS Web Service module was created.

It handles incoming HTTP GET request and using ``Host`` HTTP header executes DNS requests as specified in `XEP-0156: Discovering Alternative XMPP Connection Methods <https://xmpp.org/extensions/xep-0156.html>`__. Results are returned in XML or JSON as specified in the XEP mentioned before.

By default it is deployed at ``dns-webservice`` and path for XML response is ``/dns-webservice/.well-known/host-meta`` and for JSON is ``/dns-webservice/-known/host-meta.json``.

User Status Endpoint module
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This module is designed as an endpoint required for REST API User Status to work properly. It is not accessible using HTTP/REST API, so it can (and in most cases should) be active.

