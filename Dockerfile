FROM openjdk:8

COPY /target/universal/stage /scalagram

WORKDIR scalagram

CMD ["cd", "scalagram"]
CMD ["chmod", "+x", "./bin/scalagram"]
# Run the web service on container startup.
#CMD ["ls"]
CMD ["./bin/scalagram", "-Dconfig.resource=production.conf", "-Dlogger.resource=prod-logger.xml"]
#CMD ["java", "-cp", "../lib/*", "play.core.server.ProdServerStart"]

EXPOSE 9000
