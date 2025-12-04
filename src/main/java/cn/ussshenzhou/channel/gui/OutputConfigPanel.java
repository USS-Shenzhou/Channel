package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.audio.NC;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.audio.Vad;
import cn.ussshenzhou.channel.audio.client.send.LevelGatherer;
import cn.ussshenzhou.channel.audio.client.send.MicManager;
import cn.ussshenzhou.channel.audio.client.send.WebRTCHelper;
import cn.ussshenzhou.channel.audio.nativ.NvidiaHelper;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.config.ChannelPlayerConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import cn.ussshenzhou.t88.gui.util.ImageFit;
import cn.ussshenzhou.t88.gui.widegt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.joml.Vector2i;

import javax.sound.sampled.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author USS_Shenzhou
 */
public class OutputConfigPanel extends TOptionsPanel {

    public OutputConfigPanel() {
        var cfg = ChannelClientConfig.get();
        addOptionSplitter(Component.translatable("channel.config.post"));

        addOptionSplitter(Component.translatable("channel.config.post.player_control"));
        this.container.add(new PlayerVolumePanel());
    }


    @EventBusSubscriber(Dist.CLIENT)
    public static class PlayerVolumePanel extends TPanel {

        private static final LinkedHashMap<UUID, Float> PLAYER_VOLUME = new LinkedHashMap<>();
        private static boolean dirty = true;

        public PlayerVolumePanel() {
            add(Minecraft.getInstance().player.getUUID(), 0);
        }

        public static void add(UUID id, float db) {
            PLAYER_VOLUME.put(id, db);
            dirty = true;
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            PLAYER_VOLUME.clear();
            dirty = true;
        }

        private void update() {
            if (!dirty) {
                return;
            }
            dirty = false;
            this.children.clear();
            PLAYER_VOLUME.forEach((u, f) -> this.add(new PlayerVolumeBar(u, f)));
            layout();
        }

        @Override
        public void tickT() {
            update();
            super.tickT();
        }

        @Override
        public void layout() {
            for (int i = 0; i < children.size(); i++) {
                var bar = children.get(i);
                bar.setBounds(0, 24 * i, width, 20);

            }
            super.layout();
        }
    }

    public static class PlayerVolumeBar extends TPanel {
        private final UUID uuid;

        private final TLabel name;
        private final TSlider volume;

        public PlayerVolumeBar(UUID id, float db) {
            this.uuid = id;
            name = new TLabel();
            this.add(name);
            volume = new TSlider(
                    "",
                    -30,
                    30,
                    (_, v) -> Component.literal(ChannelClientConfig.get().unit.get(v)),
                    null,
                    false
            );
            this.add(volume);
            volume.setAbsValueWithoutRespond(db);
            volume.addResponder(d -> {
                var player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
                if (player != null) {
                    ChannelPlayerConfig.set(player, (float) volume.getAbsValue());
                }
            });
            name.setHorizontalAlignment(cn.ussshenzhou.t88.gui.util.HorizontalAlignment.RIGHT).setAutoScroll(false);

            var player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
            if (player != null) {
                name.setText(Component.literal(player.getScoreboardName()));
            } else {
                name.setText(Component.literal(uuid.toString()));
            }
        }

        @Override
        public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
            super.render(graphics, pMouseX, pMouseY, pPartialTick);
            var playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
            if (playerInfo != null) {
                PlayerFaceRenderer.draw(graphics, playerInfo.getSkin().texture(), width / 2 - 16, this.getYT() + 2, 16, playerInfo.showHat(), false, -1);
            }
        }

        @Override
        public void layout() {
            int gapBetweenOptions = 4;
            name.setBounds(0, 0, width / 2 - gapBetweenOptions * 3 - 16, height);
            volume.setBounds(width / 2 + gapBetweenOptions, 0, width / 2 - gapBetweenOptions * 2, height);
            super.layout();
        }
    }
}
