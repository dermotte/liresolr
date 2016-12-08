package net.semanticmetadata.lire.solr.indexing;

import junit.framework.TestCase;

/**
 * Created by mlux on 08.12.2016.
 */
public class LogoCaImageDataProcessorTest extends TestCase {
    public void testGetTitle() {
        LogoCaImageDataProcessor ip = new LogoCaImageDataProcessor();
        assertEquals(ip.getTitle("c:\\temp\\test\\converted-1\\myimage.png"), "1/myimage.png");
        assertEquals(ip.getTitle("c:\\temp\\test\\converted-12\\myimage.png"), "12/myimage.png");
        assertEquals(ip.getTitle("/detrt/converted-39\\myimage.png"), "39/myimage.png");
        assertEquals(ip.getTitle("c:\\temp\\test\\converted-0\\myimage.png"), "0/myimage.png");
    }

    public void testGetId() {
        LogoCaImageDataProcessor ip = new LogoCaImageDataProcessor();
        assertEquals(ip.getIdentifier("c:\\temp\\test\\converted-1\\myimage.png"), "1/myimage.png");
        assertEquals(ip.getIdentifier("c:\\temp\\test\\converted-12\\myimage.png"), "12/myimage.png");
        assertEquals(ip.getIdentifier("/converted-39\\myimage.png"), "39/myimage.png");
        assertEquals(ip.getIdentifier("d:\\temp\\test\\converted-0\\myimage.png"), "0/myimage.png");
    }
}
