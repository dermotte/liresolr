LIRE Solr Integration Project
=============================

This is a Solr plugin for the LIRE content based image retrieval library, so basically it's for indexing images and then finding similar (looking) ones. The original library can be found at [Github](https://github.com/dermotte/lire)

The LIRE Solr plugin includes a `RequestHandler` for searching, an `EntityProcessor` for indexing, a `ValueSource` Parser for content based re-ranking and a parallel indexing application.

An outdated demo can be found at [http://demo-itec.uni-klu.ac.at/liredemo/](http://demo-itec.uni-klu.ac.at/liredemo/). If you want to give it a try yourself, there is a [docker image](https://hub.docker.com/r/dermotte/liresolr/), which you use to run a pre-configured core on a Solr server. There's also a tool to create import XML files from Flickr. More information is available at [src/main/docs/docker.md](src/main/docs/docker.md).

If you need help on the plugin, please use the mailing list at [lire-dev mailing list](http://groups.google.com/group/lire-dev) to ask questions. Additional documentation is available on [src/main/docs/index.md](src/main/docs/index.md) If you need help with your project, please contact me, we also offer consulting services.

If you use LIRE Solr for scientific purposes, please cite the following paper: 

> *Mathias Lux and Glenn Macstravic* "The LIRE Request Handler: A Solr Plug-In for Large Scale Content Based Image Retrieval." *MultiMedia Modeling. Springer International Publishing, 2014*. [Springer](http://link.springer.com/chapter/10.1007/978-3-319-04117-9_39)

The `RequestHandler` supports the following different types of queries

1.  Get random images ...
2.  Get images that are looking like the one with id ...
3.  Get images looking like the one found at url ...
4.  Get images with a feature vector like ...
5.  Extract histogram and hashes from an image URL ...

Preliminaries
-------------
Supported values for feature field parameters, e.g. `lireq?field=cl` are ...

-  **ph** .. PHOG (pyramid histogram of oriented gradients)
-  **oh** .. OpponentHistogram (simple color histtogram in the opponent color space)
-  **cl** .. ColorLayout (from MPEG-7)
-  **sc** .. ScalableColor (from MPEG-7)
-  **eh** .. EdgeHistogram (from MPEG-7)
-  **ce** .. CEDD (very compact and accurate joint descriptor)
-  **fc** .. FCTH (more accurate, less compact than CEDD)
-  **jc** .. JCD (joined descriptor of CEDD and FCTH)
-  **ac** .. AutoColorCorrelogram (color to color correlation histogram)
-  **pc** .. SPCEDD (pyramid histogram of CEDD)
-  **fo** .. FuzzyOpponentHistogram (fuzzy color histogram)

Also consult the Lire project and the documentation of features there. You can also extend the list of features by changing the `FeatureRegistry` in the LireSolr source.

The field parameter (partially) works with the LIRE request handler:

-  **fl** .. Fields, give them as a comma or space separated list, like "fl=title,id,score". Note that "*" is denoting all fields and score adds the distance (which already comes with the "d" fields) in an additional score field.
-  **fq** .. Filter query, give them as a comma separated list in the format "fq=tags:dog tags:funny". No wildcards and no spaces in terms supported for now.

Getting random images
---------------------
Returns randomly chosen images from the index. While it does not seem extremely helpful, it's actually great to find images to be used for example queries. 

Parameters:

-   **rows** ... indicates how many results should be returned (optional, default=60). Example: lireq?rows=30

Search by ID
------------
Returns images that look like the one with the given ID.

Parameters:

-   **id** .. the ID of the image used as a query as stored in the "id" field in the index.
-   **field** .. gives the feature field to search for (optional, default=cl_ha, values see above)
-   **rows** .. indicates how many results should be returned (optional, default=60).
-   **ms** .. prefer MetricSpaces over BitSampling (optional, default=true).
-   **accuracy** .. double in [0.05, 1] indicates how many accurate the results should be (optional, default=0.33, less is less accurate, but faster).
-   **candidates** .. int in [100, 100000] indicates how many accurate the results should be (optional, default=10000, less is less accurate, but faster).

Search by URL
-------------
Returns images that look like the one found at the given URL.

Parameters:

-   **url** .. the URL of the image used as a query. Note that the image has to be accessible by the web server Java has to be able to read it.
-   **field** .. gives the feature field to search for (optional, default=cl_ha, values see above)
-   **rows** .. indicates how many results should be returned (optional, default=60).
-   **ms** .. prefer MetricSpaces over BitSampling (optional, default=true).
-   **accuracy** .. double in [0.05, 1] indicates how many accurate the results should be (optional, default=0.33, less is less accurate, but faster).
-   **candidates** .. int in [100, 100000] indicates how many accurate the results should be (optional, default=10000, less is less accurate, but faster).

Search by feature vector
------------------------
Returns an image that looks like the one the given features were extracted. This method is used if the client extracts the features from the image, which makes sense if the image should not be submitted.

Parameters:

-   **hashes** .. Hashes of the image feature as returned by BitSampling#generateHashes(double[]) as a String of white space separated numbers.
-   **feature** .. Base64 encoded feature histogram from LireFeature#getByteArrayRepresentation().
-   **field** .. gives the feature field to search for (optional, default=cl_ha, values see above)
-   **rows** .. indicates how many results should be returned (optional, default=60).
-   **ms** .. prefer MetricSpaces over BitSampling (optional, default=true).
-   **accuracy** .. double in [0.05, 1] indicates how many accurate the results should be (optional, default=0.33, less is less accurate, but faster).
-   **candidates** .. int in [100, 100000] indicates how many accurate the results should be (optional, default=10000, less is less accurate, but faster).

Extracting histograms
---------------------
Extracts the histogram and the hashes of an image for use with the Lire sorting function. It will give you hashes and a truncated query for BitSampling (`bs_list` and `bs_query`) and MetricSpaces (`ms_list` and `ms_query`), but the latter only if it's available. the return values for `bs_list` and `ms_list` are ordered by ascending document frequency (BitSampling) and distance from the image to the respective reference point. 

Parameters:

-   **extract** .. the URL of the image. Note that the image has to be accessible by the web server Java has to be able to read it.
-   **field** .. gives the feature field to search for (optional, default=cl_ha, values see above)
-   **accuracy** .. double in [0.05, 1] indicates how many query terms should be in the queries.


Function querioes with lirefunc
-------------------------------
The function `lirefunc(arg1,arg2)` is available for function queries. Two arguments are necessary and are defined as:

-  Feature to be used for computing the distance between result and reference image. Possible values are {cl, ph, eh, jc}
-  Actual Base64 encoded feature vector of the reference image. It can be obtained by calling `LireFeature.getByteRepresentation()` and by Base64 encoding the resulting byte[] data or by using the extract feature of the `RequestHandler`
-  Optional maximum distance for those data items that cannot be processed, ie. don't feature the respective field.

Note that if you send the parameters using an URL you might take extra care of the URL encoding, ie. white space, the "=" sign, etc.

Examples:

-  `[solrurl]/select?q=*:*&fl=id,lirefunc(cl,"FQY5DhMYDg...AQEBA=")` – adding the distance to the reference image to the results
-  `[solrurl]/select?q=*:*&sort=lirefunc(cl,"FQY5DhMYDg...AQEBA=")+asc` – sorting the results based on the distance to the reference image

If you extract the features yourself, use code like his one:

    // ColorLayout
    ColorLayout cl = new ColorLayout();
    cl.extract(ImageIO.read(new File("...")));
    String arg1 = "cl";
    String arg2 = Base64.getEncoder().encodeToString(cl.getByteArrayRepresentation());

    // PHOG
    PHOG ph = new PHOG();
    ph.extract(ImageIO.read(new File("...")));
    String arg1 = "ph";
    String arg2 = Base64.getEncoder().encodeToString(ph.getByteArrayRepresentation());


Installation
============

We assume you have a Solr server installed and running and you have already added a core. If not, check [src/main/docs/install.md](src/main/docs/install.md) or don't even try but go for the docker image. First run the dist task by `gradlew distForSolr` command in folder where the `build.gradle` file is found to create a plugin jar. Then copy jars: `cp ./dist/*.jar /opt/solr/server/solr-webapp/webapp/WEB-INF/lib/`. Then add the new `RequestHandler` and the `ValueSourceParser` have to be registered in the `solrconfig.xml` file:

    <requestHandler name="/lireq" class="net.semanticmetadata.lire.solr.LireRequestHandler">
        <lst name="defaults">
            <str name="echoParams">explicit</str>
            <str name="wt">json</str>
            <str name="indent">true</str>
        </lst>
    </requestHandler>
     
    <valueSourceParser name="lirefunc" 
        class="net.semanticmetadata.lire.solr.LireValueSourceParser" />

Use of the request handler is detailed above.

You'll also need the respective fields in the `managed-schema` file:

    <!-- file path for ID, should be there already -->
    <field name="id" type="string" indexed="true" stored="true" required="true" multiValued="false" />
    <!-- the title of the image, e.g. the file name, optional -->
    <field name="title" type="text_general" indexed="true" stored="true" multiValued="true"/>
    <!-- the url where the image is to be downloaded, optional  -->
    <field name="imgurl" type="string" indexed="true" stored="true" multiValued="false"/>
    <!-- Dynamic fields for LIRE Solr -->
    <dynamicField name="*_ha" type="text_ws" indexed="true" stored="false"/> <!-- if you are using BitSampling --> 
    <dynamicField name="*_ms" type="text_ws" indexed="true" stored="false"/> <!-- if you are using Metric Spaces Indexing -->
    <dynamicField name="*_hi" type="binaryDV" indexed="false" stored="true"/>

Do not forget to add the custom field at the very same file:

    <fieldtype name="binaryDV" class="net.semanticmetadata.lire.solr.BinaryDocValuesField"/>


Indexing
========

Check `ParallelSolrIndexer.java` for indexing. It creates XML documents (either one per image or one single large file)
to be sent to the Solr Server.

ParallelSolrIndexer
-------------------
This help text is shown if you start the ParallelSolrIndexer with the '-h' option.

    $> ParallelSolrIndexer -i <infile> [-o <outfile>] [-n <threads>] [-f] [-p] [-m <max_side_length>] [-r <full class name>] \\
             [-y <list of feature classes>]

Note: if you don't specify an outfile just ".xml" is appended to the input image for output. So there will be one XML
file per image. Specifying an outfile will collect the information of all images in one single file.

- *-n* ... number of threads should be something your computer can cope with. default is 4.
- *-f* ... forces overwrite of outfile
- *-p* ... enables image processing before indexing (despeckle, trim white space)
- *-a* ... use both BitSampling and MetricSpaces.
- *-l* ... disables BitSampling and uses MetricSpaces instead.
- *-m* ... maximum side length of images when indexed. All bigger files are scaled down. default is 512.
- *-r* ... defines a class implementing net.semanticmetadata.lire.solr.indexing.ImageDataProcessor
       that provides additional fields.
- *-y* ... defines which feature classes are to be extracted. default is "-y ph,cl,eh,jc". "-y ce,ac" would
       add to the other four features.

INFILE
------
The infile gives one image per line with the full path. You can create an infile easily on Windows with running in the
parent directory of the images

    $> dir /s /b *.jpg > infile.txt

On linux just use find, grep and whatever you find appropriate. With find it'd look like this assuming that you run it
from the root directory:

    $> find /[path-to-image-base-dir]/ -name *.jpg

OUTFILE
-------
The `outfile` from `ParallelIndexer` has to be send to the Solr server. Assuming the Solr server is local you may use

    $> curl http://localhost:8983/solr/lire/update -H "Content-Type: text/xml" --data-binary "<delete><query>*:*</query></delete>"
    $> curl http://localhost:8983/solr/lire/update -H "Content-Type: text/xml" --data-binary @outfile.xml
    $> curl http://localhost:8983/solr/lire/update -H "Content-Type: text/xml" --data-binary "<commit/>"

You need to commit you changes! If your outfile exceeds 500MB, curl might complain. Then use split to cut it into pieces and repair the root tags (`<add>` and `</add>`). Here is an example how to do that with bash & linux (use *Git Bash* on Windows) under the assumption that the split leads to files *{0, 1, 2, ..., n}*

```
$> split -l 100000 -d images.xml images_
$> echo "</add>" >> images_00 
$> echo "</add>" >> images_01
...
$> echo "</add>" >> images_<n-1> 
$> sed -i.old '1s;^;<add>;' images_01
$> sed -i.old '1s;^;<add>;' images_02
...
$> sed -i.old '1s;^;<add>;' images_<n>
```

For small output files you may use the file upload option in the Solr admin interface. 

LireEntityProcessor
-------------------

Another way is to use the LireEntityProcessor. Then you have to reference the *solr-data-config.xml* file in the
*solrconfig.xml*, and then give the configuration for the EntityProcessor like this:

    <dataConfig>
        <dataSource name ="bin" type="BinFileDataSource" />
        <document>
            <entity name="f"
                    processor="FileListEntityProcessor"
                    transformer="TemplateTransformer"
                    baseDir="D:\Java\Projects\Lire\testdata\wang-1000\"
                    fileName=".*jpg"
                    recursive="true"
                    rootEntity="false" dataSource="null" onError="skip">
                <entity name="lire-test" processor="net.semanticmetadata.lire.solr.LireEntityProcessor" url="${f.fileAbsolutePath}" dataSource="bin"  onError="skip">
                    <field column="id"/>
                    <field column="cl_ha"/>
                    <field column="cl_hi"/>
                    <field column="ph_ha"/>
                    <field column="ph_hi"/>
                    <field column="oh_ha"/>
                    <field column="oh_hi"/>
                    <field column="jc_ha"/>
                    <field column="jc_hi"/>
                    <field column="eh_ha"/>
                    <field column="eh_hi"/>
                </entity>
            </entity>
        </document>
    </dataConfig>

*Mathias Lux, 2016-12-27*
