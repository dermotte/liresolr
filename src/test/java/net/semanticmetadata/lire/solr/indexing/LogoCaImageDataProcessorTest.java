package net.semanticmetadata.lire.solr.indexing;

import junit.framework.TestCase;

/**
 * Created by mlux on 08.12.2016.
 */
public class LogoCaImageDataProcessorTest extends TestCase {
    public void testGetTitle() {
        AbstractImageDataProcessor ip = new LogoCaImageDataProcessor();
        ip.setImageData("c:\\temp\\test\\converted-1\\myimage.png");
        assertEquals(ip.getTitle(), "1/myimage.png");
        ip.setImageData("c:\\temp\\test\\converted-12\\myimage.png");
        assertEquals(ip.getTitle(), "12/myimage.png");
        ip.setImageData("/detrt/converted-39\\myimage.png");
        assertEquals(ip.getTitle(), "39/myimage.png");
        ip.setImageData("c:\\temp\\test\\converted-0\\myimage.png");
        assertEquals(ip.getTitle(), "0/myimage.png");
    }

    public void testGetId() {
        AbstractImageDataProcessor ip = new LogoCaImageDataProcessor();
        ip.setImageData("c:\\temp\\test\\converted-1\\myimage.png");
        assertEquals(ip.getIdentifier(), "1/myimage.png");
        ip.setImageData("c:\\temp\\test\\converted-12\\myimage.png");
        assertEquals(ip.getIdentifier(), "12/myimage.png");
        ip.setImageData("/converted-39\\myimage.png");
        assertEquals(ip.getIdentifier(), "39/myimage.png");
        ip.setImageData("d:\\temp\\test\\converted-0\\myimage.png");
        assertEquals(ip.getIdentifier(), "0/myimage.png");
    }
}
