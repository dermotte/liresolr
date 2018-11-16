package net.semanticmetadata.lire.solr.tools;

import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalShortFeature;
import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.solr.HashingMetricSpacesManager;
import org.apache.solr.common.util.XML;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.util.Base64;

public class EncodeAndHashStax {
    static BufferedWriter bw;
    public static void main(String[] args) throws XMLStreamException, IOException {
        HashingMetricSpacesManager.init();

        bw = new BufferedWriter(new FileWriter("150k_out.xml"));

        StringBuilder sb = new StringBuilder();
        String hashes = null;

        XMLInputFactory f = XMLInputFactory.newInstance();
        XMLStreamReader r = f.createXMLStreamReader(new FileInputStream("images_150k_cat.xml"));
        boolean isInRelevantTag = false;
        boolean skipTag = false;
        while(r.hasNext()) {
            int state = r.next();
            if (state == XMLEvent.START_ELEMENT) {
                if (r.getLocalName().equals("field")) {
                    String ft = r.getAttributeValue(0);
                    if (ft.equals("sf_hi")) isInRelevantTag = true;
                    if (!ft.equals("sf_ha")) {
                        write("<" + r.getLocalName() + " name=\""+ft+"\">");
                    } else skipTag = true;
                } else {
                    write("<" + r.getLocalName() + ">");
                }
            } else if (state == XMLEvent.CHARACTERS) {
                if (!isInRelevantTag)
                    write(r.getText());
                else {
                    // do the trick with the Hashing etc.
                    sb.append(r.getText());

                }
            } else if (state == XMLEvent.END_ELEMENT) {
                if (isInRelevantTag) {
                    isInRelevantTag = false;
                    hashes = compute(sb);
                    sb.delete(0,sb.length());
                }
                if (!skipTag)
                    writeln("</" + r.getLocalName() + ">");
                else skipTag = false;
                if (hashes!=null) {
                    write("<field name=\"sf_ha\">");
                    write(hashes);
                    writeln("</field>");
                    hashes = null;
                }
            }
        }
        bw.close();
    }

    private static String compute(StringBuilder sb) throws IOException {
        GenericGlobalShortFeature feature = new GenericGlobalShortFeature();
        String[] numbers = sb.toString().split(";");
        short[] d = new short[numbers.length];
        for (int k = 0; k < numbers.length; k++) {
            d[k] = Short.parseShort(numbers[k]);
        }
        feature.setData(d);
                    int[] hashes = BitSampling.generateHashes(feature.getFeatureVector());

        write(Base64.getEncoder().encodeToString(feature.getByteArrayRepresentation()));
        return Utilities.hashesArrayToString(hashes);
    }
    
    private static void write(String s) throws IOException {
        bw.write(s);
    }
    
    private static void writeln(String s) throws IOException {
        write(s + "\n");
    }
}
