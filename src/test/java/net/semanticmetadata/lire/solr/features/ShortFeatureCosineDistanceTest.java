package net.semanticmetadata.lire.solr.features;

import org.junit.Test;

import static org.junit.Assert.*;

public class ShortFeatureCosineDistanceTest {

    @Test
    public void getByteArrayRepresentation() {
        short[] d = new short[1400];
        for (int i = 0; i < 64; i++) {
            d[((int) Math.floor(Math.random() * d.length))] = (short) ((Math.random() * Short.MIN_VALUE * 2) + Short.MIN_VALUE);
        }
        ShortFeatureCosineDistance sd = new ShortFeatureCosineDistance();
        sd.setData(d);
        byte[] byteArrayRepresentation = sd.getByteArrayRepresentation();
        System.out.println(byteArrayRepresentation.length);
        ShortFeatureCosineDistance se = new ShortFeatureCosineDistance();
        se.setByteArrayRepresentation(byteArrayRepresentation);
        assertArrayEquals(se.getFeatureVector(), sd.getFeatureVector(), 0.001);
    }
}