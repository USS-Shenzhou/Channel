package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * @author USS_Shenzhou
 */
public class TransmitConfigPanel extends TOptionsPanel {

    public TransmitConfigPanel() {
        var cfg = ChannelClientConfig.get();

        addOptionSplitter(Component.translatable("channel.config.net"));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.net.length"),
                ModConstant.USABLE_FRAME_LENGTH,
                //FIXME change during running
                length -> _ -> ChannelClientConfig.write(c -> c.frameLengthMs = length),
                entry -> entry.getContent() == cfg.frameLengthMs
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.net.length.tooltip")));
        var netSampleRate = addOptionCycleButtonInit(

                Component.translatable("channel.config.net.samplerate"),
                ModConstant.USABLE_NETWORK_SAMPLE_RATE,
                //FIXME change during running
                f -> _ -> ChannelClientConfig.write(c -> c.networkSampleRate = f),
                entry -> entry.getContent() == cfg.networkSampleRate
        ).getB();
        netSampleRate.setTooltip(Tooltip.create(Component.translatable("channel.config.net.samplerate.tooltip")));
    }
}
