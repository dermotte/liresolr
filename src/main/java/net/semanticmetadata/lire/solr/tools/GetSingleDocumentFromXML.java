package net.semanticmetadata.lire.solr.tools;

import org.apache.commons.cli.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.Iterator;

public class GetSingleDocumentFromXML implements Runnable{
    Document doc = null;
    String docID = null;

    public GetSingleDocumentFromXML(FileInputStream i, String documentId) throws DocumentException {
        doc = parse(i);
        this.docID = documentId;
    }

    public static Document parse(InputStream in) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(in);
        return document;
    }

    public static void main(String[] args) throws ParseException, FileNotFoundException, DocumentException {
        File fileIn = null;
        Options options = new Options();
        options.addOption("i", "input-file", true, "XML File to open.");
        options.addOption("d", "document-id", true, "The document to search for.");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!(cmd.hasOption('i') && cmd.hasOption('d'))) {
            HelpFormatter f = new HelpFormatter();
            f.printHelp("GetSingleDocumentFromXML", options);
            System.exit(1);
        }

        GetSingleDocumentFromXML x = new GetSingleDocumentFromXML(new FileInputStream(cmd.getOptionValue('i')), cmd.getOptionValue('d'));
        x.run();
    }

    @Override
    public void run() {
        Iterator<Element> docs = this.doc.getRootElement().elementIterator("doc");
        while (docs.hasNext()) {
            Element d = docs.next();
            String id = d.selectSingleNode("field[@name='id']").getText();
            if (id.endsWith(docID)) {
                try {
                    XMLWriter w = new XMLWriter(System.out, OutputFormat.createPrettyPrint());
                    w.write(d);
                    w.flush();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
