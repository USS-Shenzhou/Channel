package cn.ussshenzhou.channel.audio.nativ;

import cn.ussshenzhou.channel.audio.client.send.MicManager;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 * @author USS_Shenzhou
 */
public class NvidiaInit {
    private static String dllPath = null;

    protected static void checkRequire() {
        if (MicManager.getSampleRate() == 8000) {
            NvidiaHelper.stat = NvidiaHelper.Stat.CHANGE_SAMPLE_RATE;
            return;
        }
        var os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("windows")) {
            NvidiaHelper.stat = NvidiaHelper.Stat.UNSUPPORTED_OS;
            return;
        }
        var gpudevice = RenderSystem.getDevice();
        if (!gpudevice.getRenderer().contains("RTX")) {
            NvidiaHelper.stat = NvidiaHelper.Stat.UNSUPPORTED_GPU;
            return;
        }
        var splitDriver = gpudevice.getVersion().split(" ");
        var driver = splitDriver[splitDriver.length - 1];
        int version = Integer.parseInt(driver.split("\\.")[0]);
        if (version < 570) {
            NvidiaHelper.stat = NvidiaHelper.Stat.UNSUPPORTED_DRIVER;
            return;
        }
        NvidiaHelper.stat = NvidiaHelper.Stat.OK;
    }

    protected static void tryLoadDll() {
        if (!loadFromSDK()
                && !loadFromConfig()
                && !loadFromSystem()
                && !loadFromGuess()) {
            NvidiaHelper.stat = NvidiaHelper.Stat.NEED_DOWNLOAD;
        }
    }

    private static boolean loadFromSystem() {
        try {
            System.loadLibrary("NVAudioEffects");
            var module = Kernel32.INSTANCE.GetModuleHandle("NvAudioEffects.dll");
            if (module != null) {
                var buf = new char[1024];
                // NEEDTEST
                int len = ExtraKernel32.INSTANCE.GetModuleFileNameW(module, buf, buf.length);
                if (len > 0) {
                    dllPath = Paths.get(Native.toString(buf)).toAbsolutePath().normalize().toString();
                }
            }
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    private static final ArrayList<String> OTHER_SDK = new ArrayList<>() {{
        add("C:\\ProgramData\\NVIDIA\\NGX\\models\\nvbcast");
    }};

    private static boolean loadFromGuess() {
        ArrayList<String> result = new ArrayList<>();
        OTHER_SDK.forEach(dir -> {
            var root = Paths.get(dir);
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override
                    public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                        if ("NVAudioEffects.dll".equalsIgnoreCase(file.getFileName().toString())) {
                            result.add(file.toAbsolutePath().toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception ignored) {
            }
        });
        result.sort((a, b) -> {
            try {
                long t1 = Files.getLastModifiedTime(Path.of(b)).toMillis();
                long t2 = Files.getLastModifiedTime(Path.of(a)).toMillis();
                return Long.compare(t1, t2);
            } catch (IOException e) {
                return 0;
            }
        });
        for (var path : result) {
            if (loadFromPath(path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean loadFromConfig() {
        var path = ChannelClientConfig.get().nvidiaDllPath;
        if (!path.isBlank()) {
            return loadFromPath(path);
        }
        return false;
    }

    private static boolean loadFromPath(String path) {
        try {
            if (!Files.exists(Paths.get(path))) {
                return false;
            }
            // flag 0x8: LOAD_WITH_ALTERED_SEARCH_PATH
            Kernel32.INSTANCE.LoadLibraryEx(path, null, 0x8);
            System.load(path);
            dllPath = path;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    private static boolean loadFromSDK() {
        return loadFromPath("C:\\Program Files\\NVIDIA Corporation\\NVIDIA Audio Effects SDK\\NVAudioEffects.dll");
    }

    public interface ExtraKernel32 extends Library {
        ExtraKernel32 INSTANCE = Native.load(
                "kernel32",
                ExtraKernel32.class,
                W32APIOptions.DEFAULT_OPTIONS
        );

        // DWORD GetModuleFileNameW(HMODULE hModule, LPWSTR lpFilename, DWORD nSize);
        @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
        int GetModuleFileNameW(WinDef.HMODULE hModule, char[] lpFilename, int nSize);
    }
}
