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
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import cn.ussshenzhou.t88.gui.util.ImageFit;
import cn.ussshenzhou.t88.gui.widegt.TCycleButton;
import cn.ussshenzhou.t88.gui.widegt.TImage;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import cn.ussshenzhou.t88.gui.widegt.TProgressBar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector2i;

import javax.sound.sampled.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author USS_Shenzhou
 */
public class OutputConfigPanel extends TOptionsPanel {

    public OutputConfigPanel() {
        var cfg = ChannelClientConfig.get();
        addOptionSplitter(Component.translatable("channel.config.post"));

    }
}
