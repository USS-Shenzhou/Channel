package cn.ussshenzhou.channel.subspace.server;

import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.channel.subspace.AesGcmEncoder;
import cn.ussshenzhou.channel.subspace.SubspacePacket;
import cn.ussshenzhou.channel.subspace.server.send.InitPacket;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Varint21FrameDecoder;
import net.minecraft.network.Varint21LengthFieldPrepender;

import java.util.concurrent.TimeUnit;

/**
 * @author USS_Shenzhou
 */
public class SubspaceConnection {
    private static EventLoopGroup group;
    private static volatile Channel channel;
    private static volatile boolean activelyDisconnect;

    /**
     * <pre>
     * ┌---┬-------┬----┬---------┬---┐
     * │ H │ Nonce │ Id | Content │TAG│
     * └---┴-------┴----┴---------┴---┘
     *             └----Packet----┘
     *             └------AES-GCM-----┘
     *
     * H       = header, varint, packet length
     * Nonce   = Nonce, varint, for AES-GCM
     * Id      = packet id, varint, by {@link SubspacePacket#getId()}
     * Content = packet payload, by {@link SubspacePacket#encode(FriendlyByteBuf)}
     * TAG     = tag, byte[16], for AES-GCM
     * </pre>
     */
    public static void connect() {
        var cfg = ChannelServerConfig.get();
        group = new MultiThreadIoEventLoopGroup(1, new DefaultThreadFactory("Channel-Server-Subspace", true), NioIoHandler.newFactory());
        new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new Varint21FrameDecoder(null),

                                new Varint21LengthFieldPrepender(),
                                new AesGcmEncoder(cfg.subspaceFrequency)
                        );
                    }
                })
                .connect(cfg.subspaceAddress, cfg.subspaceServerPort)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        channel = future.channel();
                        channel.closeFuture().addListener((ChannelFutureListener) f -> {
                            if (!activelyDisconnect) {
                                LogUtils.getLogger().warn("Disconnected from subspace. Reconnecting in 10s...");
                                group.schedule(SubspaceConnection::connect, 10, TimeUnit.SECONDS);
                            }
                        });
                        send(new InitPacket());
                    } else {
                        LogUtils.getLogger().error("Failed to connect to subspace server. Try again in 10s...");
                        group.schedule(SubspaceConnection::connect, 10, TimeUnit.SECONDS);
                    }
                });
    }

    public static void send(SubspacePacket packet) {
        if (channel != null && channel.isActive()) {
            var buf = new FriendlyByteBuf(channel.alloc().buffer());
            buf.writeVarInt(packet.getId());
            packet.encode(buf);
            channel.writeAndFlush(buf);
        }
    }

    public static void changeSubspace() {
        activelyDisconnect = true;
        if (channel != null) {
            channel.close();
        }
        activelyDisconnect = false;
        connect();
    }

    public static void shutdown() {
        activelyDisconnect = true;
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        activelyDisconnect = false;
    }
}
