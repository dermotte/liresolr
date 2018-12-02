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
}
