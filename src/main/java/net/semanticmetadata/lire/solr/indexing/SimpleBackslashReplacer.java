package net.semanticmetadata.lire.solr.indexing;

/**
 * Created by mlux on 19.12.2016.
 */
public class SimpleBackslashReplacer implements ImageDataProcessor {
    @Override
    public CharSequence getTitle(String filename) {
        return filename.substring(filename.indexOf(":")+1).replaceAll("\\\\", "/");
    }

    @Override
    public CharSequence getIdentifier(String filename) {
        return filename.substring(filename.indexOf(":")+1).replaceAll("\\\\", "/");
    }

    @Override
    public CharSequence getAdditionalFields(String filename) {
        return "";
    }
}
