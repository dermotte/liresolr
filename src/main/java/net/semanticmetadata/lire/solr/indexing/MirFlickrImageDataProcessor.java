package net.semanticmetadata.lire.solr.indexing;

import net.semanticmetadata.lire.utils.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * This file is part of LIRESolr, a Java library for content based image retrieval.
 *
 * @author Mathias Lux, mathias@juggle.at, 08.12.2014
 */
public class MirFlickrImageDataProcessor extends AbstractImageDataProcessor {
    @Override
    public String getTitle() {
//        return filename.replace("G:\\", "").replaceAll("\\\\", "/");
        return getFilePath().replace("D:\\DataSets\\MirFlickr\\", "").replaceAll("\\\\", "/");
    }

    @Override
    public String getIdentifier() {
//        return filename.replace("G:\\", "").replaceAll("\\\\", "/");
        return getFilePath().replace("D:\\DataSets\\MirFlickr\\", "").replaceAll("\\\\", "/");
    }

    @Override
    public String getAdditionalFields() {
        StringBuilder sb = new StringBuilder(1024);
        int fileNumber = Integer.parseInt(getFilePath().substring(getFilePath().lastIndexOf("\\")).replaceAll("[^0-9]", "")) - 1;
        String tagFileName = "D:\\DataSets\\MirFlickr\\tags_raw\\" + (fileNumber/10000) + "\\" + fileNumber + ".txt";
//        String tagFileName = filename.substring(0, filename.lastIndexOf("\\")) + "\\" + fileNumber + ".txt";
        try {
            sb.append("<field name=\"tags\">");
            StringBuilder tmp = new StringBuilder();
            FileUtils.readWholeFile(new File(tagFileName), tmp);
            sb.append(tmp.toString().replaceAll("\\s", " "));  // replacing \n\r\t and all those with " "
            sb.append("</field>");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return sb.toString();
    }
 	
}
