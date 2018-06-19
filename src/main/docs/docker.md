# LireSolr on Docker

The LireSolr project is available at [Github](https://github.com/dermotte/liresolr). You can find installation instructions, documentation, the Dockerfile and source code there. It's the version the docker image is built from. The image is available on [Docker Hub](https://hub.docker.com/r/dermotte/liresolr/).

This Docker image comes with a pre-installed core on a runnning Solr 6.4.0 server by extending the official Solr image. It allows for indexing images and the use of the *LireRequestHandler*. 100 sample documents are available at [Github](https://raw.githubusercontent.com/dermotte/liresolr/master/src/test/resources/sampledocuments.xml) and you can import them using the web interface of Solr.

## Run the image / container
First of all install the Docker engine following the [installation instructions](https://docs.docker.com/engine/installation/). Then run the image with

``
$> docker run -p 8983:8983 dermotte/liresolr:latest
``

Note that with Docker you can run an image and will create a container. This container then retains your changes, the image will not. You can think of that like the image is the class and the container is the instance. Once you have run an image, you can find the container with

``
$> docker ps -a
``

If you lookup the id at this list, then you will be able to start and stop your container with

```
$> docker stop <id>
$> docker start <id>
```


## Try out the following things ...

* Connect to the running server on (http://localhost:8983)
* Select the `lire` core (drop down list on the left side of the admin interface)
* Go to the `documents` option (in the list below the core selection)
* Select `document type` -> `file upload`
* Upload the sample file from [Github](https://raw.githubusercontent.com/dermotte/liresolr/master/src/test/resources/sampledocuments.xml)
* Either try a query in the query window or go to [http://localhost:8983/solr/lire.html](http://localhost:8983/solr/lire.html)

## What to do next?

You might index more Flickr photos by using the provided tool. Download both `lire.jar` and `liresolr.jar` from the [Github release page](https://github.com/dermotte/liresolr/releases) and run

``
$> java -cp lire.jar;liresolr.jar net.semanticmetadata.lire.solr.tools.FlickrSolrIndexingTool -o out.xml -n 50
``

The resulting file `out.xml` contains 50 more images to send to the server and extends your set of indexed images.
