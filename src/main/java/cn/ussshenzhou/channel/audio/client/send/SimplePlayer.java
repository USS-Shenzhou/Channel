package cn.ussshenzhou.channel.audio.client.send;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import javax.sound.sampled.*;

/**
 * @author USS_Shenzhou
 */
public class SimplePlayer {
    private static SourceDataLine line = null;
    private static AudioFormat audioFormat = null;

    public static void play(@Nullable byte[] audio, AudioFormat format) {
        if (!Minecraft.getInstance().isSameThread()) {
            LogUtils.getLogger().error("Must call this on main thread.");
        }
        if (line == null) {
            initLine(format);
        }
        if (!audioFormat.equals(format)) {
            line.stop();
            line.close();
            initLine(format);
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

    private static void initLine(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        line.start();
        audioFormat = format;
    }

    public static void flush() {
        if (line != null) {
            line.flush();
        }
    }
}
