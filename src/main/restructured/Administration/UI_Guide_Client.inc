Tigase Web Client
-------------------

Tigase now has a fully featured XMPP client built right into the HTTP interface. Everything you would expect from an XMPP client can now be done from the comfort of your browser window with no software install required!

The web client is active and available by default on servers v7.2.0 and later.

To access the client, point a browser to the following address: xmpp.your-server.net:8080/ui/

It will ask you for a login, any bare JID of users registered with the server will work. **NOTE: Use your bare JID for login**

Once you have logged in successfully, you will be presented with the following screen.

|WebUI Home|

The commands are broken into categories shown here. All changes made in these sections are instant and should be seen the same as if you were using an external XMPP client like Psi.

**NOTE** The BOSH client will automatically translate all requests to the server name. In some rare cases this may not be resolvable by the browser and you will be unable to login. Should that happen, you may disable that feature using the following line in your config.tdsl:

.. code:: text

   bosh {
       'send-node-hostname' = false
   }

You may have to specifically designate the bosh URL when using the advanced tag in the login screen.

Chat
^^^^^^^^^

This is the default window, and your main interface for chatting inside XMPP with this server. **NOTE: you can only communicate to users logged onto the current server, or connected clusters** Users from your roster will be on the left panel, the right all active discussions and MUCs, as well as the currently selected chat will be available.

|WebUI Chat|

Users that are logged in and on your roster will be displayed on the left side. Double-clicking will bring up a new chat window with the user. You can Right-click on them to bring up a sub menu with the following;

|WebUI Usersubmenu|

-  **Chat** replicates a double-click and opens a new window for chat.

-  **Modify** brings up a dialogue that allows you to change the JID of the contact, a nickname, and group.

-  **Delete** removes the user from your roster. This will also remove subscription authorization for the selected user to receive presence information effectively removing you from their roster. **NOTE: this will not block user packets from your JID**

-  **Info** brings up the User Info (this is the disco#info command for the selected user)

The top right section has a few icons with specific functionality, they are;

|WebUI Chat Add New| adds a new user to your roster.

|WebUI Chat NewMUC| creates a new Multi-user chatroom.

|WebUI Chat Profile| allows you to edit your user information such as picture and nickname.

|WebUI Chat CloseWindow| closes the active chat window.

|WebUI Chat settings| provides a place to change your password or publish changes to your user info. **NOTE: you are limited to changing the General fields**


Discovery
^^^^^^^^^^^^^

This is your service discovery panel, which breaks down by component in the sidebar. Each component name and its associated JID is listed to help you find what you need. Most components give you an option to Execute commands with a few exceptions allowing browsing and the ability to join a MUC.

**Browse** allows you to dig deeper into certain components; for example list the chatrooms available in the MUC component. At the top of the page the specific JID of the component are you in will be displayed. This is a text field, and can be edited to reflect the JID of the component (or just the server name) to navigate.

|WebUI Browse Comp|

**Join to Room** will join you to a MUC room that is selected. Alternatively, selecting Join to Room while MUC component is selected, you can join and start a new MUC room.

**Execute Command** Provides a hierarchy of commands and options to view and edit settings, run commands and scripts, view contents of files, and see statistics. Since each Component can have a unique structure it is best to explore each to see what options are available.

Management
^^^^^^^^^^^^^^

This is an advanced window for settings and management for the XMPP server.

Configuration
~~~~~~~~~~~~~~

Here you can manage some of the server settings.

Notifications
~~~~~~~~~~~~~~

This section has one simple command: to be able to send a mass message to all logged in users. You may choose to change the type of message to headline or Normal which will show as a pop-up in most XMPP clients. Chat messages will open up a chat dialog with users.

Other
~~~~~

This section contains a considerable list of options and settings affecting server functions.

Activate log tracker for a user
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This allows you to set a log file to track a specific user. Set the bare or full JID of the user you want to log, and a name of the files you wish the log to be written to. The files will be written in the root Tigase directory unless you give a directory like logs/filename. The log files will be named with a .0 extension and will be named .1, .2, .3 and so on as each file reaches 10MB by default. filename.0 will always be the most recent. Logging will cease once the server restarts.

Add SSL certificate
~~~~~~~~~~~~~~~~~~~~

Here you can add SSL certificates from PEM files to specific virtual hosts. Although Tigase can generate its own self-signed certificates, this will override those default certificates.

Add Monitor Task
~~~~~~~~~~~~~~~~~

You can write scripts for Groovy or ECMAScript to add to monitor tasks here. This only adds the script to available scripts however, you will need to run it from another prompt.

Add Monitor Timer Task
~~~~~~~~~~~~~~~~~~~~~~~

This section allows you to add monitor scripts in Groovy while using a delay setting which will delay the start of the script.

Add New Item - ext
~~~~~~~~~~~~~~~~~~~

Provides a method to add external components to the server. By default you are considered the owner, and the Tigase load balancer is automatically filled in.

Add New Item - Vhost
~~~~~~~~~~~~~~~~~~~~~

This allows you to add new virtual hosts to the XMPP server. A breakdown of the fields is as follows:

-  Domain name: the full domain name of the new vhost. Tigase will not add anything to this domain, so if it is ti be a subdomain of example.com, you will need to enter sub.domain.com.

-  Enabled: Whether the domain is turned on or off.

-  Anonymous enabled: Allow anonymous logins.

-  In-band registration: Whether or not to allow users to register accounts upon login.

-  TLS required: Require logins to the vhost to conduct a TLS handshake before opening streams.

-  S2S secret: a server-generated code to differentiate traffic between servers, typically there is no need to enter your own, but you may if you need to get into low level code.

-  Domain filter policy: Sets the filter policy for this domain, see `This section <#domainBasedPacketFiltering>`__ for a description of the rules.

-  Domain filter domains: a specific setting to restrict or control cross domain traffic.

-  Max users: maximum users allowed to be registered to the server.

-  Allowed C2S, BOSH, Websocket ports: Comma separated list of ports that this vhost will check for all of these services.

-  Presence forward address: specific address where presence information is forwarded too. This may be handy if you are looking to use a single domain for presence processing and handling.

-  Message forward address: Specific address where all messages will be sent too. This may be useful to you if you have a single server handling AMP or message storage and want to keep the load there.

-  Other Parameters: Other settings you may wish to pass to the server, consider this a section for options after a command.

-  Owner: The owner of the vhost who will also be considered an administrator.

-  Administrators: comma separated list of JIDs who will be considered admins for the vhost.

-  XEP-0136 Message Archiving Enabled: Whether to turn on or off this feature.

-  XEP-0136 Required store method: If XEP-0136 is turned on, you may restrict the portion of message that is saved. This is required for any archiving, if null, any portion of the message may be stored.

-  Client certificate required: Whether the client should submit a certificate to login.

-  Client certificate CA: The Certificate Authority of the client certificate.

-  XEP-0136 retention period: integer of number of days message archives will be set.

-  Trusted JIDs: Comma separated list of JIDs who will be added to the trusted list, these are JIDS that may conduct commands, edit settings, or other secure work without needed secure logins.

-  XEP-0136 retention type: Sets the type of data that retention period will use. May be User defined (custom number type), Unlimited, or Number of Days.

-  XEP-0136 - store MUC messages: Whether or not to store MUC messages for archiving. Default is user, which allows users to individually set this setting, otherwise tue/false will override.

-  see-other-host redirection enabled: in servers that have multiple clusters, this feature will help to automatically repopulate the cluster list if one goes down, however if this is unchecked, that list will not change and may attempt to send traffic to a down server.

-  XEP-0136 Default store method: The default section of messages that will be stored in the archive.

Change user inter-domain communication permission
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can restrict users to only be able to send and receive packets to and from certain virtual hosts. This may be helpful if you want to lock users to a specific domain, or prevent them from getting information from a statistics component.


Connections Time
~~~~~~~~~~~~~~~~~

Lists the longest and average connection time from clients to servers.

DNS Query
~~~~~~~~~~

A basic DNS Query form.

Default config - Pubsub
~~~~~~~~~~~~~~~~~~~~~~~~

This section enables you to change the default pubsub node configuration for all future nodes. **Note: these changes will be reset on server restart.** - pubsub#node type: sets the type of node the the new node will be. Options include:

-  **leaf** Node that can publish and be published too.

-  **collection** A collection of other nodes.

   -  A friendly name for the node: Allows spaces and other characters to help differentiate it from other nodes.

   -  Whether to deliver payloads with event notifications: as it says, to publish events or not.

   -  Notify subscribers when the configuration changes: default is false

   -  Persist items to storage: whether or not to physically store items in the node.

   -  Max # of items to persist: Limit how many items are kept in the node archive.

   -  The collection with which the node is affiliated: If the node is to be in a collection, place that node name here.

   -  Specify the subscriber model: Choose what type of subscriber model will be used for this node. Options include:

-  **authorize** - Requires all subscriptions to be approved by the node owner before items will be published to the user. Also only subscribers may retrieve items.

-  **open** - All users may subscribe and retrieve items from the node.

-  **presence** - Typically used in an instant message environment. Provides a system under which users who are subscribed to the owner JID’s presence with a from or both subscription may subscribe from and retrieve items from the node.

-  **roster** - This is also used in an instant message environments, Users who are both subscribed to the owners presence AND is placed in specific allowed groups by the roster are able to subscribe to the node and retrieve items from it.

-  **whitelist** - Only explicitly allowed JIDs are allowed to subscribe and retrieve items from the node, this list is set by the owner/administrator.

   -  Specify the Publisher model: Choose what type of publisher model will be used for this node. Options include:

-  **open** - Any user may publish to this node.

-  **publishers** - Only users listed as publishers may be able to publish.

-  **subscribers** - Only subscribers may publish to this node.

   -  When to send the last published item: This allows you to decide if and when the last published item to the node may be sent to newly subscribed users.

-  **never** - Do not send the last published item.

-  **on_sub** - Send the last published item when a user subscribes to the node.

-  **on_sub_and_presence** - Send the last published item to the user after a subscription is made, and the user is available.

   -  The domains allowed to access this node: Comma separated list of domains for which users can access this node. If left blank there is no domain restriction.

   -  Whether to deliver items to available users only: Items will only be published to users with available status if this is selected.

   -  Whether to subscription expired when subscriber going offline: This will make all subscriptions to the node valid for a single session and will need to be re-subscribed upon reconnect.

   -  The XSL transformation which can be applied to payloads in order to generate an appropriate message body element: Since you want a properly formatted <body> element, you can add an XSL transformation here to address any payloads or extra elements to be properly formatted here.

   -  The URL of the XSL transformation which can be applied to payloads in order to generate an appropriate message body element: This would be the URL of the XSL Transform, e.g. http://www.w3.org/1999/XSL/Transform.

   -  Roster groups allowed to subscribe: a list of groups for whom users will be able to subscribe. If this is blank, no user restriction will be imposed.

   -  Notify subscribers when owner changes their subscription or affiliation state: This will have the node send a message in the case of an owner changing affiliation or subscription state.

   -  Allows get list of subscribers for each subscriber: Allows subscribers to produce a list of other subscribers to the node.

   -  Whether to sort collection items by creation date or update time: options include

-  **byCreationDate** - Items will be sorted by the creation date, i.e. when the item was made.

-  **byUpdateTime** - Items will be sorted by the last update time, i.e. when the item was last edited/published/etc..

Default room config
~~~~~~~~~~~~~~~~~~~~~

Allows you to set the default configuration for new MUC rooms. This will not be able to modify current in use and persistent rooms.


Delete Monitor Task
~~~~~~~~~~~~~~~~~~~~

This removes a monitor task from the list of available monitor scripts. This action is not permanent as it will revert to initial settings on server restart.


Fix User’s Roster
~~~~~~~~~~~~~~~~~

You can fix a users roster from this prompt. Fill out the bare JID of the user and the names you wish to add or remove from the roster. You can edit a users roster using this tool, and changes are permanent.

Fix User’s Roster on Tigase Cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This does the same as the Fix User’s Roster, but can apply to users in clustered servers.

Get User Roster
~~~~~~~~~~~~~~~

As the title implies this gets a users' roster and displays it on screen. You can use a bare or full JID to get specific rosters.

Get any file
~~~~~~~~~~~~

Enables you to see the contents of any file in the tigase directory. By default you are in the root directory, if you wish to go into directory use the following format: logs/tigase.log.0

Get Configuration File
~~~~~~~~~~~~~~~~~~~~~~~~

If you don’t want to type in the location of a configuration file, you can use this prompt to bring up the contents of either tigase.conf or config.tdsl.


Get config.tdsl File
~~~~~~~~~~~~~~~~~~~~~~~~

Will output the current config.tdsl file, this includes any modifications made during the current server session.


Load Errors
~~~~~~~~~~~~~~~

Will display any errors the server encounters in loading and running. Can be useful if you need to address any issues.


New command script - Monitor
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Allows you to write command scripts in Groovy and store them physically so they can be saved past server restart and run at any time. Scripts written here will only be able to work on the Monitor component.

New command script - MUC
~~~~~~~~~~~~~~~~~~~~~~~~

Allows you to write command scripts in Groovy and store them physically so they can be saved past server restart and run at any time. Scripts written here will only be able to work on the MUC component.


OAUth credentials
~~~~~~~~~~~~~~~~~~~

Uses OAuth to set new credentials and enable or disable a registration requirement with a signed form.


Pre-Bind BOSH user session
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Allows admins to pre-bind a BOSH session with a full or bare JID (with the resource automatically populated on connection). You may also specify HOLD or WAIT parameters.

Reload component repository
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This will show if you have any external components and will reload them in case of any stuck threads.

Scripts
~~~~~~~~

This section provides a list of command scripts for all active components. Each component has the following options - **New command script** provides a method to author new command scripts for specific components written in EMCAScript or Groovy. You do have an option to save the script to disk which will make the script permanent within the server. - **Remove command script** allows you to remove the selected script from the repository. If Remove from disk is not checked, the script will be unavailable until server restart. If it is, it will be permanently removed from the server.

Newly made commands will be listed under the Group listing in the left column.

Statistics
~~~~~~~~~~~

These statistics might be more useful as script results yield small bits of data, but you may find them useful when looking for server loads or finding user issues.

Get User Statistics
~~~~~~~~~~~~~~~~~~~

Provides a script output of user statistics including how many active sessions are in use, number of packets used, specific connections and their packet usage and location. All resources will return individual stats along with IP addresses.

Get Active User List
~~~~~~~~~~~~~~~~~~~~

Provides a list of active users under the selected domain within the server. An active user is considered a user currently logged into the XMPP server.


Get list of idle users
~~~~~~~~~~~~~~~~~~~~~~

This will list all idle users separated by vhost.


Get list of online users
~~~~~~~~~~~~~~~~~~~~~~~~

This will list users separated by the vhost they are connected to. The list will include the bare JID as well as any resources for that JID.

Get number of active users
~~~~~~~~~~~~~~~~~~~~~~~~~~

This displays the number of current active users.

Get number of idle users
~~~~~~~~~~~~~~~~~~~~~~~~

This section returns the number of active users per specific vhost.

Get top active users

This will list the top number of active users by packets sent and online time. This list will only be built with users currently online and from all vhosts.


Users
~~~~~

Add New User
~~~~~~~~~~~~~~~

Here you can add new users to any domain handled by vHosts, users are added to database immediately and are able to login. **NOTE: You cannot bestow admin status to these users in this section.**

Change user password
'''''''''''''''''''''

Allows for admins to change the password of a specific user without needing to know the original password for the selected bare JID. Users currently logged in will not know password has been changed until they attempt to re-login.

Delete user
~~~~~~~~~~~~~~~

Provides a text window for admins to input the bare JID of the user they wish to remove from the server.

Get User Info
~~~~~~~~~~~~~~~

This section allows admins to get information about a specific user including current connections as well as offline and online messages awaiting delivery.

Get registered user list
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Provides a list of vhosts to search and a maximum number of users to list. Once run, the script will display a list of registered bare JIDs of users from the selected vhost.

Modify User
~~~~~~~~~~~~~~~

Allows you to modify some user details including E-mail and whether it is an active user.

.. |WebUI Home| image:: /images/WebUI-Home.jpg
.. |WebUI Chat| image:: /images/WebUI-Chat.jpg
.. |WebUI Usersubmenu| image:: /images/WebUI-Usersubmenu.jpg
.. |WebUI Chat Add New| image:: /images/WebUI-Chat-Add-New.jpg
.. |WebUI Chat NewMUC| image:: /images/WebUI-Chat-NewMUC.jpg
.. |WebUI Chat Profile| image:: /images/WebUI-Chat-Profile.jpg
.. |WebUI Chat CloseWindow| image:: /images/WebUI-Chat-CloseWindow.jpg
.. |WebUI Chat settings| image:: /images/WebUI-Chat-settings.jpg
.. |WebUI Browse Comp| image:: /images/WebUI-Browse-Comp.jpg

