# SOLR Installation
*Version: 6.3.0*

## First Steps: Get Solr running
Using the plain Solr installation in bin\solr start it with
```
$> bin/solr start
```
Then one should basically create a core using
```
$> bin/solr create_collection -c lire -d basic_configs
```
Then you are up to changing the config files to your need. To check if it worked point your browser to http://localhost:[port]/ whereas the port should be stated in your shell, where you started Solr.

## Where to learn more?
I assume the better way to learn to handle Solr is by reading the Solr documentation:

1. [Solr Reference Guide](https://cwiki.apache.org/confluence/display/solr/Getting+Started)
1. [Solr web page](http://lucene.apache.org/solr/)