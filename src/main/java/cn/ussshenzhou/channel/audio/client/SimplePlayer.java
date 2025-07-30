package cn.ussshenzhou.channel.audio.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.*;

/**
 * @author USS_Shenzhou
 */
public class SimplePlayer {
    private static SourceDataLine line = null;

    public static void play(byte[] audio, AudioFormat format) {
        if (!Minecraft.getInstance().isSameThread()) {
            LogUtils.getLogger().error("Must call this on main thread.");
        }
        if (line == null) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            try {
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
            } catch (LineUnavailableException e) {
                throw new RuntimeException(e);
            }
            line.start();
        }
        if (audio == null) {
            line.flush();
            return;
        }
        if (line.available() < audio.length / 2) {
            line.flush();
        }
        line.write(audio, 0, audio.length);
    }
}
