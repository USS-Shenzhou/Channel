package cn.ussshenzhou.channel.mixin;

import cn.ussshenzhou.channel.audio.client.send.WebRTCHelper;
import cn.ussshenzhou.channel.util.PlatformUtils;
import com.mojang.logging.LogUtils;
import dev.onvoid.webrtc.internal.NativeLoader;
import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author USS_Shenzhou
 */
@Mixin(NativeLoader.class)
public class NativeLoaderMixin {

    @Inject(method = "loadLibrary", at = @At(value = "HEAD"), cancellable = true)
    private static void channelCancelLoad(String libName, CallbackInfo ci) {
        if (SharedConstants.IS_RUNNING_WITH_JDWP) {
            LogUtils.getLogger().warn("We are in a dev env now. Native things may work differently.");
            return;
        }
        loadNativeInternal(PlatformUtils.getOS(), PlatformUtils.getLibSuffix());
        ci.cancel();
    }

    @Unique
    private static void loadNativeInternal(String platform, String fileSuffix) {
        var jarJarPath = "/META-INF/jarjar/webrtc-java-0.14.0-" + platform + ".jar";
        var libName = "webrtc-java-" + platform + fileSuffix;
        LogUtils.getLogger().info("Loading " + libName + " from " + jarJarPath);
        try (var resourceStream = WebRTCHelper.class.getResourceAsStream(jarJarPath)) {
            if (resourceStream == null) {
                throw new RuntimeException("Mod Jar is not complete.");
            }
            try (var zipStream = new ZipInputStream(resourceStream)) {
                ZipEntry entry;
                boolean found = false;
                while ((entry = zipStream.getNextEntry()) != null) {
                    found = findAndLoad(fileSuffix, entry, libName, zipStream, found);
                    zipStream.closeEntry();
                }
                if (!found) {
                    throw new RuntimeException("Failed to extract and load WebRTC. Something went wrong.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load WebRTC lib.", e);
        }
        LogUtils.getLogger().info("Successfully loaded WebRTC lib.");
    }

    @Unique
    private static boolean findAndLoad(String fileSuffix, ZipEntry entry, String libName, ZipInputStream zipStream, boolean found) throws IOException {
        if (entry.getName().equals(libName)) {
            var tempLib = Files.createTempFile("Channel_WebRTC_", fileSuffix);
            tempLib.toFile().deleteOnExit();
            Files.copy(zipStream, tempLib, StandardCopyOption.REPLACE_EXISTING);
            System.load(tempLib.toAbsolutePath().toString());
            found = true;
        }
        return found;
    }
}
