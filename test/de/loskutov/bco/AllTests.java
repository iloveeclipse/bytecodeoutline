package de.loskutov.bco;

import de.loskutov.bco.ui.TestJdk14Compatibility;
import de.loskutov.bco.ui.TestJdk15Compatibility;
import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for de.loskutov.bco");
        //$JUnit-BEGIN$
        suite.addTestSuite(TestJdk14Compatibility.class);
        suite.addTestSuite(TestJdk15Compatibility.class);
        //$JUnit-END$
        return suite;
    }

}
