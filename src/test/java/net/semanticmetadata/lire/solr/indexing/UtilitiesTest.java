package net.semanticmetadata.lire.solr.indexing;

import junit.framework.TestCase;
import net.semanticmetadata.lire.solr.tools.Utilities;

import java.util.Arrays;

public class UtilitiesTest extends TestCase {
    public void testNormalize() {
        double[] d = {1.0, 2.0, 3.0, 4.0, 5.0};
        d = Utilities.normalize(d);
        System.out.println(Arrays.toString(d));

        d = new double[]{-1.0, 2.0, 3.0, 4.0, -5.0};
        d = Utilities.normalize(d);
        System.out.println(Arrays.toString(d));
    }

    public void testQuantizeToShort() {
        double[] d = {1.0, 2.0, 3.0, 4.0, 5.0};
        d = Utilities.normalize(d);
        System.out.println(Arrays.toString(Utilities.quantizeToShort(d)));

        d = new double[]{-1.0, 2.0, 3.0, 4.0, -5.0};
        d = Utilities.normalize(d);
        System.out.println(Arrays.toString(Utilities.quantizeToShort(d)));
    }
}
