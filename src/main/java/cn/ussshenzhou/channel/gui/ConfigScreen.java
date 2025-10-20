package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.t88.gui.container.TTabPageContainer;
import cn.ussshenzhou.t88.gui.screen.TScreen;
import cn.ussshenzhou.t88.gui.util.LayoutHelper;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import net.minecraft.network.chat.Component;

/**
 * @author USS_Shenzhou
 */
public class ConfigScreen extends TScreen {
    private final TLabel title = new TLabel(Component.translatable("channel.config.title"));
    private final TTabPageContainer tabs = new TTabPageContainer();

    public ConfigScreen() {
        super(Component.literal("Channel Mod Config Screen"));
        this.add(title);
        title.setFontSize((float) (TLabel.STD_FONT_SIZE * 1.5));

        this.add(tabs);
        var input = tabs.newTab(Component.translatable("channel.config.tab.input"), new InputConfigPanel());
        input.setCloseable(false);
        var transmit = tabs.newTab(Component.translatable("channel.config.tab.transmit"), new TransmitConfigPanel());
        transmit.setCloseable(false);
        var output = tabs.newTab(Component.translatable("channel.config.tab.output"), new OutputConfigPanel());
        output.setCloseable(false);
    }

    @Override
    public void layout() {
        title.setBounds(4, 4, width - 8, title.getPreferredSize().y);
        LayoutHelper.BBottomOfA(tabs, 4, title, width - 8, height - title.getYT() - title.getHeight() - 8);
        super.layout();
    }
}
