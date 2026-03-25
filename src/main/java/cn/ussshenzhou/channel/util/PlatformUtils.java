package cn.ussshenzhou.channel.util;

/**
 * @author USS_Shenzhou
 */
public class PlatformUtils {

    public static String getOS() {
        String osName = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        String osArch = System.getProperty("os.arch").toLowerCase(java.util.Locale.ROOT);

        String os;
        if (osName.contains("win")) {
            os = "windows";
        } else if (osName.contains("mac")) {
            os = "macos";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            os = "linux";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        String arch;
        if ("amd64".equals(osArch) || "x86_64".equals(osArch)) {
            arch = "x86_64";
        } else if ("aarch64".equals(osArch)) {
            arch = "aarch64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + osArch);
        }

        return os + "-" + arch;
    }

    public static String getLibSuffix() {
        String osName = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);

        if (osName.contains("win")) {
            return ".dll";
        } else if (osName.contains("mac")) {
            return ".dylib";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return ".so";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }
    }
}