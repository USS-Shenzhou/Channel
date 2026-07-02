package cn.ussshenzhou.channel.subspace.client;

import cn.ussshenzhou.channel.gui.hud.MicrophoneHud;
import cn.ussshenzhou.channel.network.TalkPacket2S;
import cn.ussshenzhou.channel.network.SubspaceInitPacket;
import cn.ussshenzhou.channel.subspace.*;
import cn.ussshenzhou.channel.util.Protocol;
import cn.ussshenzhou.channel.util.SecurityLevel;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ScheduledFuture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Varint21FrameDecoder;
import net.minecraft.network.Varint21LengthFieldPrepender;

import java.util.concurrent.TimeUnit;

/**
 * @author USS_Shenzhou
 */
public class SubspaceConnection {
    private static Protocol protocol;
    private static SecurityLevel securityLevel;
    private static final EventLoopGroup EVENT_LOOP_GROUP = new MultiThreadIoEventLoopGroup(1, new DefaultThreadFactory("Channel-Client-Subspace", true), NioIoHandler.newFactory());
    ;
    private static volatile Channel channel;
    private static volatile boolean activelyDisconnect;
    private static ScheduledFuture<?> reconnectFuture;

    /**
     * <h3>TCP</h3>
     * <h4>Security: NONE</h4>
     * <h5>C -> S Handshake</h5>
     * <pre>
     * ┌---┬------┐
     * │ H │ UUID │
     * └---┴------┘
     *
     * H       = header, varint, packet total length
     * UUID    = player UUID, uuid
     * </pre>
     * <h5>C <-> S Voice</h5>
     * <pre>
     * ┌---┬------------------------------------┐
     * │ H │ {@link TalkPacket2S} Or {@link SubspaceAudioPacket} │
     * └---┴------------------------------------┘
     * </pre>
     * <h4>Security: LOW</h4>
     * <h5>C -> S Handshake</h5>
     * <pre>
     * ┌---┬------┬-----┐
     * │ H │ UUID │ TAG │
     * └---┴------┴-----┘
     *            └-----┘
     *            AES-GCM
     *
     * (Nonce  = 0, since we only use it once.)
     * TAG     = byte[16], for AES-GCM
     * </pre>
     * <h5>C <-> S Voice</h5>
     * <i>Same as above.</i>
     * <h4>Security: MID</h4>
     * <h5>C -> S Handshake</h5>
     * <i>Same as above.</i>
     * <h5>C <-> S Voice</h5>
     * <pre>
     * ┌---┬-------┬------------------------------------┐
     * │ H │ Nonce │ {@link TalkPacket2S} Or {@link SubspaceAudioPacket} │
     * └---┴-------┴------------------------------------┘
     *             └---------------AES-CTR--------------┘
     *
     * Nonce   = varint, packet order.
     * </pre>
     * <h4>Security: HIGH</h4>
     * <h5>C -> S Handshake</h5>
     * <i>Same as above.</i>
     * <h5>C <-> S Voice</h5>
     * <pre>
     * ┌---┬-------┬------------------------------------┬-----┐
     * │ H │ Nonce │ {@link TalkPacket2S} Or {@link SubspaceAudioPacket} │ TAG │
     * └---┴-------┴------------------------------------┴-----┘
     *             └------------------AES-GCM-----------------┘
     * </pre>
     *
     * <h3>UDP</h3>
     * One datagram = one packet, no more H field.
     * <h5>C -> S NAT Heartbeat</h5>
     * <i>Empty packet</i>
     * <h4>Security: NONE</h4>
     * <h5>C -> S Handshake</h5>
     * <pre>
     * ┌------┐
     * │ UUID │
     * └------┘
     * </pre>
     * <h5>C <-> S Voice</h5>
     * <pre>
     * ┌---┬------------------------------------┐
     * │ O │ {@link TalkPacket2S} Or {@link SubspaceAudioPacket} │
     * └---┴------------------------------------┘
     *
     * O   = varint, packet order, same as Nonce
     * </pre>
     * <h4>Security: LOW</h4>
     * <h5>C -> S Handshake</h5>
     * <pre>
     * ┌------┬-----┐
     * │ UUID │ TAG │
     * └------┴-----┘
     *        └-----┘
     *        AES-GCM
     * </pre>
     * <h5>C <-> S Voice</h5>
     * <pre>
     * ┌---┬------------------------------------┬------┐
     * │ O │ {@link TalkPacket2S} Or {@link SubspaceAudioPacket} │ HMAC │
     * └---┴------------------------------------┴------┘
     * └---------------HMAC input---------------┘
     * </pre>
     * <h4>Security: MID</h4>
     * <h5>C -> S Handshake</h5>
     * <i>Same as above.</i>
     * <h5>C <-> S Voice</h5>
     * <pre>
     * ┌-------┬------------------------------------┐
     * │ Nonce │ {@link TalkPacket2S} Or {@link SubspaceAudioPacket} │
     * └-------┴------------------------------------┘
     *         └---------------AES-CTR--------------┘
     * </pre>
     * <h4>Security: HIGH</h4>
     * <h5>C -> S Handshake</h5>
     * <i>Same as above.</i>
     * <h5>C <-> S Voice</h5>
     * <pre>
     * ┌-------┬------------------------------------┬-----┐
     * │ Nonce │ {@link TalkPacket2S} Or {@link SubspaceAudioPacket} │ TAG │
     * └-------┴------------------------------------┴-----┘
     *         └------------------AES-GCM-----------------┘
     * </pre>
     * <h3>gRPC</h3>
     * gRPC will handle packet length.
     * Security level is always NONE in gRPC. Whoever needs security may use TLS.
     * <h4>Security: NONE</h4>
     * <h5>C -> S Handshake</h5>
     * <pre>
     * ┌------┐
     * │ UUID │
     * └------┘
     * </pre>
     * <h5>C <-> S Voice</h5>
     * <pre>
     * ┌------------------------------------┐
     * │ {@link TalkPacket2S} Or {@link SubspaceAudioPacket} │
     * └------------------------------------┘
     * </pre>
     */

    public static void connect(SubspaceInitPacket packet) {
        MicrophoneHud.setStatus(MicrophoneHud.Status.SUBSPACE);
        if (channel != null && channel.isActive()) {
            channel.close();
        }
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
        protocol = packet.protocol;
        securityLevel = packet.securityLevel;
        switch (packet.protocol) {
            case TCP -> connectTcp(packet);
            case UDP -> throw new UnsupportedOperationException();
            //TODO
            case GRPC -> throw new UnsupportedOperationException();
        }
    }

    private static void connectTcp(SubspaceInitPacket packet) {
        new Bootstrap()
                .group(EVENT_LOOP_GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new Varint21FrameDecoder(null),
                                new Varint21LengthFieldPrepender()
                        );
                        switch (packet.securityLevel) {
                            case NONE -> {
                            }
                            case LOW -> ch.pipeline().addLast(new OnceAesGcmEncoder(packet.subspaceToken));
                            case MID -> ch.pipeline().addLast(new OnceAesGcmEncoder(packet.subspaceToken, new AesCtrEncoder(packet.subspaceToken, 1)),
                                    new AesCtrDecoder(packet.subspaceToken));
                            case HIGH -> ch.pipeline().addLast(new OnceAesGcmEncoder(packet.subspaceToken, new AesGcmEncoder(packet.subspaceToken, 1)),
                                    new AesGcmDecoder(packet.subspaceToken));
                        }
                        ch.pipeline().addLast(new SubspacePacketHandler());
                    }
                })
                .connect(packet.subspaceAddress, packet.subspacePort)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        channel = future.channel();
                        channel.closeFuture().addListener((ChannelFutureListener) f -> {
                            if (activelyDisconnect) {
                                activelyDisconnect = false;
                            } else {
                                LogUtils.getLogger().warn("Disconnected from subspace. Reconnecting in 10s...");
                                reconnectFuture = EVENT_LOOP_GROUP.schedule(() -> connect(packet), 10, TimeUnit.SECONDS);
                            }
                            channel = null;
                        });
                        send(new HandshakePacket());
                        MicrophoneHud.setStatus(MicrophoneHud.Status.STANDBY);
                    } else {
                        LogUtils.getLogger().error("Failed to connect to subspace server. Try again in 10s...");
                        reconnectFuture = EVENT_LOOP_GROUP.schedule(() -> connect(packet), 10, TimeUnit.SECONDS);
                        channel = null;
                    }
                });
    }

    public static void send(SubspacePacket packet) {
        if (channel != null && channel.isActive()) {
            var buf = new FriendlyByteBuf(channel.alloc().buffer());
            packet.encode(buf);
            channel.writeAndFlush(buf);
        }
    }

    public static void terminate() {
        activelyDisconnect = true;
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
        protocol = null;
        securityLevel = null;
        MicrophoneHud.resumeStatus();
    }

    public static Protocol getProtocol() {
        return protocol;
    }

    public static SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public static Channel getChannel() {
        return channel;
    }

    public static boolean using() {
        return channel != null && channel.isActive();
    }
}
