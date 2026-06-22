package cn.ussshenzhou.channel.subspace.server.send;

import cn.ussshenzhou.channel.audio.server.RelayHandler;
import cn.ussshenzhou.channel.subspace.SubspacePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
public class DataUpdatePacket extends SubspacePacket {

    public DataUpdatePacket() {
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            buf.writeVarInt(0);
            return;
        }
        var list = server.getPlayerList().getPlayers();
        buf.writeVarInt(list.size());
        for (var player : list) {
            buf.writeUUID(player.getUUID());
            buf.writeDouble(player.getX());
            buf.writeDouble(player.getY());
            buf.writeDouble(player.getZ());
            buf.writeInt(player.level().dimension().toString().hashCode());
            buf.writeBoolean(player.isSpectator());
        }

        buf.writeVarInt(RelayHandler.PLAYER_CHANNELS_SEND.size());
        RelayHandler.PLAYER_CHANNELS_SEND.forEach((player, channels) -> {
            buf.writeUUID(player.getUUID());
            buf.writeVarInt(channels.size());
            for (var channel : channels) {
                buf.writeVarInt(channel);
            }
        });

        buf.writeVarInt(RelayHandler.CHANNEL_PLAYERS_RECEIVE.size());
        RelayHandler.CHANNEL_PLAYERS_RECEIVE.forEach((channel, players) -> {
            buf.writeVarInt(channel);
            buf.writeVarInt(players.size());
            for (var player : players) {
                buf.writeUUID(player.getUUID());
            }
        });
    }

    @Override
    public int getId() {
        return 3;
    }
}
