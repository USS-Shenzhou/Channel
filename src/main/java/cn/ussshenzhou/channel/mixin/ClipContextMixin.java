package cn.ussshenzhou.channel.mixin;

import cn.ussshenzhou.channel.util.CompatHelper;
import cn.ussshenzhou.channel.util.ModTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClipContext.Block.class)
enum ClipContextMixin {
    CHANNEL_OUTLINE((state, level, pos, context) -> {
        if (state.is(ModTags.AUDIO_SOURCE)) {
            return Shapes.empty();
        }
        if (CompatHelper.MEK_BLOCK_CLASS != null && CompatHelper.MEK_BLOCK_CLASS.isInstance(state.getBlock())) {
            return Shapes.block();
        }
        return state.getShape(level, pos, context);
    }),
    CHANNEL_VISUAL((state, level, pos, context) -> {
        if (state.is(ModTags.AUDIO_SOURCE)) {
            return Shapes.empty();
        }
        if (CompatHelper.MEK_BLOCK_CLASS != null && CompatHelper.MEK_BLOCK_CLASS.isInstance(state.getBlock())) {
            return Shapes.block();
        }
        return state.getVisualShape(level, pos, context);
    });


    @Shadow
    private ClipContextMixin(ClipContext.ShapeGetter getShape) {
    }
}
