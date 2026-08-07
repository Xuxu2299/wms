FROM eclipse-temurin:17-jre-alpine

MAINTAINER xuzhihao074

RUN mkdir -p /ruoyi/server/logs \
    /ruoyi/server/temp

WORKDIR /ruoyi/server

ENV TZ=Asia/Shanghai
ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-admin-wms.jar ./app.jar

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dserver.port=${SERVER_PORT}", \
            "-Duser.timezone=${TZ}", \
            "-jar", "app.jar"]
