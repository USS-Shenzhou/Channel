package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.client.Initializer;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.gui.hud.MicrophoneHud;
import cn.ussshenzhou.t88.gui.notification.TSimpleNotification;
import cn.ussshenzhou.t88.gui.screen.TScreen;
import cn.ussshenzhou.t88.gui.util.LayoutHelper;
import cn.ussshenzhou.t88.gui.widegt.TButton;
import cn.ussshenzhou.t88.gui.widegt.TComponent;
import net.minecraft.network.chat.Component;

public class MicConfirmScreen extends TScreen {
    private final TSimpleNotification notification = new TSimpleNotification(Component.translatable("channel.comfirm"), 0, TSimpleNotification.Severity.INFO) {
        @Override
        public void tickT() {
            tickChildren();
        }
    };

    private final TButton yes = new TButton(Component.literal("YES"), _ -> {
        Initializer.realInit();
        MicConfirmScreen.this.onClose(true);
    });

    private final TButton no = new TButton(Component.literal("NO"), _ -> {
        MicrophoneHud.resumeStatus();
        MicrophoneHud.setStatus(MicrophoneHud.Status.MUTE);
        ChannelClientConfig.write(c -> c.onAir = false);
        MicConfirmScreen.this.onClose(true);
    });

    public MicConfirmScreen() {
        super(Component.literal("Mic Confirm"));
        this.add(notification);
        this.add(yes);
        this.add(no);
    }

    @Override
    public void layout() {
        var notificationSize = notification.getPreferredSize().add(16, 6);
        notification.setBounds((width - notificationSize.x) / 2, height / 2, notificationSize);
        LayoutHelper.BBottomOfA(yes, 4, notification, (notificationSize.x - 4) / 2, 20);
        LayoutHelper.BRightOfA(no, 4, yes);
        super.layout();
    }
}
