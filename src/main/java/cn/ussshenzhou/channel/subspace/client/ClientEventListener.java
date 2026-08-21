package cn.ussshenzhou.channel.subspace.client;

import cn.ussshenzhou.channel.network.TalkPacket2S;
import cn.ussshenzhou.channel.subspace.server.send.DataUpdatePacket;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventListener {
    private static long lastHeartbeat = 0;

    @SubscribeEvent
    public static void onExit(ClientPlayerNetworkEvent.LoggingOut event) {
        SubspaceConnection.terminate();
    }

    @SubscribeEvent
    public static void updatePlayerData(ClientTickEvent.Pre event) {
        if (SubspaceConnection.using()) {
            var now = Util.getMillis();
            if (now - lastHeartbeat > 1000) {
                lastHeartbeat = now;
                SubspaceConnection.send(new TalkPacket2S(new byte[0]));
            }
        }
    }
}
