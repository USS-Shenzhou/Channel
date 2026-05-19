package cn.ussshenzhou.channel.subspace.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventListener {

    @SubscribeEvent
    public static void onExit(ClientPlayerNetworkEvent.LoggingOut event) {
        SubspaceConnection.terminate();
    }
}
