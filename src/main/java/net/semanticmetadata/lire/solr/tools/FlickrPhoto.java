package net.semanticmetadata.lire.solr.tools;

import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.*;
import net.semanticmetadata.lire.indexers.hashing.BitSampling;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple tool for grabbing data from the Flickr public feed to index something in the LireSolr search service. Use the
 * command line tool {@link FlickrSolrIndexingTool} to create an XML file that can be uploaded to Solr.
 *
 * @author Mathias Lux, mathias@juggle.at, Dec 2016
 */
public class FlickrPhoto implements Runnable {
    String title, url, photourl, authorName, authorID;
    BufferedImage img = null;
    List<String> tags;
    String xml = null;

    /**
     * @param title    title of the image as defined by Flickr.
     * @param url      gives the URL to the page where Flickr presents the image.
     * @param photourl gives tha actual href to the image data.
     * @param tags     the tags assigned to the image by the user.
     */
    public FlickrPhoto(String title, String url, String photourl, List<String> tags) {
        this.title = title;
        this.url = url;
        this.photourl = photourl;
        this.tags = new LinkedList<String>(tags);
    }

    public FlickrPhoto(String title, String url, String photourl, List<String> tags, String authorName, String authorID) {
        this.title = title;
        this.url = url;
        this.photourl = photourl;
        this.authorName = authorName;
        this.authorID = authorID;
        this.tags = tags;
    }

    public String toString() {
        return title + ": " + url + " (" + photourl + ")";
    }

    public void generateSolrDocument() throws IOException {
        xml = null;
        StringBuilder bw = new StringBuilder(1024);
        bw.append("<doc>");
        BufferedImage image = ImageIO.read(new URL(photourl));
        if (FlickrSolrIndexingTool.saveDownloadedImages) {
            String filepath = saveFile(image);
            bw.append("<field name=\"localimagefile\">" + filepath + "</field>");
        }
        bw.append("<field name=\"id\">" + url + "</field>");
        if (title.length() > 0)
            bw.append("<field name=\"title\">" + title.replaceAll("&(?!amp;)", "&amp;").replaceAll("<", "&lt;") + "</field>");
        else
            bw.append("<field name=\"title\">" + url + "</field>");
        bw.append("<field name=\"imgurl\">" + photourl + "</field>");
        bw.append("<field name=\"authorid_s\">" + authorID + "</field>");
        bw.append("<field name=\"authorname_ws\">" + authorName.replaceAll("&(?!amp;)", "&amp;").replaceAll("<", "&lt;") + "</field>");
        if (tags.size() > 0)
            bw.append("<field name=\"tags_ws\">" + String.join(" ", tags).replaceAll("&(?!amp;)", "&amp;").replaceAll("<", "&lt;") + "</field>");
        writeFeature(image, new PHOG(), "ph", bw);
        writeFeature(image, new CEDD(), "ce", bw);
        writeFeature(image, new JCD(), "jc", bw);
        writeFeature(image, new OpponentHistogram(), "oh", bw);
        writeFeature(image, new ColorLayout(), "cl", bw);
        writeFeature(image, new ScalableColor(), "sc", bw);
        bw.append("</doc>\n");
        xml = bw.toString();
    }

    private String saveFile(BufferedImage image) throws IOException {
        File imageFile = File.createTempFile("flickrdownloader", ".jpg");
        ImageIO.write(image, "JPG", imageFile);
        return imageFile.getAbsolutePath();
    }

    private void writeFeature(BufferedImage img, GlobalFeature globalFeature, String fieldName, StringBuilder bufferedWriter) throws IOException {
        globalFeature.extract(img);
        bufferedWriter.append("<field name=\"" + fieldName + "_hi\">");
        bufferedWriter.append(Base64.getEncoder().encodeToString(globalFeature.getByteArrayRepresentation()));
        bufferedWriter.append("</field>");
        bufferedWriter.append("<field name=\"" + fieldName + "_ha\">");
        bufferedWriter.append(Utilities.hashesArrayToString(BitSampling.generateHashes(globalFeature.getFeatureVector())));
        bufferedWriter.append("</field>");
    }



    public String getXml() {
        return xml;
    }

    @Override
    public void run() {
        try {
            generateSolrDocument();
        } catch (Exception e) {
            e.printStackTrace();
            xml = null;
        }
    }
}
