# -- Building the docker image ...
# Make sure you run 'gradle prepareForDockerImage' first.
# $ docker build -t dermotte/liresolr:6.3.0b2 .
# $ docker tag <id> dermotte/liresolr:latest
# $ docker login
# $ docker push dermotte/liresolr:latest
#
# -- Running the docker image
# $ docker run -p 8983:8983 dermotte/liresolr:latest

FROM solr:6.3.0

MAINTAINER  Mathias Lux "mathias@juggle.at"

# copy the sample core and untar it ...
COPY lire-sample-core.tar.gz /opt/solr/server/solr/
RUN tar -C /opt/solr/server/solr -xzf /opt/solr/server/solr/lire-sample-core.tar.gz && \
    rm /opt/solr/server/solr/lire-sample-core.tar.gz
# use the jars from the dist folder ...
COPY *.jar /opt/solr/server/solr-webapp/webapp/WEB-INF/lib/
# copy the web app from the src/main/web folder ...
COPY lire.html /opt/solr/server/solr-webapp/webapp/
COPY gridify.js /opt/solr/server/solr-webapp/webapp/
