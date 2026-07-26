package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.Item.ModItems;
import cn.ussshenzhou.channel.audio.client.send.WebRTCHelper;
import cn.ussshenzhou.channel.blockentity.SpeakerBlockEntity;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.config.ChannelPlayerConfig;
import cn.ussshenzhou.channel.network.AudioPacket2C;
import cn.ussshenzhou.channel.audio.OpusManager;
import cn.ussshenzhou.channel.subspace.client.SubspaceAudioPacket;
import cn.ussshenzhou.channel.subspace.client.SubspaceConnection;
import cn.ussshenzhou.channel.subspace.packet;
import cn.ussshenzhou.channel.util.AudioHelper;
import com.google.common.collect.MapMaker;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class AudioReceiveHandler {
    public static final int PLAY_RATE10 = 2;
    private static final Set<SpeakerBlockEntity> CHANNELED_BLOCK_CACHE_C = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    @SubscribeEvent
    public static void removeRemovedBlock(ClientTickEvent.Pre event) {
        synchronized (CHANNELED_BLOCK_CACHE_C) {
            CHANNELED_BLOCK_CACHE_C.removeIf(BlockEntity::isRemoved);
        }
    }

    @SubscribeEvent
    public static void onExit(ClientPlayerNetworkEvent.LoggingOut event) {
        synchronized (CHANNELED_BLOCK_CACHE_C) {
            CHANNELED_BLOCK_CACHE_C.clear();
        }
    }

    public static void add(SpeakerBlockEntity blockEntity) {
        synchronized (CHANNELED_BLOCK_CACHE_C) {
            //noinspection MapOrSetKeyShouldOverrideHashCodeEquals
            AudioReceiveHandler.CHANNELED_BLOCK_CACHE_C.add(blockEntity);
        }
    }

    public static void handle(AudioPacket2C packet) {
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            if (ChannelPlayerConfig.muted(packet.from)) {
                return;
            }
            handleInternal(packet, level);
        } catch (Exception e) {
            LogUtils.getLogger().error("Something went wrong, but it should be okay. You can ignore this if nothing else went wrong.");
            LogUtils.getLogger().error(e.toString(), e);
        }
    }

    private static void handleInternal(AudioPacket2C packet, Level level) throws Exception {
        double x = 0, y = 0, z = 0;
        var from = level.getPlayerByUUID(packet.from);
        if (from != null) {
            // talking nearby
            var pos = from.getEyePosition();
            x = pos.x;
            y = pos.y;
            z = pos.z;
        } else if (packet instanceof SubspaceAudioPacket subspace) {
            // through subspace
            x = subspace.x;
            y = subspace.y;
            z = subspace.z;
        } else if (packet.channels.length == 0) {
            // should not happen
            return;
        }
        var earPos = AudioHelper.getEarPos();
        var decoded = OpusManager.decode(packet.opus, packet.from);
        // through speaker
        var channels = IntArraySet.of(packet.channels);
        if (packet.channels.length != 0) {
            synchronized (CHANNELED_BLOCK_CACHE_C) {
                for (var speaker : CHANNELED_BLOCK_CACHE_C) {
                    if (!channels.contains(speaker.getChannel())) {
                        continue;
                    }
                    var pos = speaker.getBlockPos();
                    if (earPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64 * 64) {
                        continue;
                    }
                    var audio = (SpeakerAudio) AudioManager.audios.compute(SpeakerAudio.hashcode(speaker.getBlockPos()), (_, old) -> old == null ? new SpeakerAudio(speaker) : old);
                    audio.push(packet.from, decoded);
                }
            }
        }
        //direct audio
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return;
        }
        boolean hearingSelf = ChannelClientConfig.get().hearMyself && localPlayer.getUUID().equals(packet.from);
        boolean hearingOther = !localPlayer.getUUID().equals(packet.from) && earPos.distanceToSqr(x, y, z) <= 64 * 64;
        if (hearingSelf || hearingOther) {
            // direct talking sound always apply
            var audio = (DirectAudio) AudioManager.audios.compute(packet.from.hashCode(), (_, old) -> old == null ? new DirectAudio(packet.from) : old);
            audio.push(decoded);
            audio.setPos(x, y, z);
        }
        // through walkie-talkie
        for (var item : localPlayer.getInventory()) {
            if (item.is(ModItems.WALKIE_TALKIE.get())) {
                var ch = item.get(ModItems.CHANNEL.get());
                if (ch != null && ch > 0 && channels.contains(ch.intValue())) {
                    var audio = (WalkieTalkieAudio) AudioManager.audios.compute(0, (_, old) -> old == null ? new WalkieTalkieAudio() : old);
                    audio.push(packet.from, decoded);
                    break;
                }
            }
        }
    }
}
