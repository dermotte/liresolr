package net.semanticmetadata.lire.solr.indexing;

import org.apache.commons.lang3.StringUtils;

/**
 *  This data processor removes the path from the file to keep only
 *  the last segment (the actual file name), and store it as the title. The
 *  same file name is used for the identifier, minus the extension (if any).
 *  @author Pascal Essiembre
 */
public class FileNameDataProcessor implements ImageDataProcessor {

    @Override
    public CharSequence getTitle(String filename) {
        return StringUtils.substringAfterLast(
                filename.replace('\\', '/'), "/");
    }

    @Override
    public CharSequence getIdentifier(String filename) {
        return StringUtils.substringBefore(StringUtils.substringAfterLast(
                filename.replace('\\', '/'), "/"), ".");
    }

    @Override
    public CharSequence getAdditionalFields(String filename) {
        return "";
    }
}
