# -- Building the docker image ...
# $ docker build -t dermotte/liresolr:6.3.0b2 .
# $ docker tag <id> dermotte/liresolr:latest
# $ docker login
# $ docker push dermotte/liresolr:latest
#
# -- Running the docker image
# $ docker run -p 8983:8983 dermotte/liresolr:latest

FROM solr:6.3.0

MAINTAINER  Mathias Lux "mlux@itec.aau.at"

RUN wget -nv https://github.com/dermotte/liresolr/releases/download/v6.3.0-beta.2/lire-sample-core.1.tar.gz -O /opt/solr/server/solr/lire-sample-core.tar.gz && \
    tar -C /opt/solr/server/solr -xzf /opt/solr/server/solr/lire-sample-core.tar.gz && \
    rm /opt/solr/server/solr/lire-sample-core.tar.gz && \
    wget -nv https://github.com/dermotte/liresolr/releases/download/v6.3.0-beta.2/lire.jar -O /opt/solr/server/solr-webapp/webapp/WEB-INF/lib/lire.jar && \
    wget -nv https://github.com/dermotte/liresolr/releases/download/v6.3.0-beta.2/liresolr.jar -O /opt/solr/server/solr-webapp/webapp/WEB-INF/lib/liresolr.jar && \
    wget -nv https://github.com/dermotte/liresolr/releases/download/v6.3.0-beta.2/sample-liresolr-webapp.tar.gz -O /opt/solr/server/solr-webapp/webapp/sample-liresolr-webapp.tar.gz && \
    tar -C /opt/solr/server/solr-webapp/webapp -xzf /opt/solr/server/solr-webapp/webapp/sample-liresolr-webapp.tar.gz && \
    rm /opt/solr/server/solr-webapp/webapp/sample-liresolr-webapp.tar.gz
