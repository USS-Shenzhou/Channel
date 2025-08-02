package cn.ussshenzhou.channel.util;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.sounds.AudioStream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DumbAudioStream implements AudioStream {
    private final AudioFormat format;
    public static final DumbAudioStream INSTANCE = new DumbAudioStream();

    public DumbAudioStream() {
        this.format = new AudioFormat(16000, 16, 1, true, false);
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int wanted) {
        return ByteBuffer.allocate(0);
    }

    @Override
    public void close() {
    }
}
