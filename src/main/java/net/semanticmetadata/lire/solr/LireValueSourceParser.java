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

import org.apache.commons.codec.binary.Base64;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

/**
 * <p>A query function for sorting results based on the LIRE CBIR functions.
 * Implementation based partially on the outdated guide given on http://www.supermind.org/blog/756,
 * comments on the mailing list provided from Chris Hostetter, and the 4.4 Solr and Lucene source. </p>
 *
 * <p>Do not forget to add the function parser to the lsolrconfig.xml file like this:<br>
 * &lt;valueSourceParser name="lirefunc" class="net.semanticmetadata.lire.solr.LireValueSourceParser" /&gt;</p>
 *
 * To use the function getting a distance to a reference image use it like:<br>
 * <pre>http://localhost:9000/solr/lire/select?q=*:*&amp;fl=id,lirefunc(cl,"FQY5DhMYDg0ODg0PEBEPDg4ODg8QEgsgEBAQEBAgEBAQEBA%3D")</pre>
 * The first parameter gives the field (cl, ph, eh, or jc), the second gives the byte[] representation of the
 * histogram in Base64 encoding
 *
 * @author Mathias Lux, mathias@juggle.at, 17.09.2013
 */
public class LireValueSourceParser extends ValueSourceParser {
    public void init(NamedList namedList) {

    }

    @Override
    public ValueSource parse(FunctionQParser fp) throws SyntaxError {
        String field=fp.parseArg();                          // eg. cl_hi
        String featureString = fp.parseArg();
        // System.out.println(featureString);
        byte[] hist= Base64.decodeBase64(featureString);     // eg. FQY5DhMYDg0ODg0PEBEPDg4ODg8QEgsgEBAQEBAgEBAQEBA=
        double maxDistance = Double.MAX_VALUE;
        if (fp.hasMoreArguments()) {                           // if there is a third argument, it's the max value to return if there is none. Note the query cache is not updated upon parameter change.
            maxDistance = Double.parseDouble(fp.parseArg());
        }
        return new LireValueSource(field, hist, maxDistance);
    }
}
