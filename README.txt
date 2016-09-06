Requirements
============


MongoDB
https://www.mongodb.com/

RabbitMQ
https://www.rabbitmq.com
enable management plugin 
rabbitmq-plugins enable rabbitmq_management

"guest" user can only connect via localhost
By default, the guest user is prohibited from connecting to the broker remotely; it can 
only connect over a loopback interface (i.e. localhost). This applies both to AMQP and 
to any other protocols enabled via plugins. Any other users you create will not (by default) 
be restricted in this way. This is configured via the loopback_users item in the configuration file.

If you wish to allow the guest user to connect from a remote host, you should set 
the loopback_users configuration item to []. A complete rabbitmq.config which does 
this would look like:
[{rabbit, [{loopback_users, []}]}]


visit http://localhost:15672/


Running
=======

Start mongoDB
Start rabbitMQ
#Start solr
#Start web
Start subs
#Start loader
#Start indexer
POST to subs
#poll web for sample
PUT to subs
#poll web for updated sample
#Stop web
#Stop solr
Stop subs
Stop rabbitMQ
Stop mongoDB