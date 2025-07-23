package cn.ussshenzhou.channel.c;

import com.mojang.logging.LogUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * @author USS_Shenzhou
 */
public class NativeLoader {
    public static void loadRnnoise() {
        //FIXME
        String os = System.getProperty("os.name").toLowerCase();
        String sub = os.contains("win") ? "windows" : os.contains("mac") ? "macos" : "linux";
        String libName = System.mapLibraryName("rnnoise");
        String resPath = "/native/" + sub + "/" + libName;

        try (InputStream in = NativeLoader.class.getResourceAsStream(resPath)) {
            if (in == null) {
                throw new UnsatisfiedLinkError(resPath + " not found");
            }
            Path tmp = Files.createTempDirectory("rnnoise");
            Path file = tmp.resolve(libName);
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            System.load(file.toAbsolutePath().toString());
        } catch (Exception e) {
            LogUtils.getLogger().error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
