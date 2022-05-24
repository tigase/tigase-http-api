Common module configuration
-------------------------------

Enabling/disabling module
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Every module can be activated or disabled by adjusting it’s activity in following way:

.. code:: DSL

   http {
       %module_id% (active: false) {}
   }

.. Note::

   You need to replace ``%module_id%`` with the id of module which you want to change activity (in this case, it will disable module).

**Disabling REST module.**

.. code:: DSL

   http {
       rest (active: false) {}
   }

Context path
^^^^^^^^^^^^^^^^^^^

This property allows you to change the context path that is used by module. In other words, it allows you to change the prefix used by module. By default every module (with exception of the Index module) uses a context path that is the same as module id. For example, the REST module ID results in the context path ``/rest``

**Changing context path for REST module to ``/api``.**

.. code:: dsl

   http {
       rest {
           context^path = '/api'
       }
   }


List of virtual hosts
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This provides the ability to limit modules to be available only on listed virtual hosts, and allows to set context path to ``/`` for more than one module. Property accepts list of strings, which in the case of config.tdsl file format is list of comma separated domain names and in DSL it is written as list of strings (see :ref:`Complex Example<complexExample>`).

**Moving the REST module to be available only for requests directed to ``api.example.com``.**

.. code:: dsl

   http {
       rest {
           vhosts = [ 'api.example.com' ]
       }
   }

.. _complexExample:

Complex example
^^^^^^^^^^^^^^^^^^^

In this example we will disable the Index module and move REST module to ``http://api.example.com/`` and ``http://rest.example.com``.

.. code:: dsl

   http {
       index (active: false) {}
       rest {
           context^path = '/'
           vhosts = [ 'api.example.com', 'rest.example.com' ]
       }
   }