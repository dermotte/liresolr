package net.semanticmetadata.lire.solr;

import com.fasterxml.jackson.databind.util.ArrayIterator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.util.Iterator;

/**
 * Created by mlux on 19.12.2016.
 */
public class TopDocsIterator implements Iterator<Integer> {
    TopDocs docs;
    ArrayIterator<ScoreDoc> innerIterator;

    public TopDocsIterator(TopDocs docs) {
        this.docs = docs;
        innerIterator = new ArrayIterator<>(docs.scoreDocs);
    }

    @Override
    public boolean hasNext() {
        return innerIterator.hasNext();
    }

    @Override
    public Integer next() {
        ScoreDoc d = innerIterator.next();
        return d.doc;
    }
}
