package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.client.Initializer;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.input.ModKeyMappingRegistry;
import cn.ussshenzhou.channel.util.CompatHelper;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.concurrent.locks.LockSupport;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class LoginNotificationHelper {

    @SubscribeEvent
    public static void showNotification(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!CompatHelper.isClientLevelValid()) {
            return;
        }
        TSimpleNotification.fire(
                Component.translatable("channel.welcome",
                        ModKeyMappingRegistry.CONFIG.getKeyModifier().getCombinedName(ModKeyMappingRegistry.CONFIG.getKey(), () -> ModKeyMappingRegistry.CONFIG.getKey().getDisplayName()).getString(),
                        ModKeyMappingRegistry.PTT.getKeyModifier().getCombinedName(ModKeyMappingRegistry.PTT.getKey(), () -> ModKeyMappingRegistry.PTT.getKey().getDisplayName()).getString()
                ),
                12,
                TSimpleNotification.Severity.TIP
        );
        if (ChannelClientConfig.get().onAir) {
            Thread.startVirtualThread(() -> {
                while (Minecraft.getInstance().screen != null) {
                    LockSupport.parkNanos(1000_000_000);
                }
                Minecraft.getInstance().execute(Initializer::init);
            });
        }
    }
}
