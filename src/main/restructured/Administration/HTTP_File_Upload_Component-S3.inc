S3 support for HTTP File Upload
-------------------------------------

By default HTTP File Upload component shipped with Tigase XMPP Server stores uploaded files locally in the directory structure. If you are using AWS it may be better to store data using external service like S3 which are better suited for this task and are more resilient.

.. Note::

   For this feature to work please make sure that you are using all required dependencies (either by using ``-dist-max`` package or obtaining them from `tigase-extras ``aws`` module <https://github.com/tigase/tigase-extras/tree/master/aws>`__)


Enabling storage in S3
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To enable storage in S3, you need to add following lines to your configuration file:

.. code:: dsl

   upload () {
       store (class: tigase.extras.http.upload.S3Store, active: true, exportable: true) {
           bucket = 'bucket-name'
       }
   }

This will enable HTTP File Upload component and configure it to be used with S3 bucket named ``bucket-name`` in the same region as your EC2 instance on which Tigase XMPP Server is running.

.. Warning::

    You would need to manually create this S3 bucket and allow your EC2 instance to access it (read and write). Alternatively, you could add ``autocreateBucket = true`` inside ``store`` block, which will enable Tigase XMPP Server to create this S3 bucket in the local AWS region.

If you wish to use S3 bucket from another AWS region, you can do that by adding setting ``region`` property in the ``store`` block to the id of the AWS region, ie. set to ``us-west-2`` to use ``US West (Oregon)`` region:

.. code:: dsl

   upload () {
       store (class: tigase.extras.http.upload.S3Store, active: true, exportable: true) {
           bucket = 'bucket-name'
           region = 'us-west-2'
       }
   }

If you wish to share the same S3 bucket between different installations of Tigase XMPP Server, you should configure ``bucketKeyPrefix`` property of ``store`` with different identifiers for each installation. That will allow you to easily filter data uploaded for each installation and will allow Tigase XMPP Server to provide you with correct storage usage for each installation.

.. code:: dsl

   upload () {
       store (class: tigase.extras.http.upload.S3Store, active: true, exportable: true) {
           bucket = 'bucket-name'
           bucketKeyPrefix = '45252AF'
       }
   }

S3Store requires appropriate IAM policy:

.. code:: json

   {
       "Version": "2012-10-17",
       "Statement": [
           {
               "Sid": "VisualEditor0",
               "Effect": "Allow",
               "Action": [
                   "s3:ListStorageLensConfigurations",
                   "s3:GetAccessPoint",
                   "s3:PutAccountPublicAccessBlock",
                   "s3:GetAccountPublicAccessBlock",
                   "s3:ListAllMyBuckets",
                   "s3:ListAccessPoints",
                   "s3:ListJobs",
                   "s3:PutStorageLensConfiguration",
                   "s3:CreateBucket",
                   "s3:GetBucketLocation"
               ],
               "Resource": "*"
           },
           {
               "Sid": "VisualEditor1",
               "Effect": "Allow",
               "Action": [
                   "s3:*"
               ],
               "Resource": [
                   "arn:aws:s3:::bucket-name",
                   "arn:aws:s3:::bucket-name/*",
                   "arn:aws:s3:*:441807014745:accesspoint/*",
                   "arn:aws:s3:*:441807014745:storage-lens/*",
                   "arn:aws:s3:*:441807014745:job/*"
               ]
           }
       ]
   }