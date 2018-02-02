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

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.util.Base64;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.uninverting.UninvertingReader;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Base64 -&gt; DocValues implementation used for the Solr Plugin. Using this field one can index byte[] values by
 * sending them to Solr base64 encoded. In case of the LIRE plugin, the fields get read linearly, so they need to be
 * extremely fast, which is the case with the DocValues.
 * @author Mathias Lux, mathias@juggle.at, 12.08.2013
 */
public class BinaryDocValuesField extends FieldType {

    private String toBase64String(ByteBuffer buf) {
        return Base64.byteArrayToBase64(buf.array(), buf.position(), buf.limit() - buf.position());
    }

    @Override
    public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException {
        writer.writeStr(name, toBase64String(toObject(f)), false);
    }

    @Override
    public SortField getSortField(SchemaField field, boolean top) {
        throw new RuntimeException("Cannot sort on a Binary field");
    }


    @Override
    public String toExternal(IndexableField f) {
        return toBase64String(toObject(f));
    }

    @Override
    public ByteBuffer toObject(IndexableField f) {
        BytesRef bytes = f.binaryValue();
        if (bytes != null) {
            return  ByteBuffer.wrap(bytes.bytes, bytes.offset, bytes.length);
        }
        return ByteBuffer.allocate(0);
    }

    @Override
    public UninvertingReader.Type getUninversionType(SchemaField sf) {
        throw new UnsupportedOperationException("Not Implemented!");
        // return null;
    }

    @Override
    public IndexableField createField(SchemaField field, Object val /*, float boost*/) {
        if (val == null) return null;
        if (!field.stored()) {
            return null;
        }
        byte[] buf = null;
        int offset = 0, len = 0;
        if (val instanceof byte[]) {
            buf = (byte[]) val;
            len = buf.length;
        } else if (val instanceof ByteBuffer && ((ByteBuffer)val).hasArray()) {
            ByteBuffer byteBuf = (ByteBuffer) val;
            buf = byteBuf.array();
            offset = byteBuf.position();
            len = byteBuf.limit() - byteBuf.position();
        } else {
            String strVal = val.toString();
            //the string has to be a base64 encoded string
            buf = Base64.base64ToByteArray(strVal);
            offset = 0;
            len = buf.length;
        }

        Field f = new org.apache.lucene.document.BinaryDocValuesField(field.getName(), new BytesRef(buf, offset, len));
//        Field f = new org.apache.lucene.document.StoredField(field.getName(), buf, offset, len);
        //f.setBoost(boost);
        return f;
    }
}
