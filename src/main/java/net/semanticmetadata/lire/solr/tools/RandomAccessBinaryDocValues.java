package net.semanticmetadata.lire.solr.tools;

import java.io.IOException;
import java.util.function.Supplier;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.util.BytesRef;


/**
 * Wraps a {@link BinaryDocValues} creation strategy so it can be reset
 * if needed.  This is a hack to port liresolr to Lucene 7+ (which enforces
 * ordered doc values) until a more formal solution is in place.
 * @author Pascal Essiembre
 */
public class RandomAccessBinaryDocValues extends BinaryDocValues {

    private final Supplier<BinaryDocValues> supplier;
    private BinaryDocValues docValues;

    public RandomAccessBinaryDocValues(Supplier<BinaryDocValues> supplier) {
        super();
        this.supplier = supplier;
        this.docValues = supplier.get();
    }

    @Override
    public BytesRef binaryValue() throws IOException {
        if (docValues == null) {
            return new BytesRef(BytesRef.EMPTY_BYTES);
        }
        return docValues.binaryValue();
    }

    @Override
    public boolean advanceExact(int target) throws IOException {
        resetIfNeeded(target);
        if (docValues == null) {
            return false;
        }
        return docValues.advanceExact(target);
    }

    @Override
    public int docID() {
        if (docValues == null) {
            return NO_MORE_DOCS;
        }
        return docValues.docID();
    }

    @Override
    public int nextDoc() throws IOException {
        if (docValues == null) {
            return NO_MORE_DOCS;
        }
        return docValues.nextDoc();
    }

    @Override
    public int advance(int target) throws IOException {
        resetIfNeeded(target);
        if (docValues == null) {
            return NO_MORE_DOCS;
        }
        return docValues.advance(target);
    }

    @Override
    public long cost() {
        if (docValues == null) {
            return 0;
        }
        return docValues.cost();
    }

    private void resetIfNeeded(int target) {
        if (docValues == null) {
            docValues = supplier.get();
        } else {
            int id = docValues.docID();
            if (id != -1 && id != NO_MORE_DOCS
                    && target < docValues.docID()) {
                docValues = supplier.get();
            }
        }
    }
}
