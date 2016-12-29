package net.semanticmetadata.lire.solr;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;
import java.util.HashMap;

/**
 * Used to hold the hashing terms in memory.
 * Created by mlux on 08.12.2016.
 */
public class HashTermStatistics {
    private static HashTermStatistics instance = new HashTermStatistics();
    private static HashMap<String, HashMap<String, Integer>> termstats = new HashMap<>(8);

    public static HashTermStatistics getInstance() {
        return instance;
    }

    public static void addToStatistics(SolrIndexSearcher searcher, String field) throws IOException {
        // check if this field is already in the stats.
//        synchronized (instance) {
            if (termstats.get(field)!=null) return;
//        }
        // else add it to the stats.
        Terms terms = searcher.getSlowAtomicReader().terms(field);
        HashMap<String, Integer> term2docFreq = new HashMap<String, Integer>(1000);
        termstats.put(field, term2docFreq);
        if (terms!=null) {
            TermsEnum termsEnum = terms.iterator();
            BytesRef term;
            while ((term = termsEnum.next()) != null) {
                term2docFreq.put(term.utf8ToString(), termsEnum.docFreq());
            }
        }
    }

    public static int docFreq(String field, String term) {
        if (termstats.get(field).get(term)!=null)
            return termstats.get(field).get(term);
        else
            return 0;
    }

}
