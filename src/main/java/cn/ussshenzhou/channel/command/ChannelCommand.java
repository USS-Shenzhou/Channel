package cn.ussshenzhou.channel.command;

import cn.ussshenzhou.channel.audio.server.RelayHandler;
import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.channel.network.OpMutePacket;
import cn.ussshenzhou.channel.subspace.server.SubspaceConnection;
import cn.ussshenzhou.t88.network.NetworkHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class ChannelCommand {

    public static void channelCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("channel")
                        .requires(commandSourceStack -> commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .redirect(dispatcher.register(Commands.literal("ch")
                                        .requires(commandSourceStack -> commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                                        .then(Commands.literal("mute_none_OP")
                                                .executes(ct -> {
                                                    var now = ChannelServerConfig.get().muteNoneOP;
                                                    ChannelServerConfig.write(c -> c.muteNoneOP = !now);
                                                    ct.getSource().sendSystemMessage(Component.literal(now ? "All players can talk now." : "Only OP 2 can talk now."));
                                                    NetworkHelper.sendToAllPlayers(new OpMutePacket(!now));
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                        .then(Commands.literal("reset_subspace")
                                                .executes(ct -> {
                                                    if (!ChannelServerConfig.get().useSubspace) {
                                                        ct.getSource().sendSystemMessage(Component.literal("Subspace is not enabled."));
                                                    }
                                                    ct.getSource().sendSystemMessage(Component.literal("Resetting start..."));
                                                    SubspaceConnection.reset();
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                        .then(Commands.literal("dump_channels")
                                                .executes(ct -> {
                                                    ct.getSource().sendSystemMessage(Component.literal(RelayHandler.dump()));
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
        );
    }

    @SubscribeEvent
    public static void playerLogIn(PlayerEvent.PlayerLoggedInEvent event) {
        var mute = ChannelServerConfig.get().muteNoneOP;
        if (mute && event.getEntity() instanceof ServerPlayer player) {
            NetworkHelper.sendToPlayer(player, new OpMutePacket(mute));
        }
    }
}
