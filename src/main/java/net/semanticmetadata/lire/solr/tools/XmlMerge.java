package net.semanticmetadata.lire.solr.tools;

import org.apache.commons.cli.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;


public class XmlMerge implements Runnable {
    File fileA, fileB;
    File outFile;

    public static Document parse(InputStream in) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(in);
        return document;
    }

    public XmlMerge(File fileA, File fileB, File outFile) {
        this.fileA = fileA;
        this.fileB = fileB;
        this.outFile = outFile;
    }

    public static void main(String[] args) throws IOException, DocumentException, ParseException {
        File fileA = null, fileB = null;
        File outFile = null;
        Options options = new Options();
        options.addOption("o", "output-file", true, "XML File to export, will not be overwritten. If not given output will be sent to stdout.");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        String[] leftoverArgs = cmd.getArgs();

        if (leftoverArgs.length != 2) {
            System.err.println("Two XML files are needed for merging.");
            printHelp(options);
            System.exit(1);
        }

        fileA = new File(leftoverArgs[0]);
        fileB = new File(leftoverArgs[1]);

        if (!(fileA.exists() && fileB.exists())) {
            System.err.println("Input XML file/s do/es not exist.");
            printHelp(options);
            System.exit(1);
        }


        if (cmd.hasOption('o')) {
            File tmp_outFile = new File(cmd.getOptionValue('o'));
            if (!tmp_outFile.exists()) {
                outFile = tmp_outFile;
            }
        }

        XmlMerge m = new XmlMerge(fileA, fileB, outFile);
        m.run();
    }

    private static void printHelp(Options options) {
        HelpFormatter f = new HelpFormatter();
        f.printHelp("XmlMerge <fileA> <fileB>", options);
    }

    public void run() {
        try {
            System.out.println(String.format("# Parsing document %s", fileA.getPath()));
            Document srcDocument = parse(new FileInputStream(fileA));
            System.out.println(String.format("# Parsing document %s", fileB.getPath()));
            Document targetDocument = parse(new FileInputStream(fileB));

            Element targetRoot = targetDocument.getRootElement();
            int numDocs = targetRoot.elements().size();
            int countDocs = 0;

            // we need to cache the src documents nodes by their ids ...
            System.out.println("# Caching nodes");
            HashMap<String, Element> nodeCache = new HashMap<>(10000);
            Iterator<Element> docIterator = srcDocument.getRootElement().elementIterator("doc");
            while (docIterator.hasNext()) {
                Element doc = docIterator.next();
                String id = doc.selectSingleNode("field[@name='id']").getText();
                nodeCache.put(id, (Element) doc.detach());
            }
            System.out.println("# " + nodeCache.size() + " nodes cached from " + fileA.getPath());
            System.out.println("# Merging data");
            docIterator = targetRoot.elementIterator("doc");
            while (docIterator.hasNext()) {
                Element targetDoc = docIterator.next();
                String id = targetDoc.selectSingleNode("field[@name='id']").getText();
                Element srcDoc = nodeCache.get(id.trim());
                // System.out.println();
                // add all from doc2 besides id
                Iterator<Element> fieldIterator = srcDoc.elementIterator("field");
                while (fieldIterator.hasNext()) {
                    Element f = fieldIterator.next();
                    if (!f.attributeValue("name").equals("id")) {
                        f.detach();
                        targetDoc.add(f);
                    }
                }
                // add imgurl if not here yet:
                // <field name="imgurl">flickrphotos/04/6296825000_d16622c83b_m.jpg</field>
                Element url = targetDoc.addElement("field");
                url.addAttribute("name", "imgurl");
                url.setText(id.substring(id.indexOf("test/")));
                countDocs++;
                if (countDocs%2000 == 0) System.out.printf("# %03d%% entries merged.\n", (int) (countDocs * 100d / numDocs));
            }
            if (outFile!=null) {
                System.out.println((String.format("# Writing output to file %s", outFile.getPath())));
                BufferedWriter writer = new BufferedWriter(new FileWriter(outFile), 1024 * 1024 * 10);
                targetDocument.write(writer);
                writer.close();
            } else {
                OutputStreamWriter writer = new OutputStreamWriter(System.out);
                targetDocument. write(writer);
                writer.close();
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
