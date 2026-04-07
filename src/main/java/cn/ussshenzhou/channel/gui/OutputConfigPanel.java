package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.client.receive.AudioManagerManager;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.config.ChannelPlayerConfig;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.widegt.*;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.joml.Vector2i;

import java.util.*;

/**
 * @author USS_Shenzhou
 */
public class OutputConfigPanel extends TOptionsPanel {

    public OutputConfigPanel() {
        var cfg = ChannelClientConfig.get();
        addOptionSplitter(Component.translatable("channel.config.post"));
        addOptionCycleButtonInit(Component.translatable("channel.config.post.rt"),
                List.of(true, false),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.rayTraceAudio = bool);
                    AudioManagerManager.reset();
                },
                entry -> entry.getContent() == cfg.rayTraceAudio
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.post.rt.tooltip")));
        addOptionSliderDoubleInit(Component.translatable("channel.config.post.delay"),
                20, 2000,
                (_, v) -> Component.literal(v.intValue() + "ms"),
                Component.translatable("channel.config.post.delay.tooltip"),
                (slider, _) -> {
                    ChannelClientConfig.write(c -> c.networkTolerance = (int) slider.getAbsValue());
                },
                cfg.networkTolerance, false

        );

        addOptionSplitter(Component.translatable("channel.config.post.control"));
        addOptionSliderDoubleInit(Component.translatable("channel.config.post.control_adjust"),
                -30, 30,
                (_, v) -> Component.literal(cfg.unit.get(v)),
                Component.translatable("channel.config.post.control_adjust.tooltip"),
                (slider, _) -> {
                    ChannelClientConfig.write(c -> c.outputAdjust = (float) slider.getAbsValue());
                },
                cfg.outputAdjust, false
        );
        addOptionSplitter(Component.translatable("channel.config.post.player_control"));
        addOption(Component.empty(), new TButton(Component.translatable("channel.config.post.player_control_clear"), _ -> {
            ChannelPlayerConfig.clear();
            PlayerVolumePanel.PLAYER_VOLUME.replaceAll((_, _) -> 0f);
        })).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.post.player_control_clear.tooltip")));
        this.container.add(new PlayerVolumePanel());
    }


    @EventBusSubscriber(Dist.CLIENT)
    public static class PlayerVolumePanel extends TPanel {

        private static final LinkedHashMap<UUID, Float> PLAYER_VOLUME = new LinkedHashMap<>();
        private static boolean dirty = true;

        public PlayerVolumePanel() {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                update(Minecraft.getInstance().player.getUUID(), 0);
            }
            dirty = true;
        }

        public static void update(UUID id, float db) {
            if (!PLAYER_VOLUME.containsKey(id) || Math.abs(PLAYER_VOLUME.get(id) - db) > 0.001) {
                dirty = true;
            }
            PLAYER_VOLUME.put(id, db);
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            PLAYER_VOLUME.clear();
            dirty = true;
        }

        private void refresh() {
            if (!dirty) {
                return;
            }
            dirty = false;
            this.children.clear();
            PLAYER_VOLUME.forEach((u, f) -> this.add(new PlayerVolumeBar(u, f)));
            layout();
        }

        @Override
        public Vector2i getPreferredSize() {
            return new Vector2i(0, 20 * PLAYER_VOLUME.size());
        }

        @Override
        public void tickT() {
            refresh();
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
            volume.setTooltip(Tooltip.create(Component.translatable("channel.config.post.player_control.tooltip")));
            name.setHorizontalAlignment(cn.ussshenzhou.t88.gui.util.HorizontalAlignment.RIGHT).setAutoScroll(false);

            var player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
            if (player != null) {
                name.setText(Component.literal(player.getScoreboardName()));
            } else {
                name.setText(Component.literal(uuid.toString()));
            }
            //TODO click face to mute
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int pMouseX, int pMouseY, float pPartialTick) {
            super.extractRenderState(graphics, pMouseX, pMouseY, pPartialTick);
            var playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
            if (playerInfo != null) {
                PlayerFaceExtractor.extractRenderState(graphics, playerInfo.getSkin(), width / 2 - 16, this.getYT() + 2, 16, -1);
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
