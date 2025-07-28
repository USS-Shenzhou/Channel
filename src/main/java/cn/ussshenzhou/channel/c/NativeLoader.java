package cn.ussshenzhou.channel.c;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;

import javax.naming.OperationNotSupportedException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * @author USS_Shenzhou
 */
public class NativeLoader {
    public static void loadRnnoise() {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            System.load(Path.of(System.getProperty("user.dir"), "../src/main/resources/native/rnnoise/windows_x64/rnnoise.dll").normalize().toAbsolutePath().toString());
            return;
        }
        String os = System.getProperty("os.name").toLowerCase();
        os = os.contains("win") ? "windows" : os.contains("mac") ? "mac" : "linux";
        String arch = System.getProperty("os.arch").toLowerCase();
        arch = arch.contains("x86_64") || arch.contains("amd64") ? "x64" : "arm64";
        String libName = System.mapLibraryName("rnnoise");
        String resPath = "/native/" + os + "_" + arch + "/" + libName;
        try (InputStream in = NativeLoader.class.getResourceAsStream(resPath)) {
            if (in == null) {
                throw new OperationNotSupportedException(resPath + " not found, maybe do not support" + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
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
