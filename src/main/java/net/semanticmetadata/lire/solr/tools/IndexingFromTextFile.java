package net.semanticmetadata.lire.solr.tools;

import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.indexers.hashing.MetricSpaces;
import net.semanticmetadata.lire.indexers.tools.text.AbstractDocumentWriter;
import net.semanticmetadata.lire.solr.FeatureRegistry;
import net.semanticmetadata.lire.solr.HashingMetricSpacesManager;
import net.semanticmetadata.lire.solr.indexing.ParallelSolrIndexer;
import net.semanticmetadata.lire.utils.CommandLineUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ThresholdingOutputStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Reads a text file created by {@link net.semanticmetadata.lire.indexers.tools.text.ParallelExtraction} and creates
 * an XML file for being sent to Solr while optionally adding hashes.
 */
public class IndexingFromTextFile extends AbstractDocumentWriter {
    private static final boolean useXML = true;
    private static String helpMessage = "HELP\n" +
            "====\n" +
            "\n" +
            "$> IndexingFromTextFile -i <infile> -o <outfile> [-hb] [-hm] [-s <documents>]\n" +
            "\n" +
            "-i  ... path to the input file\n" +
            "-o  ... path to the XML file for output\n" +
            "-s  ... split files to have a maximum of <documents> per file.\n" +
            "-hb ... employ BitSampling Hashing (overrules MetricSpaces, loads all *.mds files from current directory)\n" +
            "-hm ... employ MetricSpaces Indexing";

    File outfile;
    LinkedBlockingQueue<QueueItem> queue = new LinkedBlockingQueue<>(500);
    BufferedWriter bw;
    List<Thread> threads;
    private int numThreads = 8;

    public IndexingFromTextFile(File infile, File outFile,
                                boolean doHashingBitSampling, boolean doMetricSpaceIndexing, int splitAt) throws IOException {
        super(infile, true, doHashingBitSampling, doMetricSpaceIndexing);
        this.outfile = outFile;
        bw = new BufferedWriter(new FileWriter(outfile));
        super.loadMdsFilesAutomatically = false; // skip the auto load and read from resources ...
        HashingMetricSpacesManager.init(); // load reference points from disk.
    }

    public static void main(String[] args) {
        Properties p = CommandLineUtils.getProperties(args, helpMessage, new String[]{"-i", "-o"});
        File inFile = new File(p.getProperty("-i"));
        File outFile = new File(p.getProperty("-o"));
        int splitAt = -1;
        if (p.getProperty("-s") != null)
            splitAt = Integer.parseInt(p.getProperty("-s"));
        if (!inFile.exists()) {
            System.err.printf("File %s not found!\n", inFile.getPath());
            System.out.println(helpMessage);
            System.exit(1);
        }
        try {
            IndexingFromTextFile i = new IndexingFromTextFile(inFile, outFile,
                    p.get("-hb") != null, p.get("-hm") != null, splitAt);
            Thread t = new Thread(i);
            t.start();
            t.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void startWriting() {
        try {
            bw.write("<add>");
        } catch (IOException e) {
            e.printStackTrace();
        }
        threads = new LinkedList<>();
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(new Consumer());
            t.start();
            threads.add(t);
        }
    }

    @Override
    protected void finishWriting() {
        try {
            for (int i = 0; i < 20; i++) {
                queue.put(new QueueItem(null, null));
            }
            for (Iterator<Thread> iterator = threads.iterator(); iterator.hasNext(); ) {
                iterator.next().join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            bw.write("</add>");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void write(String s, ArrayList<GlobalFeature> a) {
        // clone the features first:
        ArrayList<GlobalFeature> tmp = new ArrayList<>(a.size());
        try {
            for (Iterator<GlobalFeature> iterator = a.iterator(); iterator.hasNext(); ) {
                GlobalFeature f = iterator.next();
                GlobalFeature n = (GlobalFeature) f.getClass().newInstance();
                n.setByteArrayRepresentation(f.getByteArrayRepresentation());
                tmp.add(n);
            }
            queue.put(new QueueItem(s, tmp));
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class QueueItem {
        String id;
        List<GlobalFeature> features;

        public QueueItem(String id, List<GlobalFeature> features) {
            this.id = id;
            this.features = features;
        }
    }

    class Consumer implements Runnable {
        HashMap<String, String> document = new HashMap<>();

        @Override
        public void run() {
            try {
                QueueItem data = queue.take();
                while (data.id != null) {
                    document.clear();
                    document.put("id", data.id);
                    document.put("title", data.id);
                    for (Iterator<GlobalFeature> iterator = data.features.iterator(); iterator.hasNext(); ) {
                        GlobalFeature f = iterator.next();
                        document.put(FeatureRegistry.getCodeForClass(f.getClass()) + FeatureRegistry.featureFieldPostfix,
                                org.apache.commons.codec.binary.Base64.encodeBase64String(f.getByteArrayRepresentation()));
                        if (doHashingBitSampling) {
                            document.put(FeatureRegistry.getCodeForClass(f.getClass()) + FeatureRegistry.hashFieldPostfix,
                                    ParallelSolrIndexer.arrayToString(BitSampling.generateHashes(f.getFeatureVector())));

                        } else if (doMetricSpaceIndexing) {
                            if (MetricSpaces.supportsFeature(f)) {
                                document.put(FeatureRegistry.getCodeForClass(f.getClass()) + FeatureRegistry.metricSpacesFieldPostfix,
                                        MetricSpaces.generateHashString(f));
                            }

                        }
                    }
                    output(document);
                    data = queue.take();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void output(HashMap<String, String> document) {
            StringBuilder sb = new StringBuilder();
            sb.append("<doc>");
            for (Iterator<String> iterator = document.keySet().iterator(); iterator.hasNext(); ) {
                String fieldName = iterator.next();
                sb.append("<field name=\"" + fieldName + "\">");
                sb.append(document.get(fieldName));
                sb.append("</field>");
            }
            sb.append("</doc>\n");
            synchronized (bw) {
                try {
                    bw.write(sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class SplittingOutputStream extends ThresholdingOutputStream {
        FileOutputStream currentOutputStream;
        int count = 0;
        String extension = "xml";
        String basename = "";

        public SplittingOutputStream(int threshold, File outFile) throws FileNotFoundException {
            super(threshold);
            String path = outFile.getPath();
            extension = FilenameUtils.getExtension(path);
            basename = FilenameUtils.getBaseName(path);
            this.currentOutputStream = new FileOutputStream(basename + String.format("%03d", count) + extension, false);
        }

        @Override
        protected OutputStream getStream() throws IOException {
            return currentOutputStream;
        }

        @Override
        protected void thresholdReached() throws IOException {
            // flush and close current, roll over to new one.
            currentOutputStream.flush();
            currentOutputStream.close();
            currentOutputStream = new FileOutputStream(basename + String.format("%03d", ++count) + extension, false);
        }
    }

}
/*
HELP
====

$> IndexingFromTextFile -i <infile> -o <outfile> [-hb] [-hm] [-s <documents>]

-i  ... path to the input file
-o  ... path to the Lucene index for output
-s  ... split files to have a maximum of <documents> per file.
-hb ... employ BitSampling Hashing (overrules MetricSpaces, loads all *.mds files from current directory)
-hm ... employ MetricSpaces Indexing
 */
