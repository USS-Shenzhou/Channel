# 沉浸语音 | Channel

![logo](src/main/resources/channel.png)

## 简介 | Introduction

沉浸语音是一个开源的游戏内对讲模组。

*Channel* is an open-source in-game voice chat mod.

### 和Simple Voice Chat有什么不同？| How does it differ from Simple Voice Chat?

| 沉浸语音</br>Channel                                                                           | SVC                                 |
|--------------------------------------------------------------------------------------------|-------------------------------------|
| ✅自由许可证</br>Free License                                                                    | ❌保留所有权利</br>All Rights Reserved     |
| ✅固定降噪算法+基于Nvidia Broadcast的AI降噪</br>Fixed Noise Suppression + NVIDIA Broadcast AI Denoiser | ❌RNNoise降噪</br>RNNoise Suppression  |
| ✅基于路径追踪的混响模拟</br>Path-Traced Reverb Simulation                                             | ❌简单位置音频</br>Simple Positional Audio |
| ✅开箱即用，不需要额外端口，无需配置</br>Out-of-the-box, no extra ports or configuration required            | ❌需要额外端口</br>Requires extra ports    |
| ❌仅NeoForge</br>NeoForge only                                                               | ✅Fabric/Quilt/Forge/Bukkit          |
| ❌无扩展功能</br>No extensions                                                                   | ✅API插件</br>API Plugins              |

### 饼 | TODO

- 路径追踪音频：
  - [x] 针对栅栏/玻璃板等半透过方块的处理
  - [ ] 针对流体方块的处理
  - [ ] 模拟绝对响度
- 转发：
  - [x] 独立的转发程序
    - [x] TCP
    - [ ] UDP
    - [ ] GRPC
- 游戏内：
  - [x] 话筒和音响方块
  - [ ] 对讲机物品


- Path-Traced Audio:
    - [x] Handling of semi-transparent blocks (e.g., fences, glass panes)
    - [ ] Handling of fluid blocks
    - [ ] Absolute loudness simulation
- Forwarding:
    - [x] Standalone forwarding application
      - [x] TCP
      - [ ] UDP
      - [ ] GRPC
- In-game:
    - [x] Microphone and Speaker blocks
    - [ ] Walkie-talkie items


## 给Linux/MacOS用户 | For Linux/MacOS Users

要在Linux/MacOS x86_64或arm64上使用此模组，请从[Actions](https://github.com/USS-Shenzhou/Channel/actions)中下载。

由于Nvidia AFX SDK客户端仅支持Windows，故智能降噪在Linux/MacOS上不可用。如果确实有此需求，可以自行修改`NvidiaInit.java`，更多信息可以在[Nvidia的用户指南](https://docs.nvidia.com/maxine/afx/LinuxAFXSDK/GetStartedOnLinux.html)查看。

由于能力有限，只对Linux提供有限支持。不对MacOS提供积极支持。

To use this mod on Linux/MacOS x86_64 or arm64, you need to download from [Actions](https://github.com/USS-Shenzhou/Channel/actions).

Since the Nvidia AFX SDK client only supports Windows, smart mode of noise-canceling is not available on Linux or macOS. If you do need this functionality, you may modify `NvidiaInit.java` yourself. For more information, see the [Nvidia User Guide](https://docs.nvidia.com/maxine/afx/LinuxAFXSDK/GetStartedOnLinux.html).

Due to limited resources, only partial support is provided for Linux. MacOS is not actively supported.

## 子空间中继 | Subspace

Subspace是一个独立于Minecraft服务端的运行程序，它可以替代Minecraft服务端进行语音数据转发操作，可以运行在完全不同的机器上，支持更多协议和端口。

Subspace is a standalone program that runs independently of the Minecraft server. It can take over voice data forwarding from the Minecraft server, may run on an entirely different machine, and supports more protocols and ports.

> [!NOTE]
>
> 即使使用了Subspace，你也仍然需要在服务端安装Channel并进行配置。
>
> 单个Subspace进程接受来自多个Minecraft服务端的连接，前提是后连接的服务端所指定的协议和安全等级与最初连接的服务端指定的相同。当没有服务端连接时，Subspace会自动重置，等待指定任意协议和安全等级。
>
> Even when Subspace is used, you still need to install and configure Channel on the Minecraft server.
>
> A single Subspace process accepts connections from multiple Minecraft servers, provided that any subsequently connected server specifies the same protocol and security level as the first one. When no server is connected, Subspace automatically resets and waits for any protocol and security level to be specified.

> [!WARNING]
>
> 多个Minecraft服务端中的相同UUID的玩家同时连接单个Subspace进程是**未定义行为**。
>
> Connecting players that share the same UUID from multiple Minecraft servers to a single Subspace process at the same time is **undefined behavior**.

要将语音数据导向Subspace，你需要编辑`<Minecraft服务器运行目录>/Config/ChannelServerConfig.json`:

To route voice data through Subspace, edit `<Minecraft server working directory>/Config/ChannelServerConfig.json`:

```json
{
  "useSubspace": true,
  "subspaceAddress": "94.3.94.3",
  "subspaceServerPort": 10943,
  "subspaceClientPort": 10944,
  "subspaceFrequency": "qwerty",
  "subspaceProtocol": "TCP",
  "subspaceSecurityLevel": "LOW"
}
```

| 配置项</br>Option                | 含义</br>Meaning                                                                                                                                                                                 |
| --------------------- |------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| useSubspace           | 总开关。启用后，服务端会指示客户端连接到指定的Subspace。</br>Master switch. When enabled, the server instructs the client to connect to the specified Subspace.                                                        |
| subspaceAddress       | Subspace对应的地址或域名。</br>The address or domain name of the Subspace instance.                                                                                                                     |
| subspaceServerPort    | Subspace与Minecraft**服务端**通信的监听端口。</br>The port on which Subspace listens for the Minecraft **server**.                                                                                         |
| subspaceClientPort    | Subspace与Minecraft**客户端**通信的监听端口。</br>The port on which Subspace listens for the Minecraft **client**.                                                                                         |
| subspaceFrequency     | Subspace与Minecraft**服务端**通信的口令。可以是任意字符串。</br>The passphrase used between Subspace and the Minecraft **server**. May be any string.                                                             |
| subspaceProtocol      | Subspace与Minecraft**客户端**通信的协议，可以是`TCP`、`UDP`、`GRPC`。</br>The protocol used between Subspace and the Minecraft **client**. May be `TCP`, `UDP`, or `GRPC`.                                     |
| subspaceSecurityLevel | Subspace与Minecraft**客户端**通信的安全等级。<br />在TCP模式下，可以是：<br />`NONE`：握手和语音均使用明文<br />`LOW`：握手时使用AES-GCM校验，语音使用明文<br />`MID`：握手时使用AES-GCM校验，语音使用AES-CTR校验（仅加密）<br />`HIGH`：握手和语音均使用AES-GCM校验（防篡改或伪造）</br>The security level used between Subspace and the Minecraft **client**.<br />In TCP mode, may be:<br />`NONE`: both handshake and voice are sent in plaintext.<br />`LOW`: the handshake is verified with AES-GCM; voice is sent in plaintext.<br />`MID`: the handshake is verified with AES-GCM; voice uses AES-CTR (encryption only).<br />`HIGH`: both handshake and voice are verified with AES-GCM (tamper- and forgery-resistant). |

Subspace与Minecraft**客户端**通信的口令由Minecraft**服务端**在玩家加入服务器时随机生成。

Subspace与Minecraft**服务端**通信的 安全等级总是为AES-GCM。

The passphrase used between Subspace and the Minecraft **client** is randomly generated by the Minecraft **server** when a player joins.

The security level used between Subspace and the Minecraft **server** is always AES-GCM.

> **为什么会采用这种设计？**
>
> 基于“如果攻击者能物理接触到你的设备，那它就不再是你的设备了”的原则，Subspace的安全分级只关心网络中间人。
>
> **Why this design?**
>
> Following the principle that "if an attacker can physically reach your device, it is no longer your device", Subspace's security tiers concern themselves only with man-in-the-middle attackers on the network.

### 可执行程序 | Executable

你可以在[CI](https://github.com/USS-Shenzhou/Channel/actions/workflows/subspace-build.yml)直接下载编译好的可执行程序。有三种方式进行配置：

Pre-built executables can be downloaded directly from [CI](https://github.com/USS-Shenzhou/Channel/actions/workflows/subspace-build.yml). There are three ways to configure them:

> [!NOTE]
> 这三种方式的配置优先级为 环境变量<JSON文件<程序参数。后者覆盖前者。
> 
> The priority of these three configuration methods is: environment variables < JSON config file < command-line arguments. The latter overrides the former.

#### JSON配置文件 | JSON config file

在你第一次运行Subspace时，会自动生成一个`SubspaceConfig.json`：

The first time you run Subspace, a `SubspaceConfig.json` is generated automatically:

```json
{
    "clientPort": 10944,
    "serverPort": 10943,
    "subspaceFrequency": "qwerty",
    "threads": 4
}
```

`threads`指的是用于和客户端进行连接和转发的线程数量，你可以根据预估使用人数和服务器CPU来确定。

填写完成之后你可以重新启动。

`threads` is the number of threads used to accept and forward client connections; choose a value based on the expected number of users and the server CPU.

Restart Subspace once the file has been filled in.

#### 运行参数 | Command-line arguments

以Linux为例：

Linux example:

`./subspace --serverPort 10943 --clientPort 10944 --frequency qwerty --threads 4 --no-json-config`

使用`--no-json-config`来绕过无json时的自动退出。

Use `--no-json-config` to bypass the automatic exit that would otherwise occur when no JSON file is present.

#### 环境变量 | Environment variables

参考下一节。

See the next section.

### Docker镜像 | Docker image

拉取镜像：

Pull the image:

```
docker pull crpi-q744b99dm7g39ls8.cn-shanghai.personal.cr.aliyuncs.com/uss-shenzhou/subspace:latest
```

运行：

Run:

```
docker run -d --name subspace \
    -p 10943:10943 \
    -p 10944:10944 \
    -e SUBSPACE_SERVER_PORT=10943 \
    -e SUBSPACE_CLIENT_PORT=10944 \
    -e SUBSPACE_FREQUENCY=qwerty \
    -e SUBSPACE_THREADS=4 \
    crpi-q744b99dm7g39ls8.cn-shanghai.personal.cr.aliyuncs.com/uss-shenzhou/subspace:latest
```

Dockerfile已内置`--no-json-config`。

`--no-json-config` is already baked into the Dockerfile.

## 版权和许可 | Copyrights and Licenses

Copyright (C) 2025 USS_Shenzhou

本模组是自由软件：你可以根据自由软件基金会发布的GNU通用公共许可证的条款，即许可证的第3版或（您选择的）任何后来的版本重新发布它和/或修改它。

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