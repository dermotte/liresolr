package net.semanticmetadata.lire.solr.tools;

import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.solr.HashingMetricSpacesManager;
import net.semanticmetadata.lire.utils.CommandLineUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple tool for grabbing data from the Flickr public feed to index something in the LireSolr search service. Used to
 * to create an XML file that can be uploaded to Solr.
 *
 * @author Mathias Lux, mathias@juggle.at, Dec 2016
 */
public class FlickrSolrIndexingTool {
    static String helpMessage = "$> FlickrSolrIndexingTool -o <outfile.xml|auto> [-n <number_of_photos>] [-s]\n\n" +
            "Options\n" +
            "=======\n" +
            "-s \t store the images locally as temp files.";
    private static int numThreads = 8;
    protected static boolean saveDownloadedImages = false;

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
        HashingMetricSpacesManager.init();
        int numberOfImages = 20;

        Properties p = CommandLineUtils.getProperties(args, helpMessage, new String[]{"-o"});
        if (p.get("-n") != null) {
            try {
                numberOfImages = Integer.parseInt(p.getProperty("-n"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        if (p.get("-s") != null) {
            FlickrSolrIndexingTool.saveDownloadedImages = true;
        }

        LinkedList<FlickrPhoto> images = new LinkedList<>();
        HashSet<String> titles = new HashSet<String>(numberOfImages);
        while (images.size() < numberOfImages) {
            List<FlickrPhoto> photos = FlickrPhotoGrabber.getRecentPhotos();
            for (FlickrPhoto photo : photos) {
                // check if it is already there:
                if (!titles.contains(photo.url)) {
                    titles.add(photo.url);
                    System.out.println("photo.url = " + photo.url);
                    if (images.size() < numberOfImages) {
                        images.add(photo);
                    } else break;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        BufferedWriter bw;
        if (p.getProperty("-o").equals("auto"))
            bw = getNextFile();
        else
            bw = new BufferedWriter(new FileWriter(p.getProperty("-o")));
        System.out.println("Downloading and analyzing photos");
        bw.write("<add>\n");
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        System.out.println("Setting up thread pool.");
        for (FlickrPhoto photo : images) {
            executorService.execute(photo);
            // System.out.print(',');
        }
        System.out.println("Waiting for thread pool to be finished.");
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(250);
        }
        for (FlickrPhoto photo : images) {
            if (photo.getXml() != null) bw.write(photo.getXml());
            System.out.print('.');
        }
        System.out.println("\nFinished.");
        bw.write("</add>\n");
        bw.close();
    }

    private static BufferedWriter getNextFile() throws IOException {
        String pattern = "flickrdownload-%05d.xml";
        int num = 0;
        while (new File(String.format(pattern, num)).exists())
            num++;
        return new BufferedWriter(new FileWriter(String.format(pattern, num)));
    }


}
