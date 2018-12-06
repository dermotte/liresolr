# LireSolr Change Log

**2017-12-06**
* Switched to max norm for importing generic double features.

**2017-12-05**
* Extraction with lireq from double histograms to short features
* Documentation updates
* changed default to ms=false

**2017-12-04**
* lire.html updates to current bootstrap and jquery versions
* search.html for debugging (raw input of query URL)
* ImportFromCSV, Apache CLI integrated & XmlMerge generified.

**2017-12-01**
* Performance tests on lirefunc + RandomAccessBinaryDocValues
* Updated documentation on the LRUCache

**2017-11-16**
* Added merge tool based on DOM4j

**2017-11-15**
* Converged to Solr 7.5.0

**2017-11-10**
* Updated Docker file

**2017-02-05**
* Implemented image caching for FlickrDownloader tool and the respective use of the cache in the Python file for image classification.

**2017-02-03**
* Added Keras based category prediction in Python
* Updated the lire.html demo application 

**2017-02-01**
* Updated to Solr 6.4.0
* Updated docker image to Solr 6.4.0

**2016-12-28**
* Finished demo application, fixed JSON names with "-"
* Updated Docker image and turned off caching 

**2016-12-28**
* Extraction of hashes extended to extract everything possible.
* Fixed bugs in boosted query generation and HashTermStatistics.
* Updated documentation

**2016-12-28**
* Added gradle wrapper to the repo.
* Removed debug information and switched to ColorLayout as default feature in LireSourceValue
* 6.3.0b2 release for Docker image.

**2016-12-27**
* Docker integration
* Sample core and documents
* Flickr download tool to create test data in an easy way. 
* Fixed sample HTML file for random photo display.

**2016-12-19**
* Generating import XML files from text file (extraction in LIRE, output can be metric spaces file).
* Support for MetricSpaces indexing & search in Solr & ParallelSolrIndexer, ie. dynamic field `*_ms`.
* Re-visited LireValueSource implementation.
* Updated documentation.
* fixed FilterQuery.

**2016-12-08**
* automated build numbering on *gradle jar*.
* tested and implemented dynamic fields `*_hi` and `*_ha` 
* Added IDF sorting to BitSampling hash values, should now work faster and more precise
* Moved to Solr 6.3.0
* Updates on the documentation