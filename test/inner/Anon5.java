package inner;

public class Anon5 {
    final static String v15 = "Anon5";
    final static String v14 = "Anon5";

    {
        new Object() {
            final static String v15 = "Anon5$3";
            final static String v14 = "Anon5$1";
        };
    }

    static {
        new Object() {
            final static String v15 = "Anon5$3";
            final static String v14 = "Anon5$1";
        };
    }

    void instanceMethod1() {
        new Object() {
            {
                new Object() {
                    final static String v15 = "Anon5$1A3$1";
                    final static String v14 = "Anon5$11";
                };
            }
            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=167357
            final static String v15 = "Anon5$1A3";
            final static String v14 = "Anon5$1$A3";
        };
    }

    void instanceMethod2() {
        class A3 {
            {
                new Object() {
                    final static String v15 = "Anon5$1A3$1";
                    final static String v14 = "Anon5$5";
                };
            }
            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=167357
            final static String v15 = "Anon5$1A3";
            final static String v14 = "Anon5$1$A3";
        }
    }

    class A1 {
        final static String v15 = "Anon5$1A3";
        final static String v14 = "Anon5$1$A3";

        {
            new Object() {
                final static String v15 = "Anon5$1A3$1";
                final static String v14 = "Anon5$5";
            };
        }
    }

    static class A2 {
        final static String v15 = "Anon5$1A3";
        final static String v14 = "Anon5$1$A3";

        {
            new Object() {
                final static String v15 = "Anon5$1A3$1";
                final static String v14 = "Anon5$5";
            };
        }

        static {
            new Object() {
                final static String v15 = "Anon5$1A3$1";
                final static String v14 = "Anon5$5";
            };
        }
    }

}
