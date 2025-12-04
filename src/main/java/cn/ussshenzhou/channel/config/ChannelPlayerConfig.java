package cn.ussshenzhou.channel.config;

import cn.ussshenzhou.channel.gui.OutputConfigPanel;
import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.config.TConfig;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("FieldMayBeFinal")
public class ChannelPlayerConfig implements TConfig {

    private HashMap<String, Float> playerVolumeAdjust = new HashMap<>();

    public static float getOrDefault(Player player) {
        float r = get().playerVolumeAdjust.computeIfAbsent(player.getGameProfile().getName(), name -> 0f);
        OutputConfigPanel.PlayerVolumePanel.add(player.getUUID(), r);
        return r;
    }

    public static void set(Player player, float db) {
        write(thiz -> thiz.playerVolumeAdjust.put(player.getGameProfile().getName(), db));
    }

    private static ChannelPlayerConfig get() {
        return ConfigHelper.getConfigRead(ChannelPlayerConfig.class);
    }

    private static void write(Consumer<ChannelPlayerConfig> writer) {
        ConfigHelper.getConfigWrite(ChannelPlayerConfig.class, writer);
    }
}
