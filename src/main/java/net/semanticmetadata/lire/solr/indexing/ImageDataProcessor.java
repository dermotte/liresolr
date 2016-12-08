package net.semanticmetadata.lire.solr.indexing;

/**
 * This file is part of LIRE-Solr, a Java library for content based image retrieval.
 *
 * @author Mathias Lux, mathias@juggle.at, 08.12.2014
 */
public interface ImageDataProcessor {
    public CharSequence getTitle(String filename);
    public CharSequence getIdentifier(String filename);
    public CharSequence getAdditionalFields(String filename);
}
