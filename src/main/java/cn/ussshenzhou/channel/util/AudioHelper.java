package cn.ussshenzhou.channel.util;

import net.minecraft.client.Minecraft;

/**
 * @author USS_Shenzhou
 */
public class AudioHelper {

    public static float s2dbfs(short value) {
        return (float) (20 * Math.log10((value + 1) / 32768f));
    }

    public static float db2factor(float db) {
        return (float) Math.pow(10, db / 20);
    }

    public static float factor2db(float factor) {
        return (float) (20 * Math.log10(factor + 0.000001));
    }

    public static void onSoundThread(Runnable r) {
        Minecraft.getInstance().getSoundManager().soundEngine.executor.execute(r);
    }
}
