Requirements
============


MongoDB
https://www.mongodb.com/

RabbitMQ
https://www.rabbitmq.com
enable management 
plugin rabbitmq-plugins enable rabbitmq_management
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