package de.loskutov.bco.asm;


public class JavaVersion {

    private final int version;
    public final int major;
    public final int minor;

    public JavaVersion(int version) {
        this.version = version;
        major = version & 0xFFFF;
        minor = version >>> 16;
    }

    public String humanReadable() {
        // 1.1 is 45, 1.2 is 46 etc.
        int javaV = major % 44;
        String javaVersion;
        if (javaV > 0) {
            if(javaV > 8) {
                javaVersion = javaV + "." + minor;
            } else {
                javaVersion = "1." + javaV;
            }
        } else {
            javaVersion = "? " + major;
        }
        return javaVersion;
    }
}
