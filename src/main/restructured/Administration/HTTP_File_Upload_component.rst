HTTP File Upload component
==============================

Tigase’s HTTP File Upload component is an implementation of `XEP-0363 HTTP File Upload <http://xmpp.org/extensions/xep-0363.html:>`__ specification. This allows file transfer between XMPP clients by uploading a file to HTTP server and sending only link to download file to recipient.

This implementation makes use of the HTTP server used by Tigase XMPP Server and Tigase HTTP API component to provide web server for file upload and download.

By default this component is **disabled** and needs to be enabled in configuration file before it can be used. Another requirement is that the proper database schema needs to be applied to database which will be used by component.

Enabling HTTP File Upload Component
-----------------------------------------

**Configuration.**

.. code:: text

   upload() {}

Metadata repository
-------------------------

Running the component requires a repository where it can store information about allocated slots. For this, a metadata repository is used. It is possible to specify a specific implementation of ``FileUploadRepository`` for every domain.

By default, metadata for all domains will be stored in the ``default`` repository. Implementation of which will be selected based on kind of data source defined as ``default``.

DummyFileUploadRepository
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This is very simple repository which does not store any data. Due to that, it can be very fast! However, it is not able to remove old uploads and apply any upload limits.

JDBCFileUploadRepository
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
This repository implementation stores data in database used to store procedures and functions. By default, data should be stored in the ``tig_hfu_slots`` table but it can be changed by modification of stored procedures or reconfiguration of the repository implementation to use different stored procedures and functions than provided.

Storage
-------------

Component contains a pluggable storage mechanism, which means that it is relatively easy to implement custom storage provisions. By default ``DirectoryStore`` based storage is used.

Currently following storage providers are available out of the box.

DirectoryStore
^^^^^^^^^^^^^^^^^^^^^^

This storage mechanism places files in subdirectories with names that correspond to the id of `allocated slot <http://xmpp.org/extensions/xep-0363.html#intro:>`__. If required, it is possible to group all slot directories allocated by single user in a directory containing this user name.

By default there is no redundancy if this store is used in clustered environment. Every file will be stored on a single cluster node.

Available properties:

**path**
   Contains path to directory in which subdirectory with files will be created on the local machine. **(default: ``data/upload``)**

**group-by-user**
   Configures if slots directories should be grouped in user directories. **(default: false)**


Logic
----------

Logic is responsible for generation of URI and applying limits. It groups all configuration settings related to allocation of slots, etc.

Available properties:

**local-only**
   Allow only users with accounts on the local XMPP server to use this component for slot allocation. **(default: true)**

**max-file-size**
   Set maximum size of a single allocated slot (maximum file size) in bytes. **(default: 5000)**

**port**
   Specifies the port which should be used in generating the upload and download URI. If it is not set, then secured (HTTPS) server port will be used if available, and plain HTTP in other case. **(default: not set)**

**protocol**
   Protocol which should be used. **This is only used in conjunction with ``port``**. Possible values are:

   -  http

   -  https

**serverName**
   Server name to use as domain part in generated URI. **(default: server hostname)**

**upload-uri-format**
   Template used in generation of URI for file upload. **(default: ``{proto}://{serverName}:{port}/upload/{userJid}/{slotId}/{filename}``)**

**download-uri-format**
   Template used in generation of URI for file download. **(default: ``{proto}://{serverName}:{port}/upload/{slotId}/{filename}``)**

URI template format
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Every block in the template between ``{`` and ``}`` is a named part which will be replaced by the property value during generation of URI for slot.

Blocks possible to use:

**proto**
   Name of protocol.

**serverName**
   Domain name of server.

**port**
   Port on which HTTPS (or HTTP) server is listening.

**userJid**
   JID of user requesting slot allocation.

**domain**
   Domain of user requesting slot allocation.

**slotId**
   Generated ID of slot.

**filename**
   Name of file to upload.

.. Note::

   ``slotId`` and ``filename`` are required to be part of every URI template.

.. Warning::

    Inclusion of ``userJid`` or ``domain`` will speed up the lookup for slot id during upload and download operation if more than one metadata repository is configured. However, this may lead to leak of user JID or user domain if message with URI containing this part will be send to recipient which is unaware of the senders' JID (ie. in case of anonymous MUC room).


File upload expiration
----------------------------

From time to time it is required to remove expired file to make place for new uploads. This is done by the ``expiration`` task.

Available properties:

**expiration-time**
   How long the server will keep uploaded files. Value in `Java Period format <https://docs.oracle.com/javase/8/docs/api/java/time/Period.html#parse-java.lang.CharSequence-:>`__ **(default: P30D - 30 days)**

**period**
   How often the server should look for expired files to remove. Value in `Java Period format <https://docs.oracle.com/javase/8/docs/api/java/time/Period.html#parse-java.lang.CharSequence-:>`__ **(default: P1D - 1 day)**

**delay**
   Time since server start up before the server should look for expired files to remove. Value in `Java Period format <https://docs.oracle.com/javase/8/docs/api/java/time/Period.html#parse-java.lang.CharSequence-:>`__ **(default: 0)**

**limit**
   Maximum number of files to remove during a single execution of ``expiration``. **(default: 10000)**

Examples
--------------

Complex configuration example
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Configuration with a separate repository for metadata to ``example.com`` pointing to ``file_upload`` data source, custom upload and download URI, maximum file size set to 10MB, expiration done every 6 hours and grouping of slot folders by user jid.

**Complex configuration example.**

.. code:: text

   upload() {
       logic {
           local-only = false
           max-file-size = 10485760
           upload-uri-format = '{proto}://{serverName}:{port}/upload/{userJid}/{slotId}/{filename}'
           download-uri-format = '{proto}://{serverName}:{port}/upload/{domain}/{slotId}/{filename}'
       }

       expiration {
           period = P6H
       }

       repositoryPool {
           'example.com' () {
               data-source = "file_upload"
           }
       }

       store {
           group-by-user = true
       }
   }


Example configuration for clustering with HA
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Configuration for high availability in a cluster with common storage at ``/mnt/shared`` and both servers available as ``upload.example.com``

**Example configuration with HA.**

.. code:: text

   upload() {
       logic {
           upload-uri-format = '{proto}://upload.example.com:{port}/upload/{userJid}/{slotId}/{filename}'
           download-uri-format = '{proto}://upload.example.com:{port}/upload/{domain}/{slotId}/{filename}'
       }

       store {
           path = '/mnt/shared/upload'
       }
   }

.. include:: HTTP_File_Upload_Component-S3.inc