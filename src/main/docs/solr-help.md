## Common Solr Commands

* Deleting everything in the core: `<delete><query>*:*</query></delete>`
* Committing changes: `<commit/>`

You may also use the browser directly, changing "lire" to the name of the core you are using:
```
http://localhost:8983/solr/lire/update?stream.body=<delete><query>*:*</query></delete>
http://localhost:8983/solr/lire/update?stream.body=<commit/>
http://localhost:8983/solr/lire/update?stream.body=<optimize/>
```

Or of course using `curl` with direct XML
```
curl http://localhost:8983/solr/lire/update -H "Content-Type: text/xml" --data-binary '<commit waitFlush="false" waitSearcher="false"/>'
```

Or using `curl` with XML files
```
curl http://localhost:8983/solr/lire/update -H "Content-Type: text/xml" --data-binary @input-data.xml
```

## Problems with the cache

If sorting with the lirefunc doesn't work as intended, i.e. if the first sort works for each query, but all following do deliver the same results, then caching gets in the way. 