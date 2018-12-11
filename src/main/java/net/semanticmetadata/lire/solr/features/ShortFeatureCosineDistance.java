package net.semanticmetadata.lire.solr.features;

import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalDoubleFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalShortFeature;
import net.semanticmetadata.lire.utils.MetricsUtils;
import net.semanticmetadata.lire.utils.SerializationUtils;
import org.apache.lucene.util.BytesRef;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * GenericGlobalShortFeature but with cosine coefficient based distance function and specific coding of byte
 * representation for sparse vectors.
 */
public class ShortFeatureCosineDistance extends GenericGlobalShortFeature {
    @Override
    public double getDistance(LireFeature feature) {
        return MetricsUtils.cosineDistance(getFeatureVector(), feature.getFeatureVector());
    }

    @Override
    public byte[] getByteArrayRepresentation() {
        int numDimensions = 0;
        LinkedList<Short> d = new LinkedList<>();
        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            short datum = data[i];
            if (datum != 0) {
                d.add((short) i);
                d.add(datum);
                numDimensions++;
            }
        }
        d.addFirst((short) data.length);
        numDimensions = 0; // re-using a variable.
        short[] s = new short[d.size()];
        for (Iterator<Short> iterator = d.iterator(); iterator.hasNext(); ) {
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
        short[] s = SerializationUtils.toShortArray(featureData, offset, length);
        data = new short[s[0]];
        for (int i = 1; i < s.length; i+=2) {
            data[s[i]] = s[i+1];
        }
    }
}
