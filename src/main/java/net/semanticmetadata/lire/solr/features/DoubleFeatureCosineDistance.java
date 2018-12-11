package net.semanticmetadata.lire.solr.features;

import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalDoubleFeature;
import net.semanticmetadata.lire.utils.MetricsUtils;
import net.semanticmetadata.lire.utils.SerializationUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * GenericGlobalDoubleFeature but with cosine coefficient based distance function.
 */
public class DoubleFeatureCosineDistance extends GenericGlobalDoubleFeature {
    @Override
    public double getDistance(LireFeature feature) {
        return MetricsUtils.cosineDistance(getFeatureVector(), feature.getFeatureVector());
    }

    @Override
    public byte[] getByteArrayRepresentation() {
        int numDimensions = 0;
        LinkedList<Double> d = new LinkedList<>();
        double[] data = getFeatureVector();
        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            double datum = data[i];
            if (datum != 0) {
                d.add((double) i);
                d.add(datum);
                numDimensions++;
            }
        }
        d.addFirst((double) data.length);
        numDimensions = 0; // re-using a variable.
        double[] s = new double[d.size()];
        for (Iterator<Double> iterator = d.iterator(); iterator.hasNext(); ) {
            s[numDimensions++] = iterator.next();
        }
        return SerializationUtils.toByteArray(s);
    }

    @Override
    public void setByteArrayRepresentation(byte[] featureData) {
        setByteArrayRepresentation(featureData, 0, featureData.length);
    }

    @Override
    public void setByteArrayRepresentation(byte[] featureData, int offset, int length) {
        double[] s = SerializationUtils.toDoubleArray(featureData, offset, length);
        double[] data = new double[(int) s[0]];
        for (int i = 1; i < s.length; i+=2) {
            data[(int) s[i]] = s[i+1];
        }
        setData(data);
    }
}
