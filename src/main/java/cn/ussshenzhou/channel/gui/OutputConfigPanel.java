package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.client.receive.AudioManager;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.config.ChannelPlayerConfig;
import cn.ussshenzhou.t88.gui.advanced.TLabelButton;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.util.Border;
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
                    AudioManager.reset();
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
        addOptionCycleButtonInit(Component.translatable("channel.config.post.hearself"),
                List.of(true, false),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.hearMyself = bool);
                    AudioManager.reset();
                },
                entry -> entry.getContent() == cfg.hearMyself
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.post.hearself.tooltip")));

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
        addOptionCycleButtonInit(Component.translatable("channel.config.post.mute_all"),
                List.of(true, false),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.muteAll = bool);
                    refreshVolumePanel();
                },
                entry -> entry.getContent() == cfg.muteAll
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.post.mute_all.tooltip")));
        addOptionSplitter(Component.translatable("channel.config.post.player_control"));
        addOption(Component.empty(), new TButton(Component.translatable("channel.config.post.player_control_clear"), _ -> {
            ChannelPlayerConfig.clear();
            refreshVolumePanel();
        })).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.post.player_control_clear.tooltip")));
        this.container.add(new PlayerVolumePanel());
    }

    private void refreshVolumePanel() {
        this.container.getChildren().forEach(tWidget -> {
            if (tWidget instanceof PlayerVolumePanel playerVolumePanel) {
                playerVolumePanel.getChildren().forEach(t -> {
                    if (t instanceof PlayerVolumeBar playerVolumeBar) {
                        playerVolumeBar.update();
                    }
                });
            }
        });
    }


    @EventBusSubscriber(Dist.CLIENT)
    public static class PlayerVolumePanel extends TPanel {

        private static final LinkedHashSet<UUID> PLAYERS = new LinkedHashSet<>();
        private static boolean dirty = true;

        public PlayerVolumePanel() {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                update(Minecraft.getInstance().player.getUUID());
            }
            dirty = true;
        }

        public static void update(UUID id) {
            if (!PLAYERS.contains(id)) {
                dirty = true;
            }
            PLAYERS.add(id);
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            PLAYERS.clear();
            dirty = true;
        }

        private void refresh() {
            if (!dirty) {
                return;
            }
            dirty = false;
            this.children.clear();
            PLAYERS.forEach(u -> this.add(new PlayerVolumeBar(u)));
            layout();
        }

        @Override
        public Vector2i getPreferredSize() {
            return new Vector2i(0, 20 * PLAYERS.size());
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

        private TLabelButton muteButton;
        private final TLabel name;
        private final TSlider volume;

        public PlayerVolumeBar(UUID id) {
            this.uuid = id;
            name = new TLabel();
            this.add(name);
            volume = new TSlider(
                    "",
                    -30,
                    30,
                    (_, v) -> Component.literal(ChannelClientConfig.get().unit.get(v)),
                    null,
                    true
            );
            this.add(volume);
            var db = ChannelPlayerConfig.getOrDefault(uuid);
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

            muteButton = new TLabelButton(Component.empty(), b -> {
                if (ChannelPlayerConfig.muted(uuid)) {
                    ChannelPlayerConfig.unmute(uuid);
                    muteButton.setBorder(null);
                    muteButton.setNormalBackGround(0x0);
                    this.volume.setVisibleT(true);
                } else {
                    ChannelPlayerConfig.mute(uuid);
                    muteButton.setBorder(new Border(0xffff0000, 1));
                    muteButton.setNormalBackGround(0x80ff0000);
                    this.volume.setVisibleT(false);
                }
            });
            update();
            muteButton.setBorder(null);
            muteButton.setTooltip(Tooltip.create(Component.translatable("channel.config.post.mute.tooltip")));
            this.add(muteButton);
            //TODO show player volume under silder
        }

        protected void update() {
            if (ChannelPlayerConfig.muted(uuid)) {
                muteButton.setBorder(new Border(0xffff0000, 1));
                muteButton.setNormalBackGround(0x80ff0000);
                this.volume.setVisibleT(false);
            } else {
                muteButton.setBorder(null);
                muteButton.setNormalBackGround(0x0);
                this.volume.setVisibleT(true);
            }
            this.volume.setAbsValueWithoutRespond(ChannelPlayerConfig.getOrDefault(uuid));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int pMouseX, int pMouseY, float pPartialTick) {
            var playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
            if (playerInfo != null) {
                PlayerFaceExtractor.extractRenderState(graphics, playerInfo.getSkin(), width / 2 - 16, this.getYT() + 2, 16, -1);
            }
            super.extractRenderState(graphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public void layout() {
            int gapBetweenOptions = 4;
            name.setBounds(0, 0, width / 2 - gapBetweenOptions * 3 - 16, height);
            volume.setBounds(width / 2 + gapBetweenOptions, 0, width / 2 - gapBetweenOptions * 2, height);
            muteButton.setAbsBounds(width / 2 - 16, this.getYT() + 2, 16, 16);
            super.layout();
        }
    }
}
