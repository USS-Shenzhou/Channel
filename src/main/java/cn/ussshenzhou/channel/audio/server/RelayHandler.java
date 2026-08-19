package cn.ussshenzhou.channel.audio.server;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.Item.ModItems;
import cn.ussshenzhou.channel.blockentity.ChanneledBlockEntity;
import cn.ussshenzhou.channel.blockentity.MicBlockEntity;
import cn.ussshenzhou.channel.blockentity.SpeakerBlockEntity;
import cn.ussshenzhou.channel.config.ChannelServerConfig;
import cn.ussshenzhou.channel.network.AudioPacket2C;
import cn.ussshenzhou.t88.network.NetworkHelper;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber
public class RelayHandler {
    //----------Channeled blockentity cache----------
    private static final ConcurrentHashMap<ResourceKey<Level>, Set<ChanneledBlockEntity>> CHANNELED_BLOCK_CACHE_S = new ConcurrentHashMap<>();

    @SuppressWarnings("DataFlowIssue")
    public static void addBlockEntity(ChanneledBlockEntity entity) {
        CHANNELED_BLOCK_CACHE_S
                .computeIfAbsent(entity.getLevel().dimension(), _ -> Collections.newSetFromMap(new MapMaker().weakKeys().makeMap()))
                .add(entity);
    }

    //----------Route update----------
    public static final ExecutorService SERVER_ROUTE_THREAD = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("Channel-Server-Route-Thread-%d")
            .setDaemon(true)
            .build());
    public static final Map<ServerPlayer, IntArraySet> PLAYER_CHANNELS_SEND = new MapMaker().weakKeys().makeMap();
    public static final Int2ObjectOpenHashMap<Set<ServerPlayer>> CHANNEL_PLAYERS_RECEIVE = new Int2ObjectOpenHashMap<>();

    @SubscribeEvent
    public static void updateRoute(ServerTickEvent.Pre event) {
        if (event.getServer().getTickCount() % 10 == 0) {
            SERVER_ROUTE_THREAD.execute(() -> {
                try {
                    Int2ObjectOpenHashMap<Set<ServerPlayer>> chanelPlayersReceive = new Int2ObjectOpenHashMap<>();
                    synchronized (CHANNELED_BLOCK_CACHE_S) {
                        CHANNELED_BLOCK_CACHE_S.forEach((_, s) -> s.removeIf(BlockEntity::isRemoved));
                        for (var player : event.getServer().getPlayerList().getPlayers()) {
                            PLAYER_CHANNELS_SEND.put(player, findNearbySendingDeviceChannels(player));
                            findNearbyReceivingDeviceChannels(player)
                                    .forEach(i -> chanelPlayersReceive
                                            .computeIfAbsent(i, _ -> Collections.newSetFromMap(new MapMaker().weakKeys().makeMap()))
                                            .add(player));
                        }
                        CHANNEL_PLAYERS_RECEIVE.clear();
                        CHANNEL_PLAYERS_RECEIVE.putAll(chanelPlayersReceive);
                    }
                } catch (Exception e) {
                    LogUtils.getLogger().error("Something went wrong, but it should be okay. You can ignore this if nothing else went wrong.");
                    LogUtils.getLogger().error(e.getMessage(), e);
                }
            });
        }
    }

    private static IntArraySet findNearbySendingDeviceChannels(ServerPlayer from) {
        var channels = new IntArraySet();
        checkHandSend(from, channels, InteractionHand.MAIN_HAND);
        checkHandSend(from, channels, InteractionHand.OFF_HAND);
        if (CHANNELED_BLOCK_CACHE_S.containsKey(from.level().dimension())) {
            checkNearbyHandSend(from, channels);
            checkNearbyBlockSend(from, channels);
        }
        return channels;
    }

    private static void checkHandSend(ServerPlayer from, IntArraySet channels, InteractionHand hand) {
        var item0 = from.getItemInHand(hand);
        if (item0.is(ModItems.HANDHELD_MIC_ITEM.get()) || item0.is(ModItems.WALKIE_TALKIE.get())) {
            var ch = item0.get(ModItems.CHANNEL.get());
            if (ch != null && ch > 0) {
                channels.add(ch.intValue());
            }
        }
    }

    private static void checkNearbyHandSend(ServerPlayer from, IntArraySet channels) {
        for (var p : from.level().players()) {
            if (from.getEyePosition().distanceToSqr(p.getEyePosition()) <= Channel.DISTANCE_COMPENSATE_SQR) {
                checkHandSend(p, channels, InteractionHand.MAIN_HAND);
                checkHandSend(p, channels, InteractionHand.OFF_HAND);
            }
        }
    }

    private static void checkNearbyBlockSend(ServerPlayer from, IntArraySet channels) {
        for (var blockEntity : CHANNELED_BLOCK_CACHE_S.get(from.level().dimension())) {
            if (blockEntity instanceof MicBlockEntity) {
                var pos = blockEntity.getBlockPos();
                if (from.getEyePosition().distanceToSqr(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f) <= Channel.DISTANCE_COMPENSATE_SQR) {
                    if (blockEntity.getChannel() > 0) {
                        channels.add(blockEntity.getChannel());
                    }
                }
            }
        }
    }

    private static IntArraySet findNearbyReceivingDeviceChannels(ServerPlayer to) {
        var channels = new IntArraySet();
        if (!CHANNELED_BLOCK_CACHE_S.containsKey(to.level().dimension())) {
            return channels;
        }
        for (var speaker : CHANNELED_BLOCK_CACHE_S.get(to.level().dimension())) {
            if (!(speaker instanceof SpeakerBlockEntity)) {
                continue;
            }
            var pos = speaker.getBlockPos();
            if (to.getEyePosition().distanceToSqr(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f) <= 64 * 64) {
                if (speaker.getChannel() > 0) {
                    channels.add(speaker.getChannel());
                }
            }
        }
        return channels;
    }

    //----------relay on receive----------
    public static final ExecutorService SERVER_RELAY_THREAD = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("Channel-Server-Relay-Thread-%d")
            .setDaemon(true)
            .build());

    public static void process(ServerPlayer from, byte[] opusAudio) {
        if (ChannelServerConfig.get().muteNoneOP && !from.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        SERVER_RELAY_THREAD.execute(() -> {
            try {
                talking(from, opusAudio);
            } catch (Exception e) {
                LogUtils.getLogger().error("Something went wrong, but it should be okay. You can ignore this if nothing else went wrong.");
                LogUtils.getLogger().error(e.getMessage(), e);
            }
        });
    }

    public static void talking(ServerPlayer from, byte[] opusAudio) {
        HashSet<ServerPlayer> tos = new HashSet<>();
        for (var p : from.level().players()) {
            if (closeHear(from, p)) {
                tos.add(p);
            }
        }
        var channels = PLAYER_CHANNELS_SEND.get(from);
        if (channels != null) {
            for (int channel : channels) {
                var players = CHANNEL_PLAYERS_RECEIVE.get(channel);
                if (players != null) {
                    tos.addAll(players);
                }
            }
        }
        tos.forEach(to -> NetworkHelper.sendToPlayer(to, new AudioPacket2C(from.getUUID(), opusAudio, channels)));
    }

    public static boolean closeHear(ServerPlayer from, ServerPlayer to) {
        return to.position().distanceToSqr(from.position()) < 64 * 64
                && (!from.isSpectator() || to.isSpectator());
    }

    public static String dump() {
        StringBuilder text = new StringBuilder();
        text.append("PLAYER_CHANNELS_SEND");
        PLAYER_CHANNELS_SEND.forEach((p, c) -> {
            text.append("\n")
                    .append(p.getScoreboardName())
                    .append("  ")
                    .append(Arrays.toString(c.toIntArray()));
        });
        text.append("\nCHANNEL_PLAYERS_RECEIVE");
        CHANNEL_PLAYERS_RECEIVE.forEach((c, s) -> {
            text.append("\n")
                    .append(c)
                    .append(" ");
            s.forEach(p -> text.append(" ").append(p.getScoreboardName()));
        });
        return text.toString();
    }
}
