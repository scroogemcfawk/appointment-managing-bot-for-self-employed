FROM alpine:3.20

RUN apk add --no-cache bash
RUN apk add --no-cache vim
RUN apk add --no-cache openjdk21-jre-headless

COPY build/libs/samurai-1*.jar start.sh stop.sh /app/

WORKDIR /app

ENTRYPOINT ["/bin/bash"]
