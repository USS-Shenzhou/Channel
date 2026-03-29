package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.input.ModKeyMappingRegistry;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class LoginNotificationHelper {

    @SubscribeEvent
    public static void showNotification(ClientPlayerNetworkEvent.LoggingIn event) {
        TSimpleNotification.fire(
                Component.translatable("channel.welcome",
                        ModKeyMappingRegistry.CONFIG.getKeyModifier().getCombinedName(ModKeyMappingRegistry.CONFIG.getKey(), () -> ModKeyMappingRegistry.CONFIG.getKey().getDisplayName()).getString()
                ),
                12,
                TSimpleNotification.Severity.TIP
        );
    }
}
