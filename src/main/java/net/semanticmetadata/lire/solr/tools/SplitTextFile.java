package net.semanticmetadata.lire.solr.tools;

import net.semanticmetadata.lire.utils.CommandLineUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.util.Properties;

/**
 * Handy file to split large XML files and create a shell script for uploading it to Solr.
 */
public class SplitTextFile {

    public static final String helpMessage = "Help\n" +
            "====\n" +
            "$> SplitTextFile -i <infile.xml> -l <lines> [-x] [-d <directory>]\n" +
            "\n" +
            "-i ... file to read, output will numbered like infileXXX.xml\n" +
            "-l ... number of lines per file\n" +
            "-x ... create a shell script for indexing like infile-index.sh\n" +
            "-d ... output directory (without trailing '/')\n";

    public static int charBufferSize = 1024 * 1024 * 128; // 128 MB

    public static void main(String[] args) throws IOException {
        Properties p = CommandLineUtils.getProperties(args, helpMessage, new String[]{"-i", "-l"});
        File in = new File(p.getProperty("-i"));
        int split = 1000;
        try {
            split = Integer.parseInt(p.getProperty("-l"));
        } catch (NumberFormatException e) {
            System.exit(1);
            System.err.printf("Number of lines as given is not a number: %s\n", p.getProperty("-l"));
        }
        if (!in.exists()) {
            System.exit(1);
            System.err.printf("File %s does not exist.\n", in.getName());
        }

        // ok, let's split
        int count = 0;
        int filenum = 0;
        String filePattern = FilenameUtils.getBaseName(p.getProperty("-i")) + "%03d." + FilenameUtils.getExtension(p.getProperty("-i"));
        if (p.get("-d")!=null) {
            filePattern = p.getProperty("-d") + '/' + filePattern;
        }
        String fileExec = FilenameUtils.getBaseName(p.getProperty("-i")) + "-index.sh";
        String currentFileName = String.format(filePattern, filenum);
        System.out.printf("Writing file %s ...\n", currentFileName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(currentFileName), charBufferSize);
        BufferedWriter bwExec = null;
        if (p.get("-x") != null) bwExec = new BufferedWriter(new FileWriter(fileExec));
        addToShellFile(bwExec, currentFileName);
        LineIterator lineIterator = FileUtils.lineIterator(in);
        bw.write("<add>\n");
        while (lineIterator.hasNext()) {
            String currentLine = lineIterator.next();
            if (!(currentLine.startsWith("<add>") || currentLine.startsWith("#") || currentLine.startsWith("</add>"))) {
                // check if the old file is full ...
                if (count >= split) {
                    bw.write("</add>\n");
                    bw.close();
                    currentFileName = String.format(filePattern, ++filenum);
                    System.out.printf("Writing file %s ...\n", currentFileName);
                    bw = new BufferedWriter(new FileWriter(currentFileName), charBufferSize);
                    bw.write("<add>\n");
                    count = 0;
                    addToShellFile(bwExec, currentFileName);
                }
                // write to the current file ...
                bw.write(currentLine);
                bw.write('\n');
                count++;
            }
        }
        bw.write("</add>\n");
        bw.close();
        if (bwExec != null) bwExec.close();
        System.out.println("Finished.");
    }

    private static void addToShellFile(Writer w, String filename) throws IOException {
        if (w != null) {
            w.write("echo \"*** " + filename + "\"\n");
            w.write("curl http://localhost:8983/solr/lire/update -H \"Content-Type: text/xml\" --data-binary @" + filename + "\n");
            w.write("curl http://localhost:8983/solr/lire/update -H \"Content-Type: text/xml\" --data-binary \"<commit/>\"\n");
            w.flush();
        }
    }
}


/*
Help
====
$> SplitTextFile -i <infile.xml> -l <lines> [-x] [-d <directory>]

-i ... file to read, output will numbered like infileXXX.xml
-l ... number of lines per file
-x ... create a shell script for indexing like infile-index.sh
-d ... output directory (without trailing '/')
 */