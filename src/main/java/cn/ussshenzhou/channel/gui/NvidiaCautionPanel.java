package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.client.send.nativ.NvidiaHelper;
import cn.ussshenzhou.t88.gui.advanced.TLabelButton;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * @author USS_Shenzhou
 */
public class NvidiaCautionPanel extends TLabel {
    private static final String CHINA_LINK = "https://www.nvidia.cn/geforce/broadcasting/broadcast-sdk/resources/";
    private static final String GLOBAL_LINK = "https://www.nvidia.com/en-us/geforce/broadcasting/broadcast-sdk/resources/";

    private TLabelButton downloadButton = null;

    public NvidiaCautionPanel(NvidiaHelper.Stat stat) {
        switch (stat) {
            case NEED_DOWNLOAD -> {
                this.setText(Component.translatable("channel.config.ai.down"));
                var lang = Minecraft.getInstance().getLanguageManager().getSelected();
                var useChinaLink = "lzh".equals(lang) || "zh_cn".equals(lang);
                downloadButton = new TLabelButton(Component.translatable("channel.config.ai.down.here"),
                        _ -> Util.getPlatform().openUri(useChinaLink ? CHINA_LINK : GLOBAL_LINK)
                );
                this.add(downloadButton);
                downloadButton.setBorder(null);
                downloadButton.setTooltip(Tooltip.create(Component.literal(useChinaLink ? CHINA_LINK : GLOBAL_LINK)));
            }
            case UNSUPPORTED_OS -> this.setText(Component.translatable("channel.config.ai.os"));
            case UNSUPPORTED_GPU -> this.setText(Component.translatable("channel.config.ai.gpu"));
            case UNSUPPORTED_DRIVER -> this.setText(Component.translatable("channel.config.ai.driver"));
            case CHANGE_SAMPLE_RATE -> this.setText(Component.translatable("channel.config.ai.sample_rate"));
            case EXCEPTION -> this.setText(Component.translatable("channel.config.ai.exception"));
        }
    }

    @Override
    public void layout() {
        if (downloadButton != null) {
            downloadButton.setBounds(0, 9, downloadButton.getPreferredSize());
        }
        super.layout();
    }
}
