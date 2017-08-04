package net.semanticmetadata.lire.solr.indexing;

/**
 * This file is part of LIRE-Solr, a Java library for content based image retrieval.
 *
 * @author Mathias Lux, mathias@juggle.at, 08.12.2014
 */
public interface ImageDataProcessor {
    public String getTitle();
    public String getIdentifier();
    public String getAdditionalFields();
    public String getFilePath();
    public void setImageData(String imageData);
    public String getImageData();
    public void appendSolrFields(StringBuilder sb);
}
