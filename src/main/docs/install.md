# SOLR Installation
*Version: 7.5.0*

## First Steps: Get Solr running
Using the plain Solr installation in bin\solr start it with
```
$> bin/solr start
```
Then one should basically create a core using
```
$> bin/solr create_core -c lire
```
Then you are up to changing the config files to your need. To check if it worked point your browser to http://localhost:[port]/ whereas the port should be stated in your shell, where you started Solr.

## Where to learn more?
I assume the better way to learn to handle Solr is by reading the Solr documentation:

1. [Solr Reference Guide](https://lucene.apache.org/solr/guide/7_5/getting-started.html)
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

## Solr Cross-Origin Development
If you want to access the Solr server from any other web page, you have to add the following lines to the `web.xml` file in `<solr-version>/server/solr-webapp/webapp/WEB-INF`:

```
<filter>
    <filter-name>cross-origin</filter-name>
    <filter-class>org.eclipse.jetty.servlets.CrossOriginFilter</filter-class>
    <init-param>
        <param-name>allowedOrigins</param-name>
        <param-value>*</param-value>
    </init-param>
    <init-param>
        <param-name>allowedMethods</param-name>
        <param-value>GET,POST,OPTIONS,DELETE,PUT,HEAD</param-value>
    </init-param>
    <init-param>
        <param-name>allowedHeaders</param-name>
        <param-value>origin, content-type, accept</param-value>
    </init-param>
</filter>

<filter-mapping>
    <filter-name>cross-origin</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```