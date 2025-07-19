package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.t88.gui.screen.TScreen;
import cn.ussshenzhou.t88.gui.util.LayoutHelper;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import net.minecraft.network.chat.Component;

/**
 * @author USS_Shenzhou
 */
public class ConfigScreen extends TScreen {
    private final TLabel title = new TLabel(Component.literal("Channel Config"));
    private final ConfigPanel panel = new ConfigPanel();

    public ConfigScreen() {
        super(Component.literal("Channel Mod Config Screen"));
        this.add(title);
        title.setFontSize(TLabel.STD_FONT_SIZE * 2);
        this.add(panel);
    }

    @Override
    public void layout() {
        title.setBounds(4, 4, width - 8, title.getPreferredSize().y);
        LayoutHelper.BBottomOfA(panel, 4, title, width - 8, height - title.getYT() - title.getHeight() - 8);
        super.layout();
    }
}
