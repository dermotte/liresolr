package net.semanticmetadata.lire.solr.tools;

import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalShortFeature;
import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.solr.HashingMetricSpacesManager;
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
    private static int TOP_N_CLASSES = 32;
    private static double MAXIMUM_FEATURE_VALUE = 64d;
    private static double TOP_CLASSES_FACTOR = 1d;
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
        CommandLine cmd = parser.parse( options, args);
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
            classes = new String[tmp_array.length-1];
            System.arraycopy(tmp_array, 1, classes, 0, classes.length);
            int line_count = 0;
            while ((line = br.readLine())!=null) {
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
                    feature[i] = Double.parseDouble(tmp_array[i+1]);
                }

                // now create the feature vector ...
                addFeatureVector(feature, classes, doc);

                // now create the fields for the classes ...
                addClassesString(feature, classes, doc);

                line_count++;
                if (line_count%2000 == 0) {
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

    private void addClassesString(double[] feature, String[] classes, Element doc) {
        // sort the feature vector and get the 32 most important classes out
        Map<String, Double> hm = new LinkedHashMap<>();
        StringBuilder field_text = new StringBuilder(1024);
        for (int i = 0; i < feature.length; i++) {
            hm.put(classes[i], feature[i]);
        }
        hm = Utilities.sortByValue(hm);
        int i = 0;
        for (Iterator<String> iterator = hm.keySet().iterator(); iterator.hasNext() && i< TOP_N_CLASSES; i++) {
            String cl = iterator.next();
            // decide how often the class is int he field per weight, here it's just the rounded weight.
            float we = (float) (Math.round(hm.get(cl)) * TOP_CLASSES_FACTOR);
            for (int j  = 0 ; j < we; j++) {
                field_text.append(cl);
                field_text.append(' ');
            }
        }
        Element field_file = doc.addElement("field");
        field_file.addAttribute("name", "classes_ws");
        field_file.addText(field_text.toString().trim());
    }

    private void addFeatureVector(double[] feature, String[] classes, Element doc) {
        GenericGlobalShortFeature f = new GenericGlobalShortFeature();
        short[] v = new short[feature.length];
        for (int i = 0; i < v.length; i++) {
            // double v1 = ((feature[i] / 64d) * Short.MAX_VALUE * 2) + Short.MIN_VALUE;
            double v1 = (feature[i] / MAXIMUM_FEATURE_VALUE) * Short.MAX_VALUE;
            assert(v1 <= Short.MAX_VALUE && v1 >= Short.MIN_VALUE);
            v[i] = (short) v1;
        }
        f.setData(v);
        int[] hashes = BitSampling.generateHashes(f.getFeatureVector());

        Element field_file = doc.addElement("field");
        field_file.addAttribute("name", "sf_hi");
        field_file.addText(Base64.getEncoder().encodeToString(f.getByteArrayRepresentation()));

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
