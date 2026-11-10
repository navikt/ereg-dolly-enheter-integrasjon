# syntax=docker.io/docker/dockerfile:1.7-labs
FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
LABEL maintainer="Team Arbeidsforhold"
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo" LD_LIBRARY_PATH="/usr/lib"
COPY target/ereg-dolly-enheter-integrasjon-*.jar /app/app.jar

CMD ["-jar","/app/app.jar"]
