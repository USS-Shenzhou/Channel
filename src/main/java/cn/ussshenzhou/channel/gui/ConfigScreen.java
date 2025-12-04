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
    final TTabPageContainer tabs = new TTabPageContainer();
    private TTabPageContainer.Tab input, transmit, output, general;

    public ConfigScreen() {
        super(Component.literal("Channel Mod Config Screen"));
        this.add(title);
        title.setFontSize((float) (TLabel.STD_FONT_SIZE * 1.5));
        this.add(tabs);
        tInit();
    }

    public void tInit() {
        input = tabs.newTab(Component.translatable("channel.config.tab.input"), new InputConfigPanel());
        input.setCloseable(false);

        transmit = tabs.newTab(Component.translatable("channel.config.tab.transmit"), new TransmitConfigPanel());
        transmit.setCloseable(false);

        output = tabs.newTab(Component.translatable("channel.config.tab.output"), new OutputConfigPanel());
        output.setCloseable(false);

        general = tabs.newTab(Component.translatable("channel.config.tab.general"), new GeneralConfigPanel());
        general.setCloseable(false);
    }

    public void forceUpdate() {
        this.tabs.removeTab(input);
        this.tabs.removeTab(transmit);
        this.tabs.removeTab(output);
        this.tabs.removeTab(general);
        this.tInit();
    }

    @Override
    public void layout() {
        title.setBounds(4, 4, width - 8, title.getPreferredSize().y);
        LayoutHelper.BBottomOfA(tabs, 4, title, width - 8, height - title.getYT() - title.getHeight() - 8);
        super.layout();
    }
}
