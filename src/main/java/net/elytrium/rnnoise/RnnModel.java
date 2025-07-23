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

import java.io.File;
import java.nio.file.Path;

/**
 * @author Elytrium
 */
public class RnnModel {

  private final long state;

  /**
   * Load a model from a file
   * It must be deallocated with {@link RnnModel#free()}
   */
  public RnnModel(File path) {
    this(path.getPath());
  }

  /**
   * Load a model from a file
   * It must be deallocated with {@link RnnModel#free()}
   */
  public RnnModel(Path path) {
    this(path.toString());
  }

  /**
   * Load a model from a file
   * It must be deallocated with {@link RnnModel#free()}
   */
  public RnnModel(String path) {
    this.state = RnnNative.modelFromFile(path);
  }

  /**
   * Free a custom model
   * It must be called after all the DenoiseStates referring to it are freed.
   */
  public void free() {
    RnnNative.modelFree(this.state);
  }

  protected long getState() {
    return this.state;
  }
}
