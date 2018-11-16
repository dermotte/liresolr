package net.semanticmetadata.lire.solr.tools;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalShortFeature;
import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.solr.HashingMetricSpacesManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

public class EncodeAndHash {
    public static void main(String[] args) throws IOException, SAXException, TransformerException {
        HashingMetricSpacesManager.init();
        DOMParser parser = new DOMParser();
        parser.parse("images_150k_cat.xml");
        Document doc = parser.getDocument();
        Element root = doc.getDocumentElement();
        NodeList docs = root.getElementsByTagName("doc");
        for (int j = 0; j < docs.getLength(); j++) {
            Element nextDoc = (Element) docs.item(j);
            NodeList fields = nextDoc.getElementsByTagName("field");
            for (int i = 0; i<fields.getLength(); i++) {
                Element field = (Element) fields.item(i);
                String name = field.getAttribute("name");
                if (name.equals("sf_hi")) {
                    GenericGlobalShortFeature f = new GenericGlobalShortFeature();
                    String[] numbers = field.getTextContent().split(";");
                    short[] d = new short[numbers.length];
                    for (int k = 0; k < numbers.length; k++) {
                        d[k] = Short.parseShort(numbers[k]);
                    }
                    f.setData(d);
                    int[] hashes = BitSampling.generateHashes(f.getFeatureVector());

                    System.out.println(Base64.getEncoder().encodeToString(f.getByteArrayRepresentation()));
                    System.out.println(Utilities.hashesArrayToString(hashes));

                    field.setTextContent(Base64.getEncoder().encodeToString(f.getByteArrayRepresentation()));
                    Element newField = doc.createElement("field");
                    newField.setAttribute("name", "sf_ha");
                    newField.setTextContent(Utilities.hashesArrayToString(hashes));
                    nextDoc.appendChild(newField);
                }
            }
            // todo: get field <field name="sf_hi">, then change it to the appropriate data, then add the hashes.
        }
        // todo: serialize document
        // write the content on console
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        System.out.println("-----------Modified File-----------");
        StreamResult consoleResult = new StreamResult(new FileOutputStream("150k_out.xml"));
//        StreamResult consoleResult = new StreamResult(System.out);
        transformer.transform(source, consoleResult);
    }
}
