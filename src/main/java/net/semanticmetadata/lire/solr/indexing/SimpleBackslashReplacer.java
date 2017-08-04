package net.semanticmetadata.lire.solr.indexing;

/**
 * Created by mlux on 19.12.2016.
 */
public class SimpleBackslashReplacer extends AbstractImageDataProcessor {
    @Override
    public String getTitle() {
        return getFilePath().substring(getFilePath().indexOf(":")+1).replaceAll("\\\\", "/");
    }

    @Override
    public String getIdentifier() {
        return getFilePath().substring(getFilePath().indexOf(":")+1).replaceAll("\\\\", "/");
    }
}
