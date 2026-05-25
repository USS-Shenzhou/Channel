package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.Unit;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.gui.hud.DebugHud;
import cn.ussshenzhou.t88.gui.HudManager;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.event.ResizeHudEvent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * @author USS_Shenzhou
 */
public class GeneralConfigPanel extends TOptionsPanel {

    public GeneralConfigPanel() {
        var cfg = ChannelClientConfig.get();

        addOptionSplitter(Component.translatable("channel.config.dispay"));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.dispay.hud"),
                List.of(false, true),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.showHudIcon = bool);
                    NeoForge.EVENT_BUS.post(new ResizeHudEvent());
                },
                entry -> entry.getContent() == cfg.showHudIcon
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.dispay.hud.tooltip")));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.dispay.hud_text"),
                List.of(false, true),
                bool -> _ -> {
                    ChannelClientConfig.write(c -> c.showHudText = bool);
                },
                entry -> entry.getContent() == cfg.showHudText
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.dispay.hud.tooltip")));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.unit"),
                List.of(Unit.values()),
                u -> _ -> {
                    ChannelClientConfig.write(c -> c.unit = u);
                    if (this.getTopParentScreen() instanceof ConfigScreen configScreen) {
                        configScreen.forceUpdate();
                        configScreen.tabs.selectTab(3);
                    }
                },
                entry -> entry.getContent() == cfg.unit
        );

        addOptionSplitter(Component.translatable("channel.config.debug"));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.debug.raytrace"),
                List.of(false, true),
                bool -> _ -> ChannelClientConfig.write(c -> c.showRaytrace = bool),
                entry -> entry.getContent() == cfg.showRaytrace
        );
        addOptionCycleButtonInit(
                Component.translatable("channel.config.debug.info"),
                List.of(false, true),
                bool -> _ -> {
                    if (bool) {
                        HudManager.addIfSameClassNotExist(new DebugHud());
                    } else {
                        HudManager.removeInstanceOf(DebugHud.class);
                    }
                },
                entry -> entry.getContent() == HudManager.getChildren().stream().anyMatch(t -> t instanceof DebugHud)
        );
    }
}
