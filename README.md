### spring-boot-kafka-interceptor

A interceptor design injection in http header on a request to kafka


The Project Diagram

![project-diagram](/images/project.png)

The Interceptor Diagram

![interpector-diagram](/images/interceptor.png)


Execution:

Run separately on each service:

```
    mvn spring-boot:run
```

On the root folder to build jaeger,cassandra and kafka run:

```
    docker-compose up
```

Test it!

To test it, just send a http post request in 0.0.0.0:9098

To see it on your jaeger, just go to http://0.0.0.0:16686 and select any service




