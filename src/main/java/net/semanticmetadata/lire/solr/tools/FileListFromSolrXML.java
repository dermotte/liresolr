package net.semanticmetadata.lire.solr.tools;

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

    public static void main(String[] args) {
        File infile = null, outfile = null;
        // check if the -i parameter is given and if so if the given file exists.
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-i") & i + 1 < args.length) {
                String infilepath = args[i + 1];
                infile = new File(infilepath);
                if (!infile.exists()) infile = null;
            }
            if (arg.startsWith("-o") & i + 1 < args.length) {
                String outfilepath = args[i + 1];
                outfile = new File(outfilepath);
            }
        }

        // check if valid. If so go on, otherwise print help:
        if (infile != null) {
            FileListFromSolrXML e;
            if (outfile == null) e = new FileListFromSolrXML(infile);
            else e = new FileListFromSolrXML(infile, outfile);
            e.run();
        } else {
            printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("This help text is shown if you start the FileListFromSolrXML with the '-h' option.\n" +
                "\n" +
                "$> FileListFromSolrXML -i <infile> -o <outfile>\n" +
                "\n" +
                "An existing <infile> is mandatory. ");
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
