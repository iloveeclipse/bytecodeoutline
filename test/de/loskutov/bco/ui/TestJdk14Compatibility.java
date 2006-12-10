package de.loskutov.bco.ui;


public class TestJdk14Compatibility extends TestJdtUtils {

    public void testGetNamed1() throws Exception {
        doTest("Anon1");
    }

    public void testGetNamed2() throws Exception {
        doTest("Anon2");
    }

    public void testGetNamed3() throws Exception {
        doTest("Anon3");
    }

    public void testGetNamed4() throws Exception {
        doTest("Anon4");
    }

    protected String getJdkVersion() {
        return "1.4";
    }

    protected String getFieldName() {
        return "v14";
    }

}
