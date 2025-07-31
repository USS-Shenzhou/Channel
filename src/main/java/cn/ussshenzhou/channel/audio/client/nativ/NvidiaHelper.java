package cn.ussshenzhou.channel.audio.client.nativ;

/**
 * @author USS_Shenzhou
 */
public class NvidiaHelper {
    private static Stat stat;

    public static void init() {
        
    }

    public static Stat getStat() {
        return Stat.OK;
    }

    public enum Stat {
        OK,
        NEED_DOWNLOAD,
        UNSUPPORTED_OS,
        UNSUPPORTED_DRIVER,
        UNSUPPORTED_GPU
    }
}
