package cn.ussshenzhou.channel.audio.client.send.nativ;

import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.client.send.MicManager;
import cn.ussshenzhou.channel.audio.client.send.MicReader;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.ArrayHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.sun.jna.platform.win32.Kernel32;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Paths;

import static cn.ussshenzhou.channel.audio.client.send.nativ.NvAudioEffects.*;
import static java.lang.foreign.MemorySegment.NULL;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("preview")
public class NvidiaHelper {
    private static Stat stat;

    public static void init() {
        checkRequire();
        tryLoadDll();
        refresh();
    }

    private static void checkRequire() {
        if (MicManager.getSampleRate() == 8000) {
            stat = Stat.CHANGE_SAMPLE_RATE;
            return;
        }
        var os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("windows")) {
            stat = Stat.UNSUPPORTED_OS;
            return;
        }
        var gpudevice = RenderSystem.getDevice();
        if (!gpudevice.getRenderer().contains("RTX")) {
            stat = Stat.UNSUPPORTED_GPU;
            return;
        }
        var splitDriver = gpudevice.getVersion().split(" ");
        var driver = splitDriver[splitDriver.length - 1];
        int version = Integer.parseInt(driver.split("\\.")[0]);
        if (version < 570) {
            stat = Stat.UNSUPPORTED_DRIVER;
            return;
        }
        stat = Stat.OK;
    }

    private static void tryLoadDll() {
        var path = "C:\\Program Files\\NVIDIA Corporation\\NVIDIA Audio Effects SDK\\NVAudioEffects.dll";
        var dllPath = Paths.get(path);
        if (!Files.exists(dllPath)) {
            stat = Stat.NEED_DOWNLOAD;
            return;
        }
        // flag 0x8: LOAD_WITH_ALTERED_SEARCH_PATH
        Kernel32.INSTANCE.LoadLibraryEx("C:\\Program Files\\NVIDIA Corporation\\NVIDIA Audio Effects SDK\\NVAudioEffects.dll", null, 0x8);
        System.load(path);
    }

    public static Stat getStat() {
        return stat;
    }

    public enum Stat {
        OK,
        NEED_DOWNLOAD,
        UNSUPPORTED_OS,
        UNSUPPORTED_GPU,
        UNSUPPORTED_DRIVER,
        CHANGE_SAMPLE_RATE,
        EXCEPTION
    }

    private static MemorySegment nvAFXHandle = NULL;

    public static void refresh() {
        synchronized (NvidiaHelper.class) {
            if (stat != Stat.OK) {
                return;
            }
            if (nvAFXHandle != NULL) {
                NvAFX_DestroyEffect(nvAFXHandle);
                nvAFXHandle = NULL;
            }
            var cfg = ChannelClientConfig.get();
            boolean denoise = cfg.noiseCanceling == NC.AI;
            boolean dereverb = cfg.echoCanceling;
            if (!denoise) {
                return;
            }
            var effect = dereverb ? NVAFX_EFFECT_DEREVERB_DENOISER : NVAFX_EFFECT_DENOISER;
            try (Arena arena = Arena.ofConfined()) {
                var handle = arena.allocate(ValueLayout.ADDRESS);
                checkStatus(NvAFX_CreateEffect(effect, handle), "NvAFX_CreateEffect");
                nvAFXHandle = handle.get(ValueLayout.ADDRESS, 0);
                int sampleRate = MicManager.getSampleRate();
                var modelPath = new StringBuilder("C:\\Program Files\\NVIDIA Corporation\\NVIDIA Audio Effects SDK\\models\\");
                if (dereverb) {
                    modelPath.append("dereverb_");
                }
                modelPath.append("denoiser_");
                switch (sampleRate) {
                    case 16000 -> modelPath.append("16k");
                    case 48000 -> modelPath.append("48k");
                    default -> {
                        stat = Stat.EXCEPTION;
                        return;
                    }
                }
                modelPath.append(".trtpkg");
                checkStatus(NvAFX_SetString(nvAFXHandle, NVAFX_PARAM_MODEL_PATH, arena.allocateUtf8String(modelPath.toString())), "NvAFX_SetString");
                checkStatus(NvAFX_SetFloat(nvAFXHandle, NVAFX_PARAM_INTENSITY_RATIO, cfg.aiNoiseCancelingRatio), "NvAFX_SetFloat");
                checkStatus(NvAFX_Load(nvAFXHandle), "NvAFX_Load");
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void checkStatus(int returnValue, String call) throws RuntimeException {
        var status = NvAFX_Status.values()[returnValue];
        if (status != NvAFX_Status.NVAFX_STATUS_SUCCESS) {
            LogUtils.getLogger().error(call + " failed with status " + status.name());
            nvAFXHandle = NULL;
            stat = Stat.EXCEPTION;
            //TODO GUI notify
            throw new RuntimeException();
        }
    }

    public static byte[] process(byte[] raw) {
        synchronized (NvidiaHelper.class) {
            if (nvAFXHandle == NULL) {
                return raw;
            }
            try (Arena arena = Arena.ofConfined()) {
                var in = arena.allocateArray(ValueLayout.JAVA_FLOAT, ArrayHelper.castS2F(ArrayHelper.reinterpretB2S(raw)));
                var out = arena.allocate(in.byteSize());
                var seg = MicReader.getFrameLength() / 10;
                var stepLength = raw.length / 2 / seg;
                for (int i = 0; i < seg; i++) {
                    var inSlice = in.asSlice(i * stepLength * 4L, stepLength * 4L);
                    var outSlice = out.asSlice(i * stepLength * 4L, stepLength * 4L);
                    var inPtr = arena.allocateArray(ValueLayout.ADDRESS, 1);
                    var outPtr = arena.allocateArray(ValueLayout.ADDRESS, 1);
                    inPtr.setAtIndex(ValueLayout.ADDRESS, 0, inSlice);
                    outPtr.setAtIndex(ValueLayout.ADDRESS, 0, outSlice);
                    checkStatus(NvAFX_Run(nvAFXHandle, inPtr, outPtr, stepLength, 1), "NvAFX_Run");
                }
                return ArrayHelper.reinterpretS2B(ArrayHelper.castF2S(out.toArray(ValueLayout.JAVA_FLOAT)));
            } catch (RuntimeException ignored) {
            }
            return raw;
        }
    }
}
