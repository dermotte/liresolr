/*
 * This file is part of the LIRE project: http://www.semanticmetadata.net/lire
 * LIRE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * LIRE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LIRE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * We kindly ask you to refer the any or one of the following publications in
 * any publication mentioning or employing Lire:
 *
 * Lux Mathias, Savvas A. Chatzichristofis. Lire: Lucene Image Retrieval –
 * An Extensible Java CBIR Library. In proceedings of the 16th ACM International
 * Conference on Multimedia, pp. 1085-1088, Vancouver, Canada, 2008
 * URL: http://doi.acm.org/10.1145/1459359.1459577
 *
 * Lux Mathias. Content Based Image Retrieval with LIRE. In proceedings of the
 * 19th ACM International Conference on Multimedia, pp. 735-738, Scottsdale,
 * Arizona, USA, 2011
 * URL: http://dl.acm.org/citation.cfm?id=2072432
 *
 * Mathias Lux, Oge Marques. Visual Information Retrieval using Java and LIRE
 * Morgan & Claypool, 2013
 * URL: http://www.morganclaypool.com/doi/abs/10.2200/S00468ED1V01Y201301ICR025
 *
 * Copyright statement:
 * --------------------
 * (c) 2002-2013 by Mathias Lux (mathias@juggle.at)
 *     http://www.semanticmetadata.net/lire, http://www.lire-project.net
 */

package net.semanticmetadata.lire.solr;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.DocTermsIndexDocValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.ColorLayout;
import net.semanticmetadata.lire.solr.tools.RandomAccessBinaryDocValues;

/**
 * A query function for sorting results based on the LIRE CBIR functions.
 * Implementation based partially on the outdated guide given on http://www.supermind.org/blog/756,
 * comments on the mailing list provided from Chris Hostetter, and the 4.4 Solr and Lucene source.
 *
 * @author Mathias Lux, 17.09.13 12:26
 */
public class LireValueSource extends ValueSource {
    String field = "cl_hi";  // default field
    byte[] histogramData;
    GlobalFeature feature, tmpFeature;
    double maxDistance = Double.MAX_VALUE;
    String objectHashBase = null; // used to store the combination of parameters to create a way to counter caching of functions with different function values.

    /**
     * @param featureField the field of the feature used for sorting.
     * @param hist the histogram in bytes.
     * @param maxDistance  the distance value returned if there is no distance calculation possible.
     */
    public LireValueSource(String featureField, byte[] hist, double maxDistance) {
        if (featureField != null) {
            field = featureField;
        }
        if (!field.endsWith("_hi")) { // TODO: Make that somewhat not so string dependent .. maybe connect with the postfix in FeatureRegistry
            field += "_hi";
        }
        this.histogramData = hist;
        this.maxDistance = maxDistance;

        // get the feature from the feature registry.
        if (field == null) {
            feature = new ColorLayout();
            tmpFeature = new ColorLayout();
        } else {
            try {
                if (FeatureRegistry.getClassForFeatureField(field) != null) {// check if feature is registered.
                    feature = (GlobalFeature) FeatureRegistry.getClassForFeatureField(field).newInstance();
                    tmpFeature = (GlobalFeature) FeatureRegistry.getClassForFeatureField(field).newInstance();
                } else {
                    System.err.println("Feature " + field + " is not registered.");
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // debug ...
        // System.out.println("Setting " + feature.getClass().getName() + " to " + Base64.byteArrayToBase64(hist, 0, hist.length));

        // adding all parameters to a string to create a hash.
        objectHashBase = field + Arrays.toString(hist) + maxDistance;
        feature.setByteArrayRepresentation(hist);
    }

    /*
    public FunctionValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
        final FieldInfo fieldInfo = readerContext.reader().getFieldInfos().fieldInfo(field);
        if (fieldInfo != null && fieldInfo.getDocValuesType() == FieldInfo.DocValuesType.BINARY) {
            final BinaryDocValues binaryValues = FieldCache.DEFAULT.getTerms(readerContext.reader(), field, false);
            return new FunctionValues() {
                BytesRef tmp = new BytesRef();

                @Override
                public boolean exists(int doc) {
                    return bytesVal(doc, tmp);
                }

                @Override
                public boolean bytesVal(int doc, BytesRef target) {
                    tmp = binaryValues.get(doc);
                    return target.length > 0;
                }

                // This is the actual value returned
                @Override
                public float floatVal(int doc) {
                    tmp = binaryValues.get(doc);
                    if (tmp.length > 0) {
                        tmpFeature.setByteArrayRepresentation(tmp.bytes, tmp.offset, tmp.length);
                        return tmpFeature.getDistance(feature);
                    } else
                        return (float) maxDistance; // make sure max distance is returned for those without value!
                }

                @Override
                public Object objectVal(int doc) {
                    return floatVal(doc);
                }

                @Override
                public String toString(int doc) {
                    return description() + '=' + strVal(doc);
                }

                @Override
                / *
                 * This method has to be implemented to support sorting!
                 * /
                public double doubleVal(int doc) {
                    return (double) floatVal(doc);
                }
            };
        } else {
            // there is no DocVal to sort by. Therefore we need to set the function value to -1 and everything without DocVal gets ranked first?
            return new DocTermsIndexDocValues(this, readerContext, field) {
                @Override
                protected String toTerm(String readableValue) {
                    return Double.toString(maxDistance);
                }

                @Override
                public Object objectVal(int doc) {
                    return maxDistance;
                }

                @Override
                public String toString(int doc) {
                    return description() + '=' + strVal(doc);
                }


                public double doubleVal(int doc) {
                    return maxDistance;
                }
            };
        }
    } */

    @Override
    /**
     * Check also {@link org.apache.lucene.queries.function.valuesource.BytesRefFieldSource}
     */
    public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
        final FieldInfo fieldInfo = readerContext.reader().getFieldInfos().fieldInfo(field);
        if (fieldInfo != null && fieldInfo.getDocValuesType() == DocValuesType.BINARY) {
//            final BinaryDocValues binaryValues = DocValues.getBinary(readerContext.reader(), field);
//            final Bits docsWithField = DocValues.getDocsWithField(readerContext.reader(), field);
            final BinaryDocValues binaryValues = new RandomAccessBinaryDocValues(() -> {
                try {
                    return DocValues.getBinary(readerContext.reader(), field);
                } catch (IOException e) {
                    throw new RuntimeException("BinaryDocValues problem.", e);
                }

            });



            return new FunctionValues() {
                @Override
                public boolean exists(int doc) throws IOException {
                    return binaryValues.advance(doc) == doc;
//                    return docsWithField.get(doc);
                }

                @Override
                public boolean bytesVal(int doc, BytesRefBuilder target)
                        throws IOException {
//                    target.copyBytes(binaryValues.get(doc));
//                    return target.length() > 0;
                    BytesRef bytesRef;
                    if (binaryValues.advance(doc) == doc) {
                        bytesRef = binaryValues.binaryValue();
                    } else {
                        bytesRef = new BytesRef(BytesRef.EMPTY_BYTES);
                    }
                    target.copyBytes(bytesRef);
                    return target.length() > 0;

                }


                @Override
                public float floatVal(int doc) throws IOException {
                    return (float) doubleVal(doc);
                }

                @Override
                public String strVal(int doc) throws IOException {
                    final BytesRefBuilder bytes = new BytesRefBuilder();
                    return bytesVal(doc, bytes)
                            ? bytes.get().utf8ToString()
                            : null;
                }

                /**
                 * This method basically decides which type is delivered on request. It can be a String,
                 * in this case it is the double form the distance function.
                 * @param doc
                 * @return the distance as Double, mapping to {@link FunctionValues#doubleVal(int)}
                 * @throws IOException
                 */
                @Override
                public Object objectVal(int doc) throws IOException {
                    return doubleVal(doc);
                }

                @Override
                public String toString(int doc) throws IOException {
                    return description() + '=' + strVal(doc);
                }

                @Override
                public double doubleVal(int doc) throws IOException {
                    BytesRef bytesRef = getBytesRef(binaryValues, doc);
                    if (bytesRef.length > 0) {
                        tmpFeature.setByteArrayRepresentation(
                                bytesRef.bytes,
                                bytesRef.offset,
                                bytesRef.length);
//                        tmpFeature.setByteArrayRepresentation(binaryValues.get(doc).bytes, binaryValues.get(doc).offset, binaryValues.get(doc).length);
                        return tmpFeature.getDistance(feature);
                    }
                    else {
                        return maxDistance; // make sure max distance is returned for those without value
                    }
                }
            };
        } else {
            // there is no DocVal to sort by. Therefore we need to set the function value to -1 and everything without DocVal gets ranked first?
            return new DocTermsIndexDocValues(this, readerContext, field) {
                @Override
                protected String toTerm(String readableValue) {
                    return Double.toString(maxDistance);
                }

                @Override
                public Object objectVal(int doc) {
                    return maxDistance;
                }

                @Override
                public String toString(int doc) throws IOException {
                    return description() + '=' + strVal(doc);
                }


                @Override
                public double doubleVal(int doc) {
                    return maxDistance;
                }
            };
        }
    }

	// TODO: Check for a workaround to not introduce variables for runtime performance (or check if this is necessary first).
    private BytesRef getBytesRef(BinaryDocValues bdv, int docId)
            throws IOException {
        if (bdv != null && bdv.advance(docId) == docId) {
//        if (bdv != null && bdv.docID() < docId && bdv.advance(docId) == docId) {
//        if (bdv != null && bdv.advanceExact(docId)) {
            return bdv.binaryValue();
        }
        return new BytesRef(BytesRef.EMPTY_BYTES);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LireValueSource) {
            // check if the function has had the same parameters.
            return objectHashBase.equals(((LireValueSource) o).objectHashBase);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return objectHashBase.hashCode();
    }

    @Override
    public String description() {
        return "distance to a given feature vector";
    }
}
