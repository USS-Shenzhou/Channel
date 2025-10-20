package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import net.minecraft.network.chat.Component;

/**
 * @author USS_Shenzhou
 */
public class TransmitConfigPanel extends TOptionsPanel {

    public TransmitConfigPanel() {
        var cfg = ChannelClientConfig.get();

    }
}
