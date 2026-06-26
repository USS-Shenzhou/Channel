package cn.ussshenzhou.channel.network;

import cn.ussshenzhou.channel.blockentity.ChanneledBlockEntity;
import cn.ussshenzhou.channel.gui.hud.MicrophoneHud;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import cn.ussshenzhou.t88.network.annotation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = ModConstant.SHORT_ID, handleOnNetwork = true)
public class OpMutePacket {
    public final boolean mute;

    public OpMutePacket(boolean mute) {
        this.mute = mute;
    }

    @Decoder
    public OpMutePacket(FriendlyByteBuf buf) {
        mute = buf.readBoolean();
    }

    @Encoder
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(mute);
    }

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        if (mute && !context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            MicrophoneHud.setStatus(MicrophoneHud.Status.OP);
            TSimpleNotification.fire(Component.translatable("notice.channel.mute_true"), 10, TSimpleNotification.Severity.WARN);
        }
        if (!mute) {
            MicrophoneHud.resumeStatus();
            TSimpleNotification.fire(Component.translatable("notice.channel.mute_false"), 10, TSimpleNotification.Severity.TIP);
        }
    }
}
