package cn.ussshenzhou.channel.audio.client.rt;

import cn.ussshenzhou.channel.config.ChannelClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class DebugRayTrace {
    public static final ArrayList<Ray> rays = new ArrayList<>();

    @SubscribeEvent
    public static void render(RenderLevelStageEvent.AfterTranslucentParticles event) {
        if (!ChannelClientConfig.get().showRaytrace) {
            return;
        }
        var posestack = event.getPoseStack();
        posestack.pushPose();
        posestack.translate(Minecraft.getInstance().gameRenderer.getMainCamera().position().scale(-1));
        for (var ray : rays) {
            Gizmos.line(ray.from, ray.to, ray.color, 1f);
        }
        posestack.popPose();
    }

    public record Ray(Vec3 from, Vec3 to, int color) {

    }
}
