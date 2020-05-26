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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.imageanalysis.features.global.GenericGlobalShortFeature;
import net.semanticmetadata.lire.solr.features.ShortFeatureCosineDistance;
import net.semanticmetadata.lire.solr.tools.EncodeAndHashCSV;
import net.semanticmetadata.lire.solr.tools.Utilities;
import org.apache.commons.codec.binary.Base64;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;

import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.ColorLayout;
import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.indexers.hashing.MetricSpaces;
import net.semanticmetadata.lire.solr.tools.RandomAccessBinaryDocValues;
import net.semanticmetadata.lire.utils.ImageUtils;
import net.semanticmetadata.lire.utils.StatsUtils;

/**
 * This is the main LIRE RequestHandler for the Solr Plugin. It supports query by example using the indexed id,
 * an url or a feature vector. Furthermore, feature extraction and random selection of images are supported.
 *
 * @author Mathias Lux, mathias@juggle.at, 07.07.13
 */

public class LireRequestHandler extends RequestHandlerBase {
    //    private static HashMap<String, Class> fieldToClass = new HashMap<String, Class>(5);
    private long time = 0;
    private int defaultNumberOfResults = 60;
    /**
     * number of candidate results retrieved from the index. The higher this number, the slower,
     * the but more accurate the retrieval will be. 10k is a good value for starters.
     */
    private int numberOfCandidateResults = 10000;
    private static final int DEFAULT_NUMBER_OF_CANDIDATES = 10000;

    /**
     * The number of query terms that go along with the TermsFilter search. We need some to get a
     * score, the less the faster. I put down a minimum of three in the method, this value gives
     * the percentage of the overall number used (selected randomly).
     */
    private double numberOfQueryTerms = 0.33;
    private static final double DEFAULT_NUMBER_OF_QUERY_TERMS = 0.33;

    /**
     * If metric spaces should be used instead of BitSampling.
     */
    private boolean useMetricSpaces = false;
    private static final boolean DEFAULT_USE_METRIC_SPACES = false;

    static {
        HashingMetricSpacesManager.init(); // load reference points from disk.
    }


    @Override
    public void init(NamedList args) {
        super.init(args);
    }

    /**
     * Handles three types of requests.
     * <ol>
     * <li>search by already extracted images.</li>
     * <li>search by an image URL.</li>
     * <li>Random results.</li>
     * </ol>
     *
     * @param req
     * @param rsp
     * @throws Exception
     */
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        // (1) check if the necessary parameters are here
        if (req.getParams().get("hashes") != null) { // we are searching for hashes ... without hashes one should go for the lirefunc version.
            handleHashSearch(req, rsp); // not really supported, just here for legacy.
        } else if (req.getParams().get("url") != null) { // we are searching for an image based on an URL
            handleUrlSearch(req, rsp);
        } else if (req.getParams().get("id") != null) { // we are searching for an image based on an URL
            handleIdSearch(req, rsp);
        } else if (req.getParams().get("extract") != null) { // we are trying to extract from an image URL.
            handleExtract(req, rsp);
        } else { // lets return random results.
            handleRandomSearch(req, rsp);
        }
    }

    /**
     * Handles the get parameters id, field and rows.
     *
     * @param req
     * @param rsp
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void handleIdSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, InstantiationException, IllegalAccessException {
        SolrIndexSearcher searcher = req.getSearcher();
        try {
//            TopDocs hits = searcher.search(new TermQuery(new Term("id", req.getParams().get("id"))), 1);
            int queryDocId = searcher.getFirstMatch(new Term("id", req.getParams().get("id")));
            // get the parameters
            String tmpParamField = req.getParams().get("field", "cl_ha");
            if (!tmpParamField.endsWith("_ha")) {
                tmpParamField += "_ha";
            }
            final String paramField = tmpParamField;
            numberOfQueryTerms = req.getParams().getDouble("accuracy", DEFAULT_NUMBER_OF_QUERY_TERMS);
            numberOfCandidateResults = req.getParams().getInt("candidates", DEFAULT_NUMBER_OF_CANDIDATES);
            useMetricSpaces = req.getParams().getBool("ms", DEFAULT_USE_METRIC_SPACES);
            int paramRows = req.getParams().getInt("rows", defaultNumberOfResults);

            GlobalFeature queryFeature = (GlobalFeature) FeatureRegistry.getClassForHashField(paramField).newInstance();
            rsp.add("QueryField", paramField);
            rsp.add("QueryFeature", queryFeature.getClass().getName());
            if (queryDocId > -1) {
                // Using DocValues to get the actual data from the index.
//              BinaryDocValues binaryValues = MultiDocValues.getBinaryValues(searcher.getIndexReader(), FeatureRegistry.getFeatureFieldName(paramField));
                BinaryDocValues binaryValues = new RandomAccessBinaryDocValues(() -> {
                    try {
                        return MultiDocValues.getBinaryValues(searcher.getIndexReader(), FeatureRegistry.getFeatureFieldName(paramField));
                    } catch (IOException e) {
                        throw new RuntimeException("BinaryDocValues problem.", e);
                    }

                });
                if (binaryValues == null) {
                    rsp.add("Error", "Could not find the DocValues of the query document. Are they in the index? Id: " + req.getParams().get("id"));
                    // System.err.println("Could not find the DocValues of the query document. Are they in the index?");
                }
//                queryFeature.setByteArrayRepresentation(binaryValues.get(queryDocId).bytes, binaryValues.get(queryDocId).offset, binaryValues.get(queryDocId).length);
                BytesRef bvBytesRef = getBytesRef(binaryValues, queryDocId);
                queryFeature.setByteArrayRepresentation(
                        bvBytesRef.bytes, bvBytesRef.offset, bvBytesRef.length);

                Query query = null;
                if (numberOfQueryTerms >= 0.90) {
                    query = new MatchAllDocsQuery();
                    rsp.add("Note", "Switching to AllDocumentsQuery because accuracy is set higher than 0.9.");
                } else {
                    if (!useMetricSpaces) {
                        // check singleton cache if the term stats can be cached.
                        HashTermStatistics.addToStatistics(searcher, paramField);
                        // Re-generating the hashes to save space (instead of storing them in the index)
                        int[] hashes = BitSampling.generateHashes(queryFeature.getFeatureVector());
                        query = createQuery(hashes, paramField, numberOfQueryTerms);
                    } else if (MetricSpaces.supportsFeature(queryFeature)) {
                        // ----< Metric Spaces >-----
                        int queryLength = (int) StatsUtils.clamp(numberOfQueryTerms * MetricSpaces.getPostingListLength(queryFeature), 3, MetricSpaces.getPostingListLength(queryFeature));
                        String msQuery = MetricSpaces.generateBoostedQuery(queryFeature, queryLength);
                        QueryParser qp = new QueryParser(paramField.replace("_ha", "_ms"), new WhitespaceAnalyzer());
                        query = qp.parse(msQuery);
                    } else {
                        query = new MatchAllDocsQuery();
                        rsp.add("Error", "Feature not supported by MetricSpaces: " + queryFeature.getClass().getSimpleName());
                    }
                }
                doSearch(req, rsp, searcher, paramField, paramRows, getFilterQueries(req), query, queryFeature);
            } else {
                rsp.add("Error", "Did not find an image with the given id " + req.getParams().get("id"));
            }
        } catch (Exception e) {
            rsp.add("Error", "There was an error with your search for the image with the id " + req.getParams().get("id")
                    + ": " + e.getMessage());
        }
    }

    /**
     * Parses the fq param and adds it as a list of filter queries or reverts to null if nothing is found
     * or an Exception is thrown.
     *
     * @param req
     * @return either a query from the QueryParser or null
     */
    private List<Query> getFilterQueries(SolrQueryRequest req) {
        List<Query> filters = null;

        String[] fqs = req.getParams().getParams("fq");
        if (fqs != null && fqs.length != 0) {
            filters = new ArrayList<>(fqs.length);
            try {
                for (String fq : fqs) {
                    if (fq != null && fq.trim().length() != 0) {
                        QParser fqp = QParser.getParser(fq, req);
                        fqp.setIsFilter(true);
                        filters.add(fqp.getQuery());
                    }
                }
            } catch (SyntaxError e) {
                e.printStackTrace();
            }

            if (filters.isEmpty()) {
                filters = null;
            }
        }
        return filters;
    }

    /**
     * Returns a random set of documents from the index. Mainly for testing purposes.
     *
     * @param req
     * @param rsp
     * @throws IOException
     */
    private void handleRandomSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
        SolrIndexSearcher searcher = req.getSearcher();
        Query query = new MatchAllDocsQuery();
        DocList docList = searcher.getDocList(query, getFilterQueries(req), Sort.RELEVANCE, 0, numberOfCandidateResults, 0);
        int paramRows = Math.min(req.getParams().getInt("rows", defaultNumberOfResults), docList.size());
        if (docList.size() < 1) {
            rsp.add("Error", "No documents in index");
        } else {
            LinkedList list = new LinkedList();
            while (list.size() < paramRows) {
                DocList auxList = docList.subset((int) (Math.random() * docList.size()), 1);
                Document doc = null;
                for (DocIterator it = auxList.iterator(); it.hasNext(); ) {
                    doc = searcher.doc(it.nextDoc());
                }
                if (!list.contains(doc)) {
                    list.add(doc);
                }
            }
            rsp.addResponse(list);
        }
    }

    /**
     * Searches for an image given by an URL. Note that (i) extracting image features takes time and
     * (ii) not every image is readable by Java.
     *
     * @param req
     * @param rsp
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void handleUrlSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, InstantiationException, IllegalAccessException {
        SolrParams params = req.getParams();
        String paramUrl = params.get("url");
        String paramField = req.getParams().get("field", "cl_ha");
        if (!paramField.endsWith("_ha")) {
            paramField += "_ha";
        }
        int paramRows = params.getInt("rows", defaultNumberOfResults);
        numberOfQueryTerms = req.getParams().getDouble("accuracy", DEFAULT_NUMBER_OF_QUERY_TERMS);
        numberOfCandidateResults = req.getParams().getInt("candidates", DEFAULT_NUMBER_OF_CANDIDATES);
        useMetricSpaces = req.getParams().getBool("ms", DEFAULT_USE_METRIC_SPACES);


        GlobalFeature feat = null;
        int[] hashes = null;
        Query query = null;
        // wrapping the whole part in the try
        try {
            BufferedImage img = ImageIO.read(new URL(paramUrl).openStream());
            img = ImageUtils.trimWhiteSpace(img);
            // getting the right feature per field:
            if (FeatureRegistry.getClassForHashField(paramField) == null) {
                feat = new ColorLayout();
            } else {
                feat = (GlobalFeature) FeatureRegistry.getClassForHashField(paramField).newInstance();
            }
            feat.extract(img);

            if (!useMetricSpaces) {
                // Re-generating the hashes to save space (instead of storing them in the index)
                HashTermStatistics.addToStatistics(req.getSearcher(), paramField);
                hashes = BitSampling.generateHashes(feat.getFeatureVector());
                query = createQuery(hashes, paramField, numberOfQueryTerms);
            } else if (MetricSpaces.supportsFeature(feat)) {
                // ----< Metric Spaces >-----
                int queryLength = (int) StatsUtils.clamp(numberOfQueryTerms * MetricSpaces.getPostingListLength(feat), 3, MetricSpaces.getPostingListLength(feat));
                String msQuery = MetricSpaces.generateBoostedQuery(feat, queryLength);
                QueryParser qp = new QueryParser(paramField.replace("_ha", "_ms"), new WhitespaceAnalyzer());
                query = qp.parse(msQuery);
            } else {
                rsp.add("Error", "Feature not supported by MetricSpaces: " + feat.getClass().getSimpleName());
                query = new MatchAllDocsQuery();
            }

        } catch (Exception e) {
            rsp.add("Error", "Error reading image from URL: " + paramUrl + ": " + e.getMessage());
            e.printStackTrace();
        }
        // search if the feature has been extracted and query is there.
        if (feat != null && query != null) {
            doSearch(req, rsp, req.getSearcher(), paramField, paramRows, getFilterQueries(req), query, feat);
        }
    }

    /**
     * Methods orders around the hashes already by docFreq removing those with docFreq == 0
     *
     * @param req
     * @param rsp
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void handleExtract(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, InstantiationException, IllegalAccessException {
        SolrParams params = req.getParams();
        String paramUrl = params.get("extract");
        String paramField = req.getParams().get("field", "cl_ha");
        if (!paramField.endsWith("_ha")) {
            paramField += "_ha";
        }
        useMetricSpaces = req.getParams().getBool("ms", DEFAULT_USE_METRIC_SPACES);
        double accuracy = req.getParams().getDouble("accuracy", DEFAULT_NUMBER_OF_QUERY_TERMS);
        GlobalFeature feat;
        // wrapping the whole part in the try
        try {
            if (!paramField.startsWith("sf")) {
                BufferedImage img = ImageIO.read(new URL(paramUrl).openStream());
                img = ImageUtils.trimWhiteSpace(img);
                // getting the right feature per field:
                if (FeatureRegistry.getClassForHashField(paramField) == null) {
                    feat = new ColorLayout();
                } else {
                    feat = (GlobalFeature) FeatureRegistry.getClassForHashField(paramField).newInstance();
                }
                feat.extract(img);
            } else {
                // we assume that this is a generic short feature, like it is used in context of deep features.
                feat = new ShortFeatureCosineDistance();
                String[] featureDoublesAsStrings = paramUrl.split(",");
                double[] featureDoubles = new double[featureDoublesAsStrings.length];
                for (int i = 0; i < featureDoubles.length; i++) {
                    featureDoubles[i] = Double.parseDouble(featureDoublesAsStrings[i]);
                }
                featureDoubles = Utilities.toCutOffArray(featureDoubles, EncodeAndHashCSV.TOP_N_CLASSES); // max norm
                short[] featureShort = Utilities.toShortArray(featureDoubles); // quantize
                ((ShortFeatureCosineDistance) feat).setData(featureShort);
            }
            rsp.add("histogram", Base64.encodeBase64String(feat.getByteArrayRepresentation()));
            if (!useMetricSpaces || true) { // select the most distinguishing hashes and deliver them back.
                HashTermStatistics.addToStatistics(req.getSearcher(), paramField);
                int[] hashes = BitSampling.generateHashes(feat.getFeatureVector());
                List<String> hashStrings = orderHashes(hashes, paramField, false);
                rsp.add("bs_list", hashStrings);
                List<String> hashQuery = orderHashes(hashes, paramField, true);
                int queryLength = (int) StatsUtils.clamp(accuracy * hashes.length,
                        3, hashQuery.size());
                rsp.add("bs_query", String.join(" ", hashQuery.subList(0, queryLength)));
            }
            if (MetricSpaces.supportsFeature(feat)) {
                rsp.add("ms_list", MetricSpaces.generateHashList(feat));
                int queryLength = (int) StatsUtils.clamp(accuracy * MetricSpaces.getPostingListLength(feat),
                        3, MetricSpaces.getPostingListLength(feat));
                rsp.add("ms_query", MetricSpaces.generateBoostedQuery(feat, queryLength));
            }
        } catch (Exception e) {
            rsp.add("Error", "Error reading image from URL: " + paramUrl + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Search based on the given image hashes.
     *
     * @param req
     * @param rsp
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void handleHashSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, IllegalAccessException, InstantiationException {
        SolrParams params = req.getParams();
        SolrIndexSearcher searcher = req.getSearcher();
        // get the params needed:
        // hashes=x y z ...
        // feature=<base64>
        // field=<cl_ha|ph_ha|...>

        byte[] featureVector = Base64.decodeBase64(params.get("feature"));
        String paramField = req.getParams().get("field", "cl_ha");

        // _ms is added automatically below and cause issues if kept here
        paramField = paramField.replace("_ms", "");

        if (!paramField.endsWith("_ha")) {
            paramField += "_ha";
        }
        int paramRows = params.getInt("rows", defaultNumberOfResults);
        numberOfQueryTerms = req.getParams().getDouble("accuracy", DEFAULT_NUMBER_OF_QUERY_TERMS);
        numberOfCandidateResults = req.getParams().getInt("candidates", DEFAULT_NUMBER_OF_CANDIDATES);
        useMetricSpaces = req.getParams().getBool("ms", DEFAULT_USE_METRIC_SPACES);

        // query feature
        GlobalFeature queryFeature = (GlobalFeature)
                FeatureRegistry.getClassForHashField(paramField).newInstance();
        queryFeature.setByteArrayRepresentation(featureVector);

        if (!useMetricSpaces) {
            HashTermStatistics.addToStatistics(req.getSearcher(), paramField); // caching the term statistics.
        }

        QueryParser qp = null;
        String queryString = null;
        if (params.get("hashes") == null) {
            // we have to create the hashes first ...
            if (!useMetricSpaces) {

            } else if (MetricSpaces.supportsFeature(queryFeature)) {
                int queryLength = (int) StatsUtils.clamp(numberOfQueryTerms * MetricSpaces.getPostingListLength(queryFeature),
                        3, MetricSpaces.getPostingListLength(queryFeature));
                queryString = MetricSpaces.generateBoostedQuery(queryFeature, queryLength);
            } else {
                queryString = "*:*";
            }
        } else {
            queryString = params.get("hashes").trim();
            if (!useMetricSpaces) {
                qp = new QueryParser(paramField, new WhitespaceAnalyzer());
            } else {
                qp = new QueryParser(paramField.replace("_ha", "_ms"), new WhitespaceAnalyzer());
            }
        }


        Query query = null;
        try {
            query = qp.parse(queryString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // get results:
        doSearch(req, rsp, searcher, paramField, paramRows, getFilterQueries(req), query, queryFeature);
    }

    /**
     * Actual search implementation based on (i) hash based retrieval and (ii) feature based re-ranking.
     *
     * @param req           the SolrQueryRequest
     * @param rsp           the response to write the data to
     * @param searcher      the actual index searcher object to search the index
     * @param hashFieldName the name of the field the hashes can be found
     * @param maximumHits   the maximum number of hits, the smaller the faster
     * @param filterQueries can be null
     * @param query         the (Boolean) query for querying the candidates from the IndexSearcher
     * @param queryFeature  the image feature used for re-ranking the results
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void doSearch(SolrQueryRequest req, SolrQueryResponse rsp, SolrIndexSearcher searcher, String hashFieldName,
                          int maximumHits, List<Query> filterQueries, Query query, GlobalFeature queryFeature)
            throws IOException, IllegalAccessException, InstantiationException {
        // temp feature instance
        GlobalFeature tmpFeature = queryFeature.getClass().newInstance();
        // Taking the time of search for statistical purposes.
        time = System.currentTimeMillis();

        String featureFieldName = FeatureRegistry.getFeatureFieldName(hashFieldName);
//        BinaryDocValues binaryValues = MultiDocValues.getBinaryValues(searcher.getIndexReader(), featureFieldName);
        BinaryDocValues binaryValues = new RandomAccessBinaryDocValues(() -> {
            try {
                return MultiDocValues.getBinaryValues(searcher.getIndexReader(), featureFieldName);
            } catch (IOException e) {
                throw new RuntimeException("BinaryDocValues problem.", e);
            }
        });

        time = System.currentTimeMillis() - time;
        rsp.add("DocValuesOpenTime", time + "");

        Iterator<Integer> docIterator;
        long numberOfResults = 0;
        time = System.currentTimeMillis();
        if (filterQueries != null) {
            DocList docList = searcher.getDocList(query, filterQueries, Sort.RELEVANCE, 0, numberOfCandidateResults, 0);
            numberOfResults = docList.size();
            docIterator = docList.iterator();
        } else {
            TopDocs docs = searcher.search(query, numberOfCandidateResults);
            numberOfResults = docs.totalHits.value;
            docIterator = new TopDocsIterator(docs);
        }
        time = System.currentTimeMillis() - time;
        rsp.add("RawDocsCount", numberOfResults + "");
        rsp.add("RawDocsSearchTime", time + "");
        time = System.currentTimeMillis();
        TreeSet<CachingSimpleResult> resultScoreDocs = getReRankedResults(
                docIterator, binaryValues, queryFeature, tmpFeature,
                maximumHits, searcher);

        // Creating response ...
        time = System.currentTimeMillis() - time;
        rsp.add("ReRankSearchTime", time + "");
        // replaced with SolrDocumentList for consistency.
//      LinkedList list = new LinkedList();
        SolrDocumentList list = new SolrDocumentList();
        for (CachingSimpleResult result : resultScoreDocs) {
            HashMap m = new HashMap(2);
            m.put("d", result.getDistance());
            // add fields as requested:
            if (req.getParams().get("fl") == null) {
                m.put("id", result.getDocument().get("id"));
                if (result.getDocument().get("title") != null) {
                    m.put("title", result.getDocument().get("title"));
                }
            } else {
                String fieldsRequested = req.getParams().get("fl");
                if (fieldsRequested.contains("score")) {
                    m.put("score", result.getDistance());
                }
                if (fieldsRequested.contains("*")) {
                    // all fields
                    for (IndexableField field : result.getDocument().getFields()) {
                        String tmpField = field.name();

                        if (result.getDocument().getFields(tmpField).length > 1) {
                            m.put(result.getDocument().getFields(tmpField)[0].name(), result.getDocument().getValues(tmpField));
                        } else if (result.getDocument().getFields(tmpField).length > 0) {
                            m.put(result.getDocument().getFields(tmpField)[0].name(), result.getDocument().getFields(tmpField)[0].stringValue());
                        }
                    }
                } else {
                    StringTokenizer st;
                    if (fieldsRequested.contains(",")) {
                        st = new StringTokenizer(fieldsRequested, ",");
                    } else {
                        st = new StringTokenizer(fieldsRequested, " ");
                    }
                    while (st.hasMoreElements()) {
                        String tmpField = st.nextToken();
                        if (result.getDocument().getFields(tmpField).length > 1) {
                            m.put(result.getDocument().getFields(tmpField)[0].name(), result.getDocument().getValues(tmpField));
                        } else if (result.getDocument().getFields(tmpField).length > 0) {
                            m.put(result.getDocument().getFields(tmpField)[0].name(), result.getDocument().getFields(tmpField)[0].stringValue());
                        }
                    }
                }
            }
//            m.put(field, result.getDocument().get(field));
//            m.put(field.replace("_ha", "_hi"), result.getDocument().getBinaryValue(field));
            //list.add(m);
            list.add(new SolrDocument(m));
        }

        // Format results to be similar to regular response
        rsp.add("response", list);
        //rsp.add("docs", list);
        // rsp.add("Test-name", "Test-val");
    }

    private TreeSet<CachingSimpleResult> getReRankedResults(
            Iterator<Integer> docIterator, BinaryDocValues binaryValues,
            GlobalFeature queryFeature, GlobalFeature tmpFeature,
            int maximumHits, IndexSearcher searcher) throws IOException {

        TreeSet<CachingSimpleResult> resultScoreDocs = new TreeSet<>();
        double maxDistance = -1f;
        double tmpScore;
        BytesRef bytesRef;
        CachingSimpleResult tmpResult;
        while (docIterator.hasNext()) {
            // using DocValues to retrieve the field values ...
            int doc = docIterator.next();

//            bytesRef = binaryValues.get(doc);
            bytesRef = getBytesRef(binaryValues, doc);
            tmpFeature.setByteArrayRepresentation(bytesRef.bytes, bytesRef.offset, bytesRef.length);
            // Getting the document from the index.
            // This is the slow step based on the field compression of stored fields.
//            tmpFeature.setByteArrayRepresentation(d.getBinaryValue(name).bytes, d.getBinaryValue(name).offset, d.getBinaryValue(name).length);
            tmpScore = queryFeature.getDistance(tmpFeature);
            if (resultScoreDocs.size() < maximumHits) {
                resultScoreDocs.add(new CachingSimpleResult(tmpScore, searcher.doc(doc), doc));
                maxDistance = resultScoreDocs.last().getDistance();
            } else if (tmpScore < maxDistance) {
                // if it is nearer to the sample than at least one of the current set:
                // remove the last one ...
                tmpResult = resultScoreDocs.last();
                resultScoreDocs.remove(tmpResult);
                // set it with new values and re-insert.
                tmpResult.set(tmpScore, searcher.doc(doc), doc);
                resultScoreDocs.add(tmpResult);
                // and set our new distance border ...
                maxDistance = resultScoreDocs.last().getDistance();
            }
        }
        return resultScoreDocs;
    }

    @Override
    public String getDescription() {
        return "LIRE Request Handler to add images to an index and search them. Search images by id, by url and by extracted features.";
    }

//    @Override
//    public String getSource() {
//        return "http://lire-project.net";
//    }
//
//    @Override
//    public NamedList<Object> getStatistics() {
//        // Change stats here to get an insight in the admin console.
//        NamedList<Object> statistics = super.getStatistics();
//        statistics.add("Number of Requests", countRequests);
//        return statistics;
//    }

    /**
     * Makes a Boolean query out of a list of hashes by ordering them ascending using their docFreq and
     * then only using the most distinctive ones, defined by size in [0.1, 1], size=1 takes all.
     *
     * @param hashes
     * @param paramField
     * @param size       in [0.1, 1]
     * @return
     */
    private BooleanQuery createQuery(int[] hashes, String paramField, double size) {
        size = Math.max(0.1, Math.min(size, 1d)); // clamp size.
        List<String> hList = orderHashes(hashes, paramField, true);
        int numHashes = (int) Math.min(hList.size(), Math.floor(hashes.length * size));
        // a minimum of 3 hashes ...
        if (numHashes < 3) {
            numHashes = 3;
        }

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        for (int i = 0; i < numHashes; i++) {
            // be aware that the hashFunctionsFileName of the field must match the one you put the hashes in before.
            queryBuilder.add(new BooleanClause(new TermQuery(new Term(paramField, hList.get(i))), BooleanClause.Occur.SHOULD));
        }
        // this query is just for boosting the results with more matching hashes. We'd need to match it to all docs.
        //queryBuilder.add(new BooleanClause(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD));
        BooleanQuery query = queryBuilder.build();
        return query;
    }

    /**
     * Sorts the hashes to put those first, that do not show up in a large number of documents
     * while deleting those that are not in the index at all. Meaning: terms sorted by docFreq ascending, removing
     * those with docFreq == 0
     *
     * @param hashes                 the int[] of hashes
     * @param paramField             the field in the index.
     * @param removeZeroDocFreqTerms
     * @return
     */
    private List<String> orderHashes(int[] hashes, String paramField, boolean removeZeroDocFreqTerms) {
        List<String> hList = new ArrayList<>(hashes.length);
        // creates a list of terms.
        for (int hashe : hashes) {
            hList.add(Integer.toHexString(hashe));
        }
        // uses our predetermined hash term stats object to sort the list
        Collections.sort(hList, (o1, o2) -> HashTermStatistics.docFreq(paramField, o1) - HashTermStatistics.docFreq(paramField, o2));
        // removing those with zero entries but leaving at least three.
        while (HashTermStatistics.docFreq(paramField, hList.get(0)) < 1 && hList.size() > 3) {
            hList.remove(0);
        }
        return hList;
    }

    /**
     * This is used to create a TermsFilter ... should be used to select in the index based on many terms.
     * We just need to integrate a minimum query too, else we'd not get the appropriate results.
     * TODO: This is wrong.
     *
     * @param hashes
     * @param paramField
     * @return
     */
    private List<Term> createTermFilter(int[] hashes, String paramField, double size) {
        List<String> hList = new ArrayList<>(hashes.length);
        // creates a list of terms.
        for (int hashe : hashes) {
            hList.add(Integer.toHexString(hashe));
        }
        // uses our predetermined hash term stats object to sort the list
        Collections.sort(hList, (o1, o2) -> HashTermStatistics.docFreq(paramField, o1) - HashTermStatistics.docFreq(paramField, o2));
        // removing those with zero entries but leaving at least three.
        while (HashTermStatistics.docFreq(paramField, hList.get(0)) < 1 && hList.size() > 3) {
            hList.remove(0);
        }
        int numHashes = (int) Math.min(hList.size(), Math.floor(hashes.length * size));
        // a minimum of 3 hashes ...
        if (numHashes < 3) {
            numHashes = 3;
        }
        LinkedList<Term> termFilter = new LinkedList<Term>();
        for (int i = 0; i < numHashes; i++) {
            // be aware that the hashFunctionsFileName of the field must match the one you put the hashes in before.
            termFilter.add(new Term(paramField, Integer.toHexString(hashes[i])));
        }
        return termFilter;
    }

    private BytesRef getBytesRef(BinaryDocValues bdv, int docId)
            throws IOException {
        if (bdv != null && bdv.advance(docId) == docId) {
//        if (bdv != null && bdv.docID() < docId && bdv.advance(docId) == docId) {
//        if (bdv != null && bdv.advanceExact(docId)) {
            return bdv.binaryValue();
        }
        return new BytesRef(BytesRef.EMPTY_BYTES);
    }
}
