package cn.ussshenzhou.channel.gui.hud;

import cn.ussshenzhou.channel.audio.DebugManager;
import cn.ussshenzhou.t88.gui.util.HorizontalAlignment;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import cn.ussshenzhou.t88.gui.widegt.TPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static cn.ussshenzhou.channel.audio.client.rt.RayTraceCalculator.*;

public class DebugHud extends TPanel {
    private final TLabel textData = new TLabel();

    public DebugHud() {
        this.add(textData);
        textData.setHorizontalAlignment(HorizontalAlignment.LEFT);
    }

    @Override
    public void layout() {
        textData.setBounds(0, 0, width, height);
        super.layout();
    }

    @Override
    public void resizeAsHud(int screenWidth, int screenHeight) {
        this.setAbsBounds(10, 10, screenWidth - 10, screenHeight);
        super.resizeAsHud(screenWidth, screenHeight);
    }

    @Override
    public void tickT() {
        textData.setText(Component.literal(String.format("""
                        Density: %.3f
                        Diffusion: %.3f
                        HF Gain: %.3f
                        RT60: %.3f
                        Early Ref. Gain: %.3f
                        Early Ref. Delay: %.3f
                        Late Ref. Gain: %.3f
                        Late Ref. Delay: %.3f
                        Echo Time: %.3f
                        Echo Depth: %.3f
                        
                        Open Space Correction: %.3f
                        
                        Sending Interval: %s
                        ICMP Ping: %s
                        Playing Interval: %s
                        Playing Hard Reset: §b%d
                        OpenAL Re-play: §b%d
                        Relay Ping: %s
                        
                        Receive Interval:
                        %s""",
                getDensity(),
                getDiffusion(),
                getHfGain(),
                getRt60(),
                getEarlyRefGain(),
                getEarlyRefDelay(),
                getLateRefGain(),
                getLateRefDelay(),
                getEchoTime(),
                getEchoDepth(),

                getOpenSpaceCorrection(),

                DebugManager.MIC_SEND_COUNTER.toString(),
                DebugManager.ICMP_PING.getStringAsMs(),
                DebugManager.PLAY_COUNTER.toString(),
                DebugManager.PLAY_RESET_COUNTER.count(),
                DebugManager.OPENAL_REPLAY_COUNTER.count(),
                DebugManager.RELAY_PING.getStringAsMs(),

                getReceiveText()
        )));
        super.tickT();
    }

    private String getReceiveText() {
        StringBuilder text = new StringBuilder();
        DebugManager.RECEIVE_COUNTER.forEach((uuid, counter) -> {
            text.append("    ");
            var player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
            if (player != null) {
                text.append(player.getScoreboardName());
            } else {
                text.append(uuid.toString());
            }
            text.append("    ").append(counter.toString());
        });
        return text.toString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float pPartialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, pPartialTick);
    }
}
