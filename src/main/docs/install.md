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
1. [Using Xml messages to update the index](https://wiki.apache.org/solr/UpdateXmlMessages)

## Common Solr Commands

* Deleting everything in the core: `<delete><query>*:*</query></delete>`
* Committing changes: `<commit/>`

You may also use the browser directly, changing "lire" to the name of the core you are using:
```
http://localhost:8983/solr/lire/update?stream.body=<delete><query>*:*</query></delete>
http://localhost:8983/solr/lire/update?stream.body=<commit/>
```

Or of course using `curl` with direct XML

```
curl http://localhost:8983/solr/lire/update -H "Content-Type: text/xml" --data-binary '<commit waitFlush="false" waitSearcher="false"/>'
```

Or using `curl` with XML files
```
curl http://localhost:8983/solr/lire/update -H "Content-Type: text/xml" --data-binary @input-data.xml
```