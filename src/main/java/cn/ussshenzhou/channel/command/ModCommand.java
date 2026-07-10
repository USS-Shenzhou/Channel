package cn.ussshenzhou.channel.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class ModCommand {
    @SubscribeEvent
    public static void regCommand(RegisterCommandsEvent event) {
        ChannelCommand.channelCommand(event.getDispatcher());
    }
}
