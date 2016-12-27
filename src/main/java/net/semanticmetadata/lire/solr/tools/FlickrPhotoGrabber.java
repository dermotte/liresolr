package net.semanticmetadata.lire.solr.tools;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple tool for grabbing data from the Flickr public feed to index something in the LireSolr search service. Use the
 * command line tool {@link FlickrSolrIndexingTool} to create an XML file that can be uploaded to Solr.
 *
 * @author Mathias Lux, mathias@juggle.at, Dec 2016
 */
public class FlickrPhotoGrabber extends DefaultHandler {
    public static final String BASE_URL = "http://api.flickr.com/services/feeds/photos_public.gne?format=atom";

    // for sax parsing
    private LinkedList<FlickrPhoto> photos = new LinkedList<FlickrPhoto>();
    private boolean inEntry = false;
    private boolean inTitle = false;
    private boolean inAuthor = false;
    private boolean inAuthorName = false;
    private boolean inAuthorUri = false;
    private String currentTitle = "", currentUrl = null, currentImage = null, currentAuthor = "", currentAuthorID = "";
    private LinkedList<String> currentTags = new LinkedList<String>();

    public static List<FlickrPhoto> getRecentPhotos() throws IOException, SAXException, ParserConfigurationException {
        LinkedList<FlickrPhoto> photos = new LinkedList<FlickrPhoto>();
        URL u = new URL(BASE_URL);
        FlickrPhotoGrabber handler = new FlickrPhotoGrabber();
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(u.openStream(), handler);
        return handler.photos;
    }

    public static List<FlickrPhoto> getPhotosWithTags(String tags) throws IOException, SAXException, ParserConfigurationException {
        LinkedList<FlickrPhoto> photos = new LinkedList<FlickrPhoto>();
        URL u = new URL(BASE_URL + "&tags=" + tags);
        FlickrPhotoGrabber handler = new FlickrPhotoGrabber();
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(u.openStream(), handler);
        return handler.photos;
    }

    public static List<FlickrPhoto> getPhotosFromUser(String userID) throws IOException, SAXException, ParserConfigurationException {
        LinkedList<FlickrPhoto> photos = new LinkedList<FlickrPhoto>();
        URL u = new URL(BASE_URL + "&id=" + userID);
        FlickrPhotoGrabber handler = new FlickrPhotoGrabber();
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        saxParser.parse(u.openStream(), handler);
        return handler.photos;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("entry")) inEntry = true;
        if (inEntry) {
            if (qName.equals("author")) inAuthor = true;
            else if (qName.equals("title")) inTitle = true;
            else if (qName.equals("link")) {
                if (attributes.getValue("rel").equals("alternate")) currentUrl = attributes.getValue("href");
                if (attributes.getValue("rel").equals("enclosure")) currentImage = attributes.getValue("href");
            }
            if (qName.equals("name") && inAuthor) inAuthorName = true;
            if (qName.equals("uri") && inAuthor) inAuthorUri = true;
        }
        if (qName.equals("category")) currentTags.add(attributes.getValue("term"));
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("entry")) {
            inEntry = false;
            // add entry to list:
            if (currentAuthorID.length() > 0)
                photos.add(new FlickrPhoto(currentTitle.trim(), currentUrl.trim(), currentImage.trim(), currentTags, currentAuthor, currentAuthorID));
            else
                photos.add(new FlickrPhoto(currentTitle.trim(), currentUrl.trim(), currentImage.trim(), currentTags));
            // clear:
            currentImage = null;
            currentTitle = "";
            currentUrl = null;
            currentAuthor = "";
            currentAuthorID = "";
            currentTags.clear();
        }
        if (qName.equals("title")) inTitle = false;
        else if (qName.equals("author")) inAuthor = false;
        if (qName.equals("name") && inAuthor) inAuthorName = false;
        if (qName.equals("uri") && inAuthor) inAuthorUri = false;

    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (inTitle) currentTitle += new String(ch, start, length);
        if (inAuthorUri) currentAuthorID += new String(ch, start, length);
        if (inAuthorName) currentAuthor += new String(ch, start, length);
    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        final List<FlickrPhoto> flickrPhotos = FlickrPhotoGrabber.getRecentPhotos();
        for (Iterator<FlickrPhoto> flickrPhotoIterator = flickrPhotos.iterator(); flickrPhotoIterator.hasNext(); ) {
            FlickrPhoto flickrPhoto = flickrPhotoIterator.next();
            System.out.println("flickrPhoto = " + flickrPhoto);
        }
    }


}
