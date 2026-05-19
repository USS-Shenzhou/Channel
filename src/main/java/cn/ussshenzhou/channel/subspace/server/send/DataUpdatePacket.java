package cn.ussshenzhou.channel.subspace.server.send;

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
    }

    @Override
    public int getId() {
        return 3;
    }
}
