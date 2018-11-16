package net.semanticmetadata.lire.solr.tools;

import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalByteFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalDoubleFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalShortFeature;
import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.solr.HashingMetricSpacesManager;
import net.semanticmetadata.lire.utils.CommandLineUtils;

import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

/**
 * Simple class to call from Python to create a Base64 encoded feature representation
 * compatible with LireSolr from a list of floats / ints
 */
public class DataConversion {
    static String helpMessage = "$> DataConversion [-t <double|float|int|short>] -d <data>\n\n" +
            "Options\n" +
            "=======\n" +
            "-t \t type to be expected, using double as a default.\n" +
            "-d \t actual data in the for of \"0.55;0.33;...\"";

    public static void main(String[] args) throws IOException {
        // TODO: integrate Bitsampling ...
        HashingMetricSpacesManager.init();
        // parse arguments
        Properties p = CommandLineUtils.getProperties(args, helpMessage, new String[]{"-d"});
        String type = "double";
        if (p.get("-t") != null)
            type = p.getProperty("-t");
        // convert data
        GlobalFeature f = null;
        String my_data = p.getProperty("-d");
        if (my_data.startsWith("x")) my_data = my_data.substring(1);
        if (type.startsWith("double"))
            f = getDouble(my_data);
        else if (type.startsWith("short"))
            f = getShort(my_data);
        else if (type.startsWith("byte"))
            f = getShort(my_data);

        // return result
        int[] hashes = BitSampling.generateHashes(f.getFeatureVector());
        System.out.print(Base64.getEncoder().encodeToString(f.getByteArrayRepresentation()) + "|" + Utilities.hashesArrayToString(hashes));
    }

    private static GlobalFeature getDouble(String data) {
        GenericGlobalDoubleFeature f = new GenericGlobalDoubleFeature();
        String[] numbers = data.split(";");
        double[] d = new double[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            d[i] = Double.parseDouble(numbers[i]);
        }
        f.setData(d);
        return f;
    }

    private static GlobalFeature getShort(String data) {
        GenericGlobalShortFeature f = new GenericGlobalShortFeature();
        String[] numbers = data.split(";");
        short[] d = new short[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            d[i] = Short.parseShort(numbers[i]);
        }
        f.setData(d);
        return f;
//        int[] hashes = BitSampling.generateHashes(f.getFeatureVector());
//        return f.getByteArrayRepresentation();
    }

    private static GlobalFeature getByte(String data) {
        GenericGlobalByteFeature f = new GenericGlobalByteFeature();
        String[] numbers = data.split(";");
        byte[] d = new byte[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            d[i] = Byte.parseByte(numbers[i]);
        }
        f.setData(d);
        return f;
//        int[] hashes = BitSampling.generateHashes(f.getFeatureVector());
//        return f.getByteArrayRepresentation();
    }

}
