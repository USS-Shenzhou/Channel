package cn.ussshenzhou.channel.audio.client.rt;

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class DebugRayTrace {
    public static final IdentityHashMap<Vector3f, Vec3> whiteRays = new IdentityHashMap<>();

    @SubscribeEvent
    public static void render(RenderLevelStageEvent.AfterEntities event) {
        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            return;
        }
        var buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());
        var posestack = event.getPoseStack();
        posestack.pushPose();
        posestack.translate(event.getCamera().getPosition().scale(-1));
        for (var entry : whiteRays.entrySet()) {
            var from = entry.getKey();
            var ray = entry.getValue();
            ShapeRenderer.renderVector(posestack, buffer, from, ray, 0x80ffffff);
        }
        posestack.popPose();
    }
}
