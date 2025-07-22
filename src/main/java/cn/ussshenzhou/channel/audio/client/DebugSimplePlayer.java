package cn.ussshenzhou.channel.audio.client;

import javax.sound.sampled.*;

/**
 * @author USS_Shenzhou
 */
public class DebugSimplePlayer {
    private static SourceDataLine line = null;

    public static void play(byte[] audio, AudioFormat format) {
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
        line.write(audio, 0, audio.length);
    }
}
