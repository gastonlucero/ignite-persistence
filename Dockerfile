FROM openjdk:alpine

WORKDIR /work

ADD ./target/scala-2.12/ignite-persistence.jar /work

EXPOSE 8000

CMD  ["java","-jar","/work/ignite-persistence.jar"]
