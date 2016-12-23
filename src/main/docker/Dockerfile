FROM solr:6.3.0

MAINTAINER  Mathias Lux "mlux@itec.aau.at"

RUN wget -nv https://github.com/dermotte/liresolr/releases/download/v6.3.0-beta.1/lire-sample-core.tar.gz -O /opt/solr/server/solr/lire-sample-core.tar.gz && \
    tar -C /opt/solr/server/solr -xzf /opt/solr/server/solr/lire-sample-core.tar.gz && \
    rm /opt/solr/server/solr/lire-sample-core.tar.gz && \
    wget -nv https://github.com/dermotte/liresolr/releases/download/v6.3.0-beta.1/lire.jar -O /opt/solr/server/solr-webapp/webapp/WEB-INF/lib/lire.jar && \
    wget -nv https://github.com/dermotte/liresolr/releases/download/v6.3.0-beta.1/liresolr.jar -O /opt/solr/server/solr-webapp/webapp/WEB-INF/lib/liresolr.jar
