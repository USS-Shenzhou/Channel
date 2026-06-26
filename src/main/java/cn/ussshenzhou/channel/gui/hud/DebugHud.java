package cn.ussshenzhou.channel.gui.hud;

import cn.ussshenzhou.t88.gui.util.HorizontalAlignment;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import cn.ussshenzhou.t88.gui.widegt.TPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static cn.ussshenzhou.channel.audio.client.rt.RayTraceCalculator.*;

public class DebugHud extends TPanel {
    private final TLabel raytraceData = new TLabel();

    public DebugHud() {
        this.add(raytraceData);
        raytraceData.setHorizontalAlignment(HorizontalAlignment.LEFT);
    }

    @Override
    public void layout() {
        raytraceData.setBounds(0, 0, 200, 100);
        super.layout();
    }

    @Override
    public void resizeAsHud(int screenWidth, int screenHeight) {
        this.setAbsBounds(50, 50, 200, 100);
        super.resizeAsHud(screenWidth, screenHeight);
    }

    @Override
    public void tickT() {
        raytraceData.setText(Component.literal(String.format("""
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
                        
                        Open Space Correction: %.3f""",
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

                getOpenSpaceCorrection()
        )));
        super.tickT();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float pPartialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, pPartialTick);
    }
}
