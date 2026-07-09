package cn.ussshenzhou.channel.gui.hud;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.audio.client.send.LevelGatherer;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.gui.HudManager;
import cn.ussshenzhou.t88.gui.widegt.TImage;
import cn.ussshenzhou.t88.gui.widegt.TPanel;
import cn.ussshenzhou.t88.gui.widegt.TProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class MicrophoneHud extends TPanel {

    @SubscribeEvent
    public static void onPlayerIn(ClientPlayerNetworkEvent.LoggingIn event) {
        HudManager.addOrReplaceIfSameClassExist(new MicrophoneHud());
    }

    @SubscribeEvent
    public static void onPlayerOut(ClientPlayerNetworkEvent.LoggingOut event) {
        HudManager.removeInstanceOf(MicrophoneHud.class);
    }

    private static Status status = ChannelClientConfig.get().onAir ? Status.STANDBY : Status.MUTE;
    private static Status prevStatus = ChannelClientConfig.get().onAir ? Status.STANDBY : Status.MUTE;
    private final TImage microphone = new TImage(Identifier.fromNamespaceAndPath(Channel.MODID, "textures/gui/microphone.png"));
    private final TImage outline = new TImage(Identifier.fromNamespaceAndPath(Channel.MODID, "textures/gui/outline.png"));
    private final TImage color = new TImage(Identifier.fromNamespaceAndPath(Channel.MODID, "textures/gui/color.png"));
    private final TProgressBar volume = new TProgressBar(ModConstant.ABS_MIN_DBFS);
    private final TImage word = new TImage(Identifier.fromNamespaceAndPath(Channel.MODID, "textures/gui/standby.png"));
    private final TImage shadow = new TImage(Identifier.fromNamespaceAndPath(Channel.MODID, "textures/gui/standby.png"));
    private final TImage slash = new TImage(Identifier.fromNamespaceAndPath(Channel.MODID, "textures/gui/slash.png"));
    private boolean render = true;

    public MicrophoneHud() {
        this.add(microphone);

        outline.setAlpha(0.5f);
        this.add(outline);

        this.add(color);

        volume.setTextMode(TProgressBar.TextMode.NONE);
        volume.setType(TProgressBar.Type.VERTICAL);
        volume.setProgressBarColorGradient(0x103c91ff, 0xff3c91ff);
        this.add(volume);

        shadow.setAlpha(0.75f);
        shadow.setColor(0x000000);
        this.add(shadow);
        this.add(word);

        this.add(slash);
    }

    @Override
    public void tickT() {
        if (getStatus() == Status.OP && Minecraft.getInstance().player != null && Minecraft.getInstance().player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            resumeStatus();
        }
        volume.setValue(Mth.clamp(ModConstant.ABS_MIN_DBFS + AudioHelper.s2dbfs(LevelGatherer.getProcessed()), 0, ModConstant.ABS_MIN_DBFS));
        var cfg = ChannelClientConfig.get();
        microphone.setVisibleT(cfg.showHudIcon);
        outline.setVisibleT(cfg.showHudIcon);
        color.setVisibleT(cfg.showHudIcon);
        volume.setVisibleT(cfg.showHudIcon);

        word.setVisibleT(cfg.showHudText);
        shadow.setVisibleT(cfg.showHudText);

        render = !(Minecraft.getInstance().screen instanceof ChatScreen);
        super.tickT();
    }

    @Override
    public void resizeAsHud(int screenWidth, int screenHeight) {
        var cfg = ChannelClientConfig.get();
        var x0 = cfg.showHudIcon ? 4 : -7;
        this.setAbsBounds(x0, screenHeight - 22, 9, 20);
        super.resizeAsHud(screenWidth, screenHeight);
    }

    @Override
    public void layout() {
        this.microphone.setBounds(0, 0, width, height);
        this.outline.setBounds(0, 0, width, height);
        this.color.setBounds(0, 0, width, height);
        this.volume.setBounds(3, 2, 3, 11);
        this.word.setBounds(9, 15, 31, 4);
        this.shadow.setBounds(10, 16, 31, 4);
        this.slash.setBounds(-3, 2, 15, 19);
        super.layout();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float pPartialTick) {
        if (!render) {
            return;
        }
        var word = Identifier.fromNamespaceAndPath(Channel.MODID, "textures/gui/" + status.name().toLowerCase() + ".png");
        this.word.setImageLocation(word);
        this.shadow.setImageLocation(word);
        this.word.setColor(switch (status) {
            case STANDBY -> 0x3c91ff;
            case TALKING -> 0x00ff00;
            case ERROR, MUTE, OP -> 0xff0000;
            case VAD_FAIL, VOLUME_FAIL -> 0xff9933;
            case SUBSPACE -> 0x9966ff;
        });
        this.color.setColor(switch (status) {
            case STANDBY, MUTE, SUBSPACE, OP -> 0x404040;
            case TALKING -> 0x00ff00;
            case ERROR -> 0xff0000;
            case VAD_FAIL, VOLUME_FAIL -> 0xff9933;
        });
        var cfg = ChannelClientConfig.get();
        this.slash.setVisibleT(cfg.showHudIcon && (status == Status.MUTE || status == Status.ERROR || status == Status.SUBSPACE || status == Status.OP));
        super.extractRenderState(graphics, mouseX, mouseY, pPartialTick);
    }

    public static void setStatus(Status status) {
        if (MicrophoneHud.status != status) {
            MicrophoneHud.prevStatus = MicrophoneHud.status;
            MicrophoneHud.status = status;
        }
    }

    public static void resumeStatus() {
        MicrophoneHud.status = MicrophoneHud.prevStatus;
    }

    public static Status getStatus() {
        return status;
    }

    public enum Status {
        STANDBY,
        MUTE,
        TALKING,
        ERROR,
        VAD_FAIL,
        VOLUME_FAIL,
        SUBSPACE,
        OP
    }

}
