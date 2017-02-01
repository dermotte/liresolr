D:
cd D:\Java\Libs\solr-6.4.0
bin\solr stop -all | more
xcopy C:\Java\Projects\LireSolr\dist\lire.jar D:\Java\Libs\solr-6.4.0\server\solr-webapp\webapp\WEB-INF\lib /Y
xcopy C:\Java\Projects\LireSolr\dist\liresolr.jar D:\Java\Libs\solr-6.4.0\server\solr-webapp\webapp\WEB-INF\lib /Y
bin\solr start
cd C:\Java\Projects\LireSolr