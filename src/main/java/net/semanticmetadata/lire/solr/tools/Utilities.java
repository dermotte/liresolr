package net.semanticmetadata.lire.solr.tools;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;


public class Utilities {
    public static String hashesArrayToString(int[] array) {
        StringBuilder sb = new StringBuilder(array.length * 8);
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(Integer.toHexString(array[i]));
        }
        return sb.toString();
    }

    /**
     * Sorts a map by value ... from https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort((Comparator<Entry<K, V>> & Serializable)
                (c1, c2) -> -c1.getValue().compareTo(c2.getValue()));

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Does a max normalization of the input vector.
     * @param featureVector the input double values, is left untouched.
     * @return new object with normalized values.
     */
    public static double[] normalize(double[] featureVector) {
        double[] result = new double[featureVector.length];
        double max = Arrays.stream(featureVector).reduce(Double::max).getAsDouble();
        double min = Arrays.stream(featureVector).reduce(Double::min).getAsDouble();
        for (int i = 0; i < featureVector.length; i++) {
            result[i] = (featureVector[i] - min)/ (max-min);

        }
        return result;
    }

    /**
     * Quantizes a normalized input vector to the maximum range of short.
     * @param featureVector the normalized input.
     * @return the short[] result, ideally quantized over the whole number space of short.
     */
    public static short[] quantizeToShort(double[] featureVector) {
        short[] result = new short[featureVector.length];
        for (int i = 0; i < featureVector.length; i++) {
            double d = featureVector[i] * Short.MAX_VALUE * 2 + Short.MIN_VALUE;
            assert (d <= Short.MAX_VALUE && d >= Short.MIN_VALUE);
            result[i] = (short) (d);
        }
        return result;
    }

    /**
     * Cuts off after the first numDimensions .. the rest is set to zero.
     * @param feature
     * @return
     */
    public static double[] toCutOffArray(double[] feature, int numDimensions) {
        double[] clone = feature.clone();
        Arrays.sort(clone);
        double cutOff = clone[clone.length-numDimensions];
        for (int i = 0; i < feature.length; i++) {
            clone[i] = (feature[i]>cutOff)?feature[i]:0d;
        }
        return clone;
    }

    public static short[] toShortArray(double[] feature) {
        short[] arr = new short[feature.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (short) (feature[i]*100d);
        }
        return arr;
    }
}
