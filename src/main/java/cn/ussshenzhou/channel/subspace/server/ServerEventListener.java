package cn.ussshenzhou.channel.subspace.server;

import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.channel.network.standalone.SubspaceInitPacket;
import cn.ussshenzhou.channel.subspace.server.send.PlayerLoginPacket;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.security.SecureRandom;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber
public class ServerEventListener {

    @SubscribeEvent
    public static void connectToSubspace(ServerStartedEvent event) {
        var cfg = ChannelServerConfig.get();
        if (!cfg.useSubspace) {
            return;
        }
        SubspaceConnection.connect();
    }

    @SubscribeEvent
    public static void leaveSubspace(ServerStoppingEvent event) {
        var cfg = ChannelServerConfig.get();
        if (!cfg.useSubspace) {
            return;
        }
        SubspaceConnection.shutdown();
    }

    @SubscribeEvent
    public static void playerLogIn(PlayerEvent.PlayerLoggedInEvent event) {
        var cfg = ChannelServerConfig.get();
        if (!cfg.useSubspace) {
            return;
        }
        var player = event.getEntity();
        byte[] token = new SecureRandom().generateSeed(32);
        SubspaceConnection.send(new PlayerLoginPacket(token, player.getUUID(), player.getId()));
        NetworkHelper.sendToPlayer((ServerPlayer) player, new SubspaceInitPacket(token, cfg.subspaceProtocol, cfg.subspaceAddress, cfg.subspaceClientPort, cfg.subspaceSecurityLevel));
    }
}
