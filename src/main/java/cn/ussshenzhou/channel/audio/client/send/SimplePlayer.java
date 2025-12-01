package cn.ussshenzhou.channel.audio.client.send;

import cn.ussshenzhou.channel.util.ModConstant;
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

    public static void play(@Nullable byte[] audio, int sampleRate) {
        if (!Minecraft.getInstance().isSameThread()) {
            LogUtils.getLogger().error("Must call this on main thread.");
        }
        if (audioFormat == null || audioFormat.getFrameRate() != sampleRate) {
            audioFormat = new AudioFormat(sampleRate, ModConstant.MIC_SAMPLE_BITS, ModConstant.MIC_CHANNEL, true, false);
            if (line == null) {
                initLine();
            }
            line.stop();
            line.close();
            initLine();
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

    private static void initLine() {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        line.start();
    }

    public static void flush() {
        if (line != null) {
            line.flush();
        }
    }
}
