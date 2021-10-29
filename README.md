**SCALAGRAM**

*Infra setup*

This project uses docker to run the infra. 

Create the following containers in docker using the commands: 

```` 
docker run -p:5432:5432 --name postgres -e POSTGRES_PASSWORD=postgres -d postgres 
````

```` 
docker run -p 6379:6379 --name redis -d redis
````

```` 
docker run -it -p 6650:6650 -p 8080:8080 --name pulsar --mount source=pulsardata,target=/pulsar/data --mount source=pulsarconf,target=/pulsar/conf apachepulsar/pulsar:2.8.1 bin/pulsar standalone
````

*Configuration*

Before running the application, you must create PostgresSQL tables and Pulsar topics. Do it running the main object class as such: 

```` 
sbt "runMain helpers.Setup"
````

*Running the Flutter App*

To run the Flutter App you must have Flutter SDK installed in your machine, create an Android virtual device and Android Studio IDE!