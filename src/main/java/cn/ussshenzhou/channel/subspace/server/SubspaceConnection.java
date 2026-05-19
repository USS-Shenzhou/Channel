package cn.ussshenzhou.channel.subspace.server;

import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.channel.network.SubspaceInitPacket;
import cn.ussshenzhou.channel.subspace.AesGcmEncoder;
import cn.ussshenzhou.channel.subspace.SubspacePacket;
import cn.ussshenzhou.channel.subspace.server.send.InitPacket;
import cn.ussshenzhou.channel.subspace.server.send.PlayerLoginPacket;
import cn.ussshenzhou.t88.network.NetworkHelper;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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
                        LogUtils.getLogger().info("Subspace server connected.");
                        group.schedule(() -> {
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000));
                            if (channel.isActive() && ServerLifecycleHooks.getCurrentServer() != null) {
                                ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(SubspaceConnection::newPlayer);
                            }
                        }, 1, TimeUnit.SECONDS);
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

    public static void newPlayer(Player player) {
        byte[] token = new SecureRandom().generateSeed(32);
        SubspaceConnection.send(new PlayerLoginPacket(token, player.getUUID(), player.getId()));
        var cfg = ChannelServerConfig.get();
        NetworkHelper.sendToPlayer((ServerPlayer) player, new SubspaceInitPacket(token, cfg.subspaceProtocol, cfg.subspaceAddress, cfg.subspaceClientPort, cfg.subspaceSecurityLevel));
    }

    public static boolean using(){
        return channel != null && channel.isActive();
    }
}
