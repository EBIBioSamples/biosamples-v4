#script for installing all the pre-requisites on a ubuntu server 16.04 

#NOTE
#If using VirtualBox you will need to enable port forwarding.
#Find the guest IP by running ifconfig
#HostIP HostPort GuestIP GuestPort
#127.0.0.1 15672 x.x.x.x 15672



##RabbitMQ
sudo apt-get install -y rabbitmq-server 
rabbitmq-plugins enable rabbitmq_management
echo '[{rabbit, [{loopback_users, []}]}].' | sudo tee /etc/rabbitmq/rabbitmq.config

#PostgreSQL
sudo apt-get install -y postgresql
	
#if you need to allow connection from other hosts for e.g. DBeaver
sudo echo 'host    all     all        samenet                 md5' >> etc/postresql/9.5/main/pg_hpa.conf
# edit /etc/postresql/9.5/main/postresql.conf
# replace #listen_addresses = 'localhost' with listen_addresses = '*'
#set a new password for postgres database user
sudo -u postgres postgres psql -c '\password postgres' postgres
	
#MongoDB
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927
echo 'deb http://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/3.2 multiverse' | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list
sudo apt-get update
sudo apt-get install -y mongodb-org

#Neo4J
wget -O - https://debian.neo4j.org/neotechnology.gpg.key | sudo apt-key add -
echo 'deb http://debian.neo4j.org/repo stable' | sudo tee /etc/apt/sources.list.d
sudo apt-get update
#under AGPL we can use enterprise see https://neo4j.com/open-source/
sudo apt-get install -y neo4j-enterprise
	
	
#Tomcat
wget 'http://mirrors.ukfast.co.uk/sites/ftp.apache.org/tomcat/tomcat-8/v8.0.37/bin/apache-tomcat-8.0.37.tar.gz'
tar -xvf apache-tomcat-8.0.37.tar.gz
	
#Maven and JDK
sudo apt-get install -y maven default-jdk git
		
#get a copy from git
git clone https://github.com/EBIBioSamples/biosamples-v4

#go into that copy 
cd biosamples-v4

#build it
mvn clean package
