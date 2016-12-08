package net.semanticmetadata.lire.solr.indexing;

/**
 * This file is part of LIRESolr, a Java library for content based image retrieval.
 *
 * @author Mathias Lux, mathias@juggle.at, 08.12.2014
 */
public class LogoCaImageDataProcessor implements ImageDataProcessor {
    @Override
    public CharSequence getTitle(String filename) {
        return filename.replace("D:\\DataSets\\WIPO\\CA\\converted-", "").replaceAll("\\\\", "/");
    }

    @Override
    public CharSequence getIdentifier(String filename) {
        return filename.replace("D:\\DataSets\\WIPO\\CA\\converted-", "").replaceAll("\\\\", "/");
    }

    @Override
    public CharSequence getAdditionalFields(String filename) {
        return "";
    }
}
