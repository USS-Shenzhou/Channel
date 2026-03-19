package cn.ussshenzhou.channel.audio.client.rt;

import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;

import java.util.IdentityHashMap;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class DebugRayTrace {
    public static final IdentityHashMap<Vector3f, Vec3> whiteRays = new IdentityHashMap<>();

    /*@SubscribeEvent
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
    }*/
}
