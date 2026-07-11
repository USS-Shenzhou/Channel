package cn.ussshenzhou.channel.audio.client.receive;

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

    public static void add(SpeakerBlockEntity blockEntity){
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
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            boolean hearingSelf = ChannelClientConfig.get().hearMyself && localPlayer.getUUID().equals(packet.from);
            boolean hearingOther = !localPlayer.getUUID().equals(packet.from) && earPos.distanceToSqr(x, y, z) <= 64 * 64;
            if (hearingSelf || hearingOther) {
                // direct talking sound always apply
                var audio = getDirectAudioAndCheckSampleRate(packet.from, 48000);
                audio.push(decoded);
                audio.setPos(x, y, z);
            }
        }
        // through speaker
        if (packet.channels.length == 0) {
            return;
        }
        var channels = IntArraySet.of(packet.channels);
        synchronized (CHANNELED_BLOCK_CACHE_C) {
            for (var speaker : CHANNELED_BLOCK_CACHE_C) {
                if (!channels.contains(speaker.getChannel())) {
                    continue;
                }
                var pos = speaker.getBlockPos();
                if (earPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64 * 64) {
                    continue;
                }
                var audio = getSpeakerAudioAndCheckSampleRate(speaker, 48000);
                audio.push(packet.from, decoded);
            }
        }
    }

    private static DirectAudio getDirectAudioAndCheckSampleRate(UUID from, int sampleRate) {
        return (DirectAudio) AudioManager.audios.compute(from.hashCode(), (_, old) -> {
            if (old == null) {
                return new DirectAudio(from, sampleRate);
            } else if (old.sampleRate != sampleRate) {
                old.close();
                return new DirectAudio(from, sampleRate);
            } else {
                return old;
            }
        });
    }

    private static SpeakerAudio getSpeakerAudioAndCheckSampleRate(SpeakerBlockEntity blockEntity, int sampleRate) {
        return (SpeakerAudio) AudioManager.audios.compute(SpeakerAudio.hashcode(blockEntity.getBlockPos()), (_, old) -> {
            if (old == null) {
                return new SpeakerAudio(blockEntity, sampleRate);
            } else if (old.sampleRate != sampleRate) {
                old.close();
                return new SpeakerAudio(blockEntity, sampleRate);
            } else {
                return old;
            }
        });
    }
}
