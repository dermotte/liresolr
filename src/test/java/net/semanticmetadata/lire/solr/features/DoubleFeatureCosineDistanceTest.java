package net.semanticmetadata.lire.solr.features;

import org.junit.Test;

import static org.junit.Assert.*;

public class DoubleFeatureCosineDistanceTest {

    @Test
    public void getByteArrayRepresentation() {
        double[] d = new double[1400];
        for (int i = 0; i < 32; i++) {
            d[((int) Math.floor(Math.random() * d.length))] = ((Math.random() * Short.MIN_VALUE * 2) + Short.MIN_VALUE);
        }
        DoubleFeatureCosineDistance sd = new DoubleFeatureCosineDistance();
        sd.setData(d);
        byte[] byteArrayRepresentation = sd.getByteArrayRepresentation();
        System.out.println(byteArrayRepresentation.length);
        DoubleFeatureCosineDistance se = new DoubleFeatureCosineDistance();
        se.setByteArrayRepresentation(byteArrayRepresentation);
        assertArrayEquals(se.getFeatureVector(), sd.getFeatureVector(), 0.001);
    }
}