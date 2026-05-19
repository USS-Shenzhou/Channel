package cn.ussshenzhou.channel.subspace.server;

import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.channel.subspace.server.send.DataUpdatePacket;
import cn.ussshenzhou.channel.subspace.server.send.PlayerLogoutPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
        if (SubspaceConnection.using()) {
            SubspaceConnection.shutdown();
        }
    }

    @SubscribeEvent
    public static void playerLogIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (SubspaceConnection.using()) {
            SubspaceConnection.newPlayer(event.getEntity());
        }

    }

    @SubscribeEvent
    public static void playerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (SubspaceConnection.using()) {
            SubspaceConnection.send(new PlayerLogoutPacket(event.getEntity().getUUID()));
        }
    }

    public static int tickCount = 0;

    @SubscribeEvent
    public static void updatePlayerData(ServerTickEvent.Post event) {
        if (SubspaceConnection.using() && tickCount % 10 == 0) {
            SubspaceConnection.send(new DataUpdatePacket());
        }
        tickCount++;
    }
}
