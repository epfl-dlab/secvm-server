# SecVM Server

This repository contains the server code for the online evaluation in Sec. 6 of the paper 'Privacy-Preserving Classification with Secret Vector Machines': https://arxiv.org/abs/1907.03373

The README in the respository with the [client code](https://github.com/cliqz-oss/browser-core/tree/6945afff7be667ed74b0b7476195678262120baf/modules/secvm/sources) contains more information on the project and on the implementation.

## Setting up the backend
### Database
1. Install MySQL and set a root password.
2. Create the database, fill it with sample data:
```
cd database
mysql -u root -p
source create-SecVM_DB.sql;
source fill-database.sql;
```
3. Create the MySQL user and give them access to the database (stay inside the MySQL command prompt):
```
CREATE USER 'java'@'localhost' IDENTIFIED BY 'java';
GRANT ALL PRIVILEGES ON SecVM_DB . * TO 'java'@'localhost';
```

### Java
1. Install Java (at least version 8) and Maven.
2. Compile the server:
```
mvn clean install
```
4. Create the folder that contains the files that are later fetched by the clients:
```
mkdir experiment-configs
```

## Running the server
In the production setting, we ran the main server behind NGINX and also served the files via NGINX. For testing purposes, one can do without NGINX.

1. Start the main server:
```
java -Dsun.net.httpserver.maxReqTime=1 -Dsun.net.httpserver.maxRspTime=1 -Dsun.net.httpserver.idleInterval=1 -cp target/secvmserver-1.0-jar-with-dependencies.jar com.cliqz.secvmserver.Server 4
```
2. Start the file server (requires Python):
```
cd SecVM-Server/experiment-configs
python -m SimpleHTTPServer
```

3. Start Firefox with the Cliqz plugin that contains the SecVM module and log in at facebook.com.

4. Check if packages arrive (new terminal):
```
mysql -u java -p
connect SecVM_DB;
select * from package_test;
select * from package_participation;
select * from package_train;
```

5. Shut the server down:
type `stop`, followed by enter.
