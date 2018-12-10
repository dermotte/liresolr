package net.semanticmetadata.lire.solr.features;

import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalDoubleFeature;
import net.semanticmetadata.lire.utils.MetricsUtils;

import java.util.Arrays;

/**
 * GenericGlobalDoubleFeature but with cosine coefficient based distance function.
 */
public class DoubleFeatureCosineDistance extends GenericGlobalDoubleFeature {
    @Override
    public double getDistance(LireFeature feature) {
        return MetricsUtils.cosineDistance(getFeatureVector(), feature.getFeatureVector());
    }
}
