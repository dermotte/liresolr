package net.semanticmetadata.lire.solr.tools;

import org.apache.commons.cli.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.Iterator;

public class FileListFromSolrXML implements Runnable {
    File infile, outfile;
    PrintStream outStream = System.out;

    public FileListFromSolrXML(File infile) {
        this.infile = infile;
    }

    public FileListFromSolrXML(File infile, File outfile) {
        this.infile = infile;
        this.outfile = outfile;
        try {
            outStream = new PrintStream(new FileOutputStream(outfile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws ParseException {
        File infile = null, outfile = null;

        Options options = new Options();
        options.addOption("i", "input-file", true, "CSV File to import (required)");
        options.addOption("o", "output-file", true, "XML File to export, will not be overwritten");
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
                outfile = new File(cmd.getOptionValue('i').replace(".xml", ".lst"));
            }
            // check if the output file exists
            if (outfile.exists()) {
                infile = null;  // go to error state.
                System.err.println(String.format("File %s already exists and will not be overwritten.", cmd.getOptionValue('o')));
            }
        }

        // check if valid. If so go on, otherwise print help:
        if (infile != null) {
            FileListFromSolrXML e;
            if (outfile == null) e = new FileListFromSolrXML(infile);
            else e = new FileListFromSolrXML(infile, outfile);
            e.run();
        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("FileListFromSolrXML", options);
        }
    }

    @Override
    public void run() {
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(new FileInputStream(infile));
            Element root = document.getRootElement();
            for (Iterator<Element> it = root.elementIterator(); it.hasNext(); ) {
                Element doc = it.next();
                String file_path = doc.selectSingleNode("field[@name='localimagefile']").getText();
                outStream.println(file_path);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        outStream.close();
    }
}
