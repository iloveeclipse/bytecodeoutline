package de.loskutov.bco.ui;


public class TestJdk15Compatibility extends TestJdtUtils {

    public void testGetNamed1() throws Exception {
        doTest("Anon1");
    }

    public void testGetNamed1_1() throws Exception {
        doTest("Anon1_1");
    }

    public void testGetNamed2() throws Exception {
        doTest("Anon2");
    }

    public void testGetNamed3() throws Exception {
        doTest("Anon3");
    }

    public void testGetNamed3_3() throws Exception {
        doTest("Anon3_3");
    }

    public void testGetNamed4() throws Exception {
        doTest("Anon4");
    }

    public void testGetNamed5() throws Exception {
        doTest("Anon5");
    }

    protected String getJdkVersion() {
        return "1.5";
    }

    protected String getFieldName() {
        return "v15";
    }

}
