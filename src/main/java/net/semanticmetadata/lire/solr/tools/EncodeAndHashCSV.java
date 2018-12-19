package net.semanticmetadata.lire.solr.tools;

import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.solr.HashingMetricSpacesManager;
import net.semanticmetadata.lire.solr.features.ShortFeatureCosineDistance;
import org.apache.commons.cli.*;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.*;
import java.util.*;

/**
 * Command line utility that takes a csv file with the file name in the first col and converts
 * it to an XML file Solr can handle. We assume that the rest of the columns is a feature vector, i.e. from a CNN.
 */
public class EncodeAndHashCSV implements Runnable {
    public static final int TOP_N_CLASSES = 32;
    public static final int TOP_N_CLASSES_FOR_QUERY = 5;
    public static final double TOP_CLASSES_FACTOR = 10d;
    public static final double THRESHOLD_RELATIVE_SIGNIFICANCE_TO_MAXIMUM = 0.8;

    File infile, outfile;

    public EncodeAndHashCSV(File infile, File outfile) {
        this.infile = infile;
        this.outfile = outfile;
    }

    public static void main(String[] args) throws ParseException {
        HashingMetricSpacesManager.init();
        File infile = null, outfile = null;

        // Using Apache Commons CLI for parsing the command line options.
        Options options = new Options();
        options.addOption("i", "input-file", true, "CSV File to import (required)");
        options.addOption("o", "output-file", true, "XML File to export, will not be overwritten");
        options.addOption("t", "top-n-classes", true, "The number of top classes used for indexing");
        options.addOption("m", "maximum-value", true, "The maximum feature value used for normalization");
        options.getOption("i").setRequired(true);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption('i')) {
            infile = new File(cmd.getOptionValue('i'));
            if (!infile.exists()) {
                System.err.println(String.format("File %s does not exist.", cmd.getOptionValue('i')));
                infile = null;
            }
        }
        if (infile != null) {
            // check for the output file:
            if (cmd.hasOption('o')) {
                outfile = new File(cmd.getOptionValue('o'));
            } else {
                outfile = new File(cmd.getOptionValue('i').replace(".csv", ".xml"));
            }
            // check if the output file exists
            if (outfile.exists()) {
                infile = null;  // go to error state.
                System.err.println(String.format("File %s already exists and will not be overwritten.", cmd.getOptionValue('o')));
            }
        }
        // check if valid. If so go on, otherwise print help:
        if (infile != null) {
            EncodeAndHashCSV e = new EncodeAndHashCSV(infile, outfile);
            e.run();
        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("EncodeAndHashCSV", options);
        }
    }

    @Override
    public void run() {
        // read infile line by line
        String[] classes;
        try {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("add");
            BufferedReader br = new BufferedReader(new FileReader(infile));
            // we assume that the first line is the name of the classes and the first col is the file name:
            String line = br.readLine();
            String[] tmp_array = line.split(",");
            classes = new String[tmp_array.length - 1];
            System.arraycopy(tmp_array, 1, classes, 0, classes.length);
            int line_count = 0;
            while ((line = br.readLine()) != null) {
                tmp_array = line.split(",");
                Element doc = root.addElement("doc");

                Element field_id = doc.addElement("field");
                field_id.addAttribute("name", "id");
                field_id.addText(tmp_array[0]);

                Element field_file = doc.addElement("field");
                field_file.addAttribute("name", "localimagefile");
                field_file.addText(tmp_array[0]);

                // converting the data to double:
                double[] feature = new double[classes.length];
                for (int i = 0; i < feature.length; i++) {
                    feature[i] = Double.parseDouble(tmp_array[i + 1]);
                }

                // now create the feature vector ...
                addFeatureVector(feature, classes, doc);

                // now create the fields for the classes ...
                addClassesString(feature, classes, doc);

                line_count++;
                if (line_count % 2000 == 0) {
                    System.out.println(String.format("# %d images encoded and hashed", line_count));
                }
            }
            // write the result
            FileWriter out = new FileWriter(outfile);
            document.write(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a string of classes based on the dimensions names (in the first row) and
     * puts in the class names n times depending on their weight and the TOP_CLASSES_FACTOR
     * value. Additionally it creates a text field, where a proposed query is given.
     *
     * @param feature
     * @param classes
     * @param doc
     */
    private void addClassesString(double[] feature, String[] classes, Element doc) {
        // sort the feature vector and get the 32 most important classes out
        Map<String, Double> hm = new LinkedHashMap<>();
        StringBuilder field_classes_ws_text = new StringBuilder(1024);
        StringBuilder field_query_s_text = new StringBuilder(1024);
        StringBuilder field_query_boosted_text = new StringBuilder(1024);
        StringBuilder field_classes_significant_text = new StringBuilder(1024);
        for (int i = 0; i < feature.length; i++) {
            hm.put(classes[i], feature[i]);
        }
        hm = Utilities.sortByValue(hm);
        int i = 0;
        for (Iterator<String> iterator = hm.keySet().iterator(); iterator.hasNext() && i < TOP_N_CLASSES; i++) {
            String cl = iterator.next();
            // decide how often the class is int he field per weight, here it's just the rounded weight.
            double we = Math.max((Math.round(hm.get(cl)) * TOP_CLASSES_FACTOR), 0);
            for (int j = 0; j < we; j++) {
                field_classes_ws_text.append(cl);
                field_classes_ws_text.append(' ');
            }
        }
        i = 0;
        for (Iterator<String> iterator = hm.keySet().iterator(); iterator.hasNext() && i < TOP_N_CLASSES_FOR_QUERY; i++) {
            String cl = iterator.next();
            field_query_s_text.append("classes_ws:");
            field_query_s_text.append(cl);
            field_query_s_text.append(' ');

            field_query_boosted_text.append("classes_ws:");
            field_query_boosted_text.append(cl);
            field_query_boosted_text.append('^');
            field_query_boosted_text.append((int) Math.round(hm.get(cl)));
            field_query_boosted_text.append(' ');
        }
        // find the classes with a weight > t times the maximum weight with t being initially 0.8
        i = 0; // init count to zero
        String firstClass = hm.keySet().iterator().next(); // get the first class
        double minimumWeight = hm.get(firstClass) * THRESHOLD_RELATIVE_SIGNIFICANCE_TO_MAXIMUM; // set the threshold
        double currentWeight = minimumWeight; // first element is highes one, so set it right now.
        Iterator<String> iterator = hm.keySet().iterator();
        do {
            String cl = iterator.next();
            currentWeight = hm.get(cl);
            if (currentWeight > minimumWeight) {
                field_classes_significant_text.append(cl);
                field_classes_significant_text.append(' ');
                i++;
            }
        } while (iterator.hasNext() && currentWeight > minimumWeight); // do this while there are still elements and the weight is above the threshold.
        Element field_classes_ws = doc.addElement("field");
        field_classes_ws.addAttribute("name", "classes_ws");
        field_classes_ws.addText(field_classes_ws_text.toString().trim());

        Element field_query_s = doc.addElement("field");
        field_query_s.addAttribute("name", "query_s");
        field_query_s.addText(field_query_s_text.toString().trim());

        Element field_query_b = doc.addElement("field");
        field_query_b.addAttribute("name", "query_boosted_s");
        field_query_b.addText(field_query_boosted_text.toString().trim());

        Element field_classes_significant_ws = doc.addElement("field");
        field_classes_significant_ws.addAttribute("name", "classes_significant_ws");
        field_classes_significant_ws.addText(field_classes_significant_text.toString().trim());
    }

    private void addFeatureVector(double[] feature, String[] classes, Element doc) {
        Element field_file;
        int[] hashes;
        ShortFeatureCosineDistance f1 = new ShortFeatureCosineDistance();
        // DoubleFeatureCosineDistance f2 = new DoubleFeatureCosineDistance();

        double[] tmpFeature = Utilities.toCutOffArray(feature, TOP_N_CLASSES);

        // double feature:
//        f2.setData(tmpFeature);
//        int[] hashes = BitSampling.generateHashes(f2.getFeatureVector());
//        Element field_file = doc.addElement("field");
//        field_file.addAttribute("name", "df_hi");
//        field_file.addText(Base64.getEncoder().encodeToString(f2.getByteArrayRepresentation()));

//        field_file = doc.addElement("field");
//        field_file.addAttribute("name", "df_ha");
//        field_file.addText(Utilities.hashesArrayToString(hashes));

        // short feature ...
//        feature = Utilities.normalize(feature);
//        f1.setData(Utilities.quantizeToShort(feature));
        f1.setData(Utilities.toShortArray(tmpFeature));
        hashes = BitSampling.generateHashes(f1.getFeatureVector());

        field_file = doc.addElement("field");
        field_file.addAttribute("name", "sf_hi");
        field_file.addText(Base64.getEncoder().encodeToString(f1.getByteArrayRepresentation()));

        field_file = doc.addElement("field");
        field_file.addAttribute("name", "sf_ha");
        field_file.addText(Utilities.hashesArrayToString(hashes));
    }

}
/*
This help text is shown if you start the EncodeAndHashCSV with the '-h' option.

$> EncodeAndHashCSV -i <infile>

The <infile> is mandatory.
 */
