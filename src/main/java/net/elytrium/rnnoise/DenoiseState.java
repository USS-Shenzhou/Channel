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

import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * @author Elytrium
 */
public class DenoiseState {

  private final int frameSize;
  private final long state;

  /**
   * Allocate and initialize a DenoiseState
   * The returned pointer MUST be freed with {@link DenoiseState#destroy()}
   */
  public DenoiseState() {
    this.frameSize = RnnNative.getFrameSize();
    this.state = RnnNative.create(0);
  }

  /**
   * Allocate and initialize a DenoiseState
   * If model is NULL the default model is used.
   * The returned pointer MUST be freed with {@link DenoiseState#destroy()}
   */
  public DenoiseState(RnnModel model) {
    this.frameSize = RnnNative.getFrameSize();
    this.state = RnnNative.create(model == null ? 0 : model.getState());
  }

  /**
   * Free a DenoiseState produced by rnnoise_create.
   * The optional custom model must be freed by {@link RnnModel#free()} after.
   */
  public void destroy() {
    RnnNative.destroy(this.state);
  }

  /**
   * Denoise a frame of samples
   *
   * @param in  must be a direct buffer at least {@link RnnNative#getFrameSize()} large.
   * @param out must be a direct buffer at least {@link RnnNative#getFrameSize()} large.
   */
  public void processFrame(FloatBuffer out, FloatBuffer in) {
    if (!in.isDirect() || !out.isDirect()) {
      throw new IllegalArgumentException("Buffer must be direct");
    }

    if (in.capacity() < this.frameSize || out.capacity() < this.frameSize) {
      throw new IllegalArgumentException("Data must be at least " + this.frameSize + " large");
    }

    RnnNative.processFrameBuffer(this.state, out, in);
  }

  /**
   * Denoise a frame of samples
   *
   * @param in  must be a direct buffer at least {@link RnnNative#getFrameSize()} large.
   * @param out must be a direct buffer at least {@link RnnNative#getFrameSize()} large.
   */
  public void processFrame(Buffer out, Buffer in) {
    if (!in.isDirect() || !out.isDirect()) {
      throw new IllegalArgumentException("Buffer must be direct");
    }

    RnnNative.processFrameBuffer(this.state, out, in);
  }

  /**
   * Denoise a frame of samples
   *
   * @param in  must be at least {@link RnnNative#getFrameSize()} large.
   * @param out must be at least {@link RnnNative#getFrameSize()} large.
   */
  public void processFrame(float[] out, float[] in) {
    if (in.length < this.frameSize || out.length < this.frameSize) {
      throw new IllegalArgumentException("Data must be at least " + this.frameSize + " large");
    }

    RnnNative.processFrame(this.state, out, in);
  }

  public int getFrameSize() {
    return this.frameSize;
  }
}
