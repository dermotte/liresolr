package net.semanticmetadata.lire.solr.indexing;

/**
 * This file is part of LIRESolr, a Java library for content based image retrieval.
 *
 * @author Mathias Lux, mathias@juggle.at, 08.12.2014
 */
public class LogoCaImageDataProcessor extends AbstractImageDataProcessor {
    
	@Override
    public String getTitle() {
        return getFilePath().substring(getFilePath().lastIndexOf("converted-")+"converted-".length()).replaceAll("\\\\", "/");
    }

    @Override
    public String getIdentifier() {
        return getFilePath().substring(getFilePath().lastIndexOf("converted-")+"converted-".length()).replaceAll("\\\\", "/");
    }
}
