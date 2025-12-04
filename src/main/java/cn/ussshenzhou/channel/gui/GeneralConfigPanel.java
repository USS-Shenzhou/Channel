package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.Unit;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * @author USS_Shenzhou
 */
public class GeneralConfigPanel extends TOptionsPanel {

    public GeneralConfigPanel() {
        var cfg = ChannelClientConfig.get();

        addOptionCycleButtonInit(
                Component.translatable("Channel.config.unit"),
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
    }
}
