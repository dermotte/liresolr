package net.semanticmetadata.lire.solr.indexing;

import net.semanticmetadata.lire.solr.HashingMetricSpacesManager;
import net.semanticmetadata.lire.solr.tools.EncodeAndHashCSV;
import net.semanticmetadata.lire.solr.tools.FileListFromSolrXML;
import net.semanticmetadata.lire.solr.tools.XmlMerge;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

/**
 * This class takes a CSV file with deep features for image files (one per line, path to image in the first col, feature
 * names in the first row) and creates an XML file that can be imported to Solr supporting LireSolr extensions. It calls
 * different utiliy classes in this order: {@link net.semanticmetadata.lire.solr.tools.EncodeAndHashCSV},
 * {@link net.semanticmetadata.lire.solr.tools.FileListFromSolrXML}, {@link ParallelSolrIndexer}, and
 * {@link net.semanticmetadata.lire.solr.tools.XmlMerge}. As the very last part of the process is done in memory, the CSV
 * file should not get too big (10 k images work just fine on a laptop computer).
 */
public class ImportFromCSV implements Runnable {
    File csvFile, outFile;

    public ImportFromCSV(File csvFile, File outFile) {
        this.csvFile = csvFile;
        this.outFile = outFile;
    }

    public static void main(String[] args) throws ParseException {
        HashingMetricSpacesManager.init();
        File infile = null, outfile = null;
        Options options = new Options();
        options.addOption("i", "input-file", true, "CSV File to import (required).");
        options.addOption("o", "output-file", true, "XML File to export, will not be overwritten.");
        options.getOption("i").setRequired(true);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);
        if (cmd.hasOption('i')) {
            infile = new File(cmd.getOptionValue('i'));
            if (!infile.exists()) {
                System.err.println(String.format("File %s does not exist.", cmd.getOptionValue('i')));
                printHelp(options);
                System.exit(1);
            }
        }
        if (infile != null) {
            // check for the output file:
            if (cmd.hasOption('o')) {
                outfile = new File(cmd.getOptionValue('o'));
            } else {
                outfile = new File(cmd.getOptionValue('i').replace(".xml", "-final.xml"));
            }
            // check if the output file exists
            if (outfile.exists()) {
                System.err.println(String.format("File %s already exists and will not be overwritten.", cmd.getOptionValue('o')));
                printHelp(options);
                System.exit(1);
            }
        } else {
            printHelp(options);
            System.exit(1);
        }
        ImportFromCSV i = new ImportFromCSV(infile, outfile);
        i.run();

    }

    private static void printHelp(Options options) {
        HelpFormatter f = new HelpFormatter();
        f.printHelp(String.format("%s", ImportFromCSV.class.getName()), options);
    }


    @Override
    public void run() {
        try {
            // create temporary files.
            File deepXML = File.createTempFile("encoded-and-hashed-", ".xml");
            File fileList = File.createTempFile("file-list-", ".lst");
            File lireXML = File.createTempFile("lire-", ".xml");

            // do the tricks.
            System.out.println("## Converting CSV to XML by encoding the features and hashing them.");
            EncodeAndHashCSV enc = new EncodeAndHashCSV(csvFile, deepXML);
            enc.run();
            System.out.println("## Creating a file list for ParallelSolrIndexer.");
            FileListFromSolrXML fl = new FileListFromSolrXML(deepXML, fileList);
            fl.run();
            System.out.println("## RunningParallelSolrIndexer.");
            ParallelSolrIndexer p = new ParallelSolrIndexer();
            p.setFileList(fileList);
            p.setOutFile(lireXML);
            p.run();
            System.out.println("## Merging the created data.");
            XmlMerge xm = new XmlMerge(deepXML, lireXML, outFile);
            xm.run();

            // delete the temp file ...
            System.out.println("## Cleaning up temporary files.");
            lireXML.deleteOnExit();
            deepXML.deleteOnExit();
            fileList.deleteOnExit();
            System.out.println("## Finished.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
