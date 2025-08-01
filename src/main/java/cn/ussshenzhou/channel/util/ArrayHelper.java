package cn.ussshenzhou.channel.util;

import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("preview")
public class ArrayHelper {

    public static short[] castB2S(byte[] array) {
        var result = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (short) (array[i] / 127f * 32767);
        }
        return result;
    }

    public static byte[] castS2B(short[] array) {
        var result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (byte) (array[i] / 32768f * 127);
        }
        return result;
    }

    public static float[] castB2F(byte[] array) {
        var result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] / 127f;
        }
        return result;
    }

    public static byte[] castF2B(float[] array) {
        var result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (byte) (array[i] * 127f);
        }
        return result;
    }

    public static float[] castS2F(short[] array) {
        var result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] / 32768f;
        }
        return result;
    }

    public static short[] castF2S(float[] array) {
        var result = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (short) (array[i] * 32767f);
        }
        return result;
    }

    public static short[] reinterpretB2S(byte[] array) {
        var result = new short[array.length / 2];
        ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(result);
        return result;
    }

    public static byte[] reinterpretS2B(short[] array) {
        var result = new byte[array.length * 2];
        ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(array);
        return result;
    }

    public static float[] reinterpretB2F(byte[] array) {
        var result = new float[array.length / 4];
        ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(result);
        return result;
    }

    public static byte[] reinterpretF2B(float[] array) {
        var result = new byte[array.length * 4];
        ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(array);
        return result;
    }

    public static MemorySegment heap2DirectAlloc(byte[] array) {
        var result = MemoryUtil.memAlloc(array.length);
        result.put(array).flip();
        return MemorySegment.ofBuffer(result);
    }

    public static MemorySegment heap2DirectAlloc(short[] array) {
        var result = MemoryUtil.memAlloc(array.length * 2);
        result.asShortBuffer().put(array).flip();
        return MemorySegment.ofBuffer(result);
    }

    public static MemorySegment heap2DirectAlloc(float[] array) {
        var result = MemoryUtil.memAlloc(array.length * 4);
        result.asFloatBuffer().put(array).flip();
        return MemorySegment.ofBuffer(result);
    }

    public static byte[] direct2HeapFreeB(MemorySegment segment) {
        var result = segment.toArray(ValueLayout.JAVA_BYTE);
        MemoryUtil.memFree(segment.asByteBuffer());
        return result;
    }

    public static short[] direct2HeapFreeS(MemorySegment segment) {
        var result = segment.toArray(ValueLayout.JAVA_SHORT);
        MemoryUtil.memFree(segment.asByteBuffer());
        return result;
    }

    public static float[] direct2HeapFreeF(MemorySegment segment) {
        var result = segment.toArray(ValueLayout.JAVA_FLOAT);
        MemoryUtil.memFree(segment.asByteBuffer());
        return result;
    }

    public static short absAverage(short[] array) {
        double sum = 0;
        for (short i : array) {
            sum += Math.abs(i);
        }
        return (short) (sum / array.length);
    }

    public static short absAverage(Collection<Short> array) {
        double sum = 0;
        for (short i : array) {
            sum += Math.abs(i);
        }
        return (short) (sum / array.size());
    }
}
