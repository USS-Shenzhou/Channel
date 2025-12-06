# 沉浸语音 | Channel

![logo](src/main/resources/channel.png)

## 给Linux/MacOS用户 | For Linux/MacOS Users

要在Linux/MacOS x86_64或arm64上使用此模组，请自行构建：

1. 将`build.gradle`中的所有`classifier: "windows-x86_64"`改为：
   1. classifier: "linux-x86_64"
   2. 或classifier: "linux-aarch64"
   3. 或classifier: "macos-x86_64"
   4. 或classifier: "macos-aarch64"；
2. 运行`./gradlew build`，构建产物将会保存在`build/libs`。

由于Nvidia AFX SDK客户端仅支持Windows，故智能降噪在Linux/MacOS上不可用。如果确实有此需求，可以自行修改`NvidiaInit.java`，更多信息可以在[Nvidia的用户指南](https://docs.nvidia.com/maxine/afx/LinuxAFXSDK/GetStartedOnLinux.html)查看。

由于能力有限，只对Linux提供有限支持。不对MacOS提供支持。

To use this mod on Linux/MacOS x86_64 or arm64, you need to build it yourself:

1. Replace all occurrences of `classifier: "windows-x86_64"` in `build.gradle` with one of the following:
    1. `classifier: "linux-x86_64"`
    2. or `classifier: "linux-aarch64"`
    3. or `classifier: "macos-x86_64"`
    4. or `classifier: "macos-aarch64"`
2. Run `./gradlew build`. The build output will be located in `build/libs`.

Since the Nvidia AFX SDK client only supports Windows, smart mode of noise-canceling is not available on Linux or macOS. If you do need this functionality, you may modify `NvidiaInit.java` yourself. For more information, see the [Nvidia User Guide](https://docs.nvidia.com/maxine/afx/LinuxAFXSDK/GetStartedOnLinux.html).

Due to limited resources, only partial support is provided for Linux. MacOS is not supported.


## 版权和许可 | Copyrights and Licenses

Copyright (C) 2025 USS_Shenzhou

本模组是自由软件：你可以根据自由软件基金会发布的GNU通用公共许可证的条款，即许可证的第3版或（您选择的）任何后来的版本重新发布它和/或修改它。。

本模组的发布是希望它能起到作用。但没有任何保证；甚至没有隐含的保证。本程序的分发是希望它是有用的，但没有任何保证，甚至没有隐含的适销对路或适合某一特定目的的保证。 参见GNU通用公共许可证了解更多细节。

This mod is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or(at your option) any later version.

This mod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

### 额外许可和附加条款 | Exceptions and Additional terms

- a) 当你作为游戏玩家，加载本程序于Minecraft并游玩时，本许可证自动地授予你一切为正常加载本程序于Minecraft并游玩所必要的、不在GPL-3.0许可证内容中、或是GPL-3.0许可证所不允许的权利。如果GPL-3.0许可证内容与Minecraft EULA或其他Mojang/微软条款产生冲突，以后者为准；
- b) 如果你对本软件进行了修改，你在转发修改后的版本时，应当明确说明其修改自本软件，并说明本软件开发者不对修改后的版本提供任何保证。不得使用本软件任一开发者或本软件的的名义来宣传修改后的版本。

<P></P>

- a) As a game player, when you load and play this program in Minecraft, this license automatically grants you all rights necessary, which are not covered in the GPL-3.0 license, or are prohibited by the GPL-3.0 license, for the normal loading and playing of this program in Minecraft. In case of conflicts between the GPL-3.0 license and the Minecraft EULA or other Mojang/Microsoft terms, the latter shall prevail.
- b) If you modify this Program, you should clearly state that modified programs are modified from this Program when you propagate the modified version and that the Developers of this Software do not provide any warranty for the modified programs. You may not use the name of any of the developers of this Software or this Software to for publicity purposes.

## 其他开源/版权信息 | Other Open-Source/Copyright Information

1. 使用了[WebRTC-java](https://github.com/devopvoid/webrtc-java)，按Apache-2.0取得许可。
2. 使用了[Concentus](https://github.com/lostromb/concentus)，按其自定义许可证，一种与BSD-3相似的许可证，取得许可。
3. 使用了[NVIDIA MAXINE AFX SDK](https://github.com/NVIDIA-Maxine/Maxine-AFX-SDK)，按MIT取得许可。
4. 部分代码受[SoundPhysicsPerfected](https://github.com/FalseMSP/SoundPhysicsPerfected)启发，按MIT取得许可。

<p></p>

1. Uses [WebRTC-java](https://github.com/devopvoid/webrtc-java), licensed under the Apache-2.0 License.
2. Uses [Concentus](https://github.com/lostromb/concentus), licensed under its custom license, which is similar to BSD-3 Clause.
3. Uses [NVIDIA MAXINE AFX SDK](https://github.com/NVIDIA-Maxine/Maxine-AFX-SDK), licensed under the MIT License.
4. Some codes are inspired by [SoundPhysicsPerfected](https://github.com/FalseMSP/SoundPhysicsPerfected), licensed under the MIT License.