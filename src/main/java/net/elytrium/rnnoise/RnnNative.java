/*
 * Copyright (C) 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.rnnoise;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * @author Elytrium
 */
public class RnnNative {

  static {
    try {
      System.loadLibrary("rnnoise-native");
    } catch (UnsatisfiedLinkError e) {
      try (InputStream inputStream = RnnNative.class.getResourceAsStream("/librnnoise-native." + getLibExtension())) {
        if (inputStream == null) {
          throw new IOException();
        }

        Path libraryFile = Files.createTempFile("librnnoise-native", getLibExtension());
        Files.copy(inputStream, libraryFile, StandardCopyOption.REPLACE_EXISTING);
        System.load(libraryFile.toAbsolutePath().toString());
        libraryFile.toFile().deleteOnExit();
      } catch (IOException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
  }

  /**
   * @return the number of samples processed by rnnoise_process_frame at a time
   */
  public static native int getFrameSize();

  /**
   * Allocate and initialize a DenoiseState
   * If model is NULL the default model is used.
   * The returned pointer MUST be freed with {@link RnnNative#destroy(long)}.
   *
   * @return rnnoise state
   */
  public static native long create(long model);

  /**
   * Free a DenoiseState produced by rnnoise_create.
   * The optional custom model must be freed by {@link RnnNative#modelFree(long)} after.
   */
  public static native void destroy(long state);

  /**
   * Denoise a frame of samples
   *
   * @param in  must be a direct buffer at least {@link RnnNative#getFrameSize()} large.
   * @param out must be a direct buffer at least {@link RnnNative#getFrameSize()} large.
   */
  public static native void processFrameBuffer(long state, Buffer out, Buffer in);

  /**
   * Denoise a frame of samples
   *
   * @param in  must be at least {@link RnnNative#getFrameSize()} large.
   * @param out must be at least {@link RnnNative#getFrameSize()} large.
   */
  public static native void processFrame(long state, float[] out, float[] in);

  /**
   * Load a model from a file
   * It must be deallocated with {@link RnnNative#modelFree(long)}
   */
  public static native long modelFromFile(String file);

  /**
   * Free a custom model
   * It must be called after all the DenoiseStates referring to it are freed.
   */
  public static native void modelFree(long model);

  private static String getLibExtension() {
    String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    if (osName.contains("win")) {
      return "dll";
    } else if (osName.contains("mac") || osName.contains("darwin")) {
      return "dylib";
    } else {
      return "so";
    }
  }
}
