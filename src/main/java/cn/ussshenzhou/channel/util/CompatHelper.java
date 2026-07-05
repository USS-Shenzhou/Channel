package cn.ussshenzhou.channel.util;

public class CompatHelper {

    public static final Class<?> MEK_BLOCK_CLASS;

    static {
        Class<?> mek;
        try {
            mek = Class.forName("mekanism.common.block.BlockMekanism");
        } catch (ClassNotFoundException e) {
            mek = null;
        }
        MEK_BLOCK_CLASS = mek;
    }
}
