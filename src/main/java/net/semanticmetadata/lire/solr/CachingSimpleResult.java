package net.semanticmetadata.lire.solr;

import net.semanticmetadata.lire.searchers.SimpleResult;
import org.apache.lucene.document.Document;

/**
 * Created by mlux on 08.12.2016.
 */
public class CachingSimpleResult extends SimpleResult {
    Document document;

    /**
     * Constructor for a result. The indexNumer is needed for sorting issues. Problem is that the TreeMap used for
     * collecting the results considers equality of objects based on the compareTo function. So if an image is in
     * the index twice, it's only found one time, the second instance -- with the same distance, but a
     * different Lucene document -- is not added to the TreeMap at runtime as their distance between each other
     * would be 0. This is tweaked with the running number of the document from the index, so duplicate documents that
     * are in the index twice, appear in the result list in the order they are found in the index. See also compareTo(...)
     * method.
     *
     * @param distance    the actual distance to the query
     * @param document    the document instance form the Lucene index
     * @param indexNumber the running number from the IndexReader. Needed for sorting issues in the result TreeMap.
     */
    public CachingSimpleResult(double distance, Document document, int indexNumber) {
        super(distance, indexNumber);
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }

    /**
     * Sets all the member anew, just to be able to save instances.
     * @param distance
     * @param document
     * @param indexNumber
     */
    public void set(double distance, Document document, int indexNumber) {
        this.setDistance(distance);
        this.document = document;
        this.setIndexNumber(indexNumber);
    }

}
