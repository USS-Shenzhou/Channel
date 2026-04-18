package cn.ussshenzhou.channel.network.standalone;

import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.channel.util.Protocol;
import cn.ussshenzhou.channel.util.SecurityLevel;
import cn.ussshenzhou.t88.network.annotation.*;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = ModConstant.SHORT_ID, handleOnNetwork = true)
public class SubspaceInitPacket {
    public final byte[] subspaceToken;
    public final Protocol protocol;
    public final String subspaceAddress;
    public final int subspacePort;
    public final SecurityLevel securityLevel;

    public SubspaceInitPacket(byte[] subspaceToken, Protocol protocol, String subspaceAddress, int subspacePort, SecurityLevel securityLevel) {
        this.subspaceToken = subspaceToken;
        this.protocol = protocol;
        this.subspaceAddress = subspaceAddress;
        this.subspacePort = subspacePort;
        this.securityLevel = securityLevel;
    }

    @Decoder
    public SubspaceInitPacket(FriendlyByteBuf buf) {
        this.subspaceToken = buf.readByteArray();
        this.protocol = buf.readEnum(Protocol.class);
        this.subspaceAddress = buf.readUtf();
        this.subspacePort = buf.readVarInt();
        this.securityLevel = buf.readEnum(SecurityLevel.class);
    }

    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(this.subspaceToken);
        buf.writeEnum(this.protocol);
        buf.writeUtf(this.subspaceAddress);
        buf.writeVarInt(this.subspacePort);
        buf.writeEnum(this.securityLevel);
    }

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        //TODO
    }
}
