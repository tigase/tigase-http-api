<p align="center">
  <a href="https://tigase.net/">
    <img
      alt="Tigase HTTP API"
      src="https://github.com/tigaseinc/website-assets/raw/master/tigase/images/tigase-logo.png?raw=true"
      width="300"
    />
  </a>
</p>

<p align="center">
  The HTTP API component for Tigase XMPP Server.
</p>

<p align="center">
  <img alt="Tigase Logo" src="https://github.com/tigaseinc/website-assets/raw/master/tigase/images/tigase-logo.png?raw=true" width="25"/>
  <img src="https://tc.tigase.net/app/rest/builds/buildType:(id:TigaseHttpApi_Build)/statusIcon" width="100"/>
</p>

# What it is

Tigase HTTP API is XMPP component and HTTP server for Tigase XMPP Server providing easy to use HTTP endpoints for server management and integration with other services.

# Features

Tigase HTTP API provides following modules and functionalities:
* AdminUI - for managing Tigase XMPP Server using a web browser
* DNS Web Service - provides support for [XEP-0156: Discovering Alternative XMPP Connection Methods
](https://xmpp.org/extensions/xep-0156.html)
* REST API - for integration with other services using REST API
* Server Info - presenting server detail
* Setup - for inital configuration and installation of Tigase XMPP Server
* UI - Tigase XMPP web-based client

# Support

When looking for support, please first search for answers to your question in the available online channels:

* Our online documentation: [Tigase Docs](https://docs.tigase.net)
* Our online forums: [Tigase Forums](https://help.tigase.net/portal/community)
* Our online Knowledge Base [Tigase KB](https://help.tigase.net/portal/kb)

If you didn't find an answer in the resources above, feel free to submit your question to either our 
[community portal](https://help.tigase.net/portal/community) or open a [support ticket](https://help.tigase.net/portal/newticket).

# Downloads

You can download distribution version of Tigase XMPP Server which contains Tigase HTTP API directly from [here](https://github.com/tigaseinc/tigase-server/releases).

If you wish to downloand SNAPSHOT build of the development version of Tigase XMPP Server which contains Tigase HTTP API you can grab it from [here](https://build.tigase.net/nightlies/dists/latest/tigase-server-dist-max.zip).

# Installation and usage

Documentation of the project is part of the Tigase XMPP Server distribution package and it is also available as part of [Tigase XMPP Server documnetation page](https://docs.tigase.net/).

# Compilation 

Compilation of the project is very easy as it is typical Maven project. All you need to do is to execute
````bash
mvn package test
````
to compile the project and run unit tests.

# License

<img alt="Tigase Tigase Logo" src="https://github.com/tigase/website-assets/blob/master/tigase/images/tigase-logo.png?raw=true" width="25"/> Official <a href="https://tigase.net/">Tigase</a> repository is available at: https://github.com/tigase/tigase-http-api/.

Copyright (c) 2004 Tigase, Inc.

Licensed under AGPL License Version 3. Other licensing options available upon request.
