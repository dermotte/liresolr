package net.semanticmetadata.lire.solr.indexing;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;


public class XmlMerge  {
    public static Document parse(InputStream in) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(in);
        return document;
    }


    public static void main(String[] args) throws IOException, DocumentException {
        System.out.println("# Parsing document 1");
        Document srcDocument = parse(new FileInputStream("test.xml"));
        System.out.println("# Parsing document 2");
        Document targetDocument = parse(new FileInputStream("images-wipo.xml"));

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
        System.out.println("# " + nodeCache.size() + " nodes cached");
        System.out.println("# Doing the merge");
        docIterator = targetRoot.elementIterator("doc");
        while (docIterator.hasNext()) {
            Element targetDoc = docIterator.next();
            String id = targetDoc.selectSingleNode("field[@name='id']").getText();
            // d1.selectSingleNode("//doc[field='test/US71000357.png']")
            Element srcDoc = nodeCache.get(id.substring(id.indexOf("test/")));
            System.out.println();
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
            System.out.printf("%03d%%",(int) (countDocs*100d/numDocs));
        }
        System.out.println("\n# Writing output to file.");
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("final.xml"));
        targetDocument.write(writer);
        writer.close();
    }
}
