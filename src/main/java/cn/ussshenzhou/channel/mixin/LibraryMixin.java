package cn.ussshenzhou.channel.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.Library;
import org.lwjgl.openal.ALC11;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.IntBuffer;

@Mixin(Library.class)
public class LibraryMixin {

    @ModifyConstant(method = "createAttributes",constant = @Constant(intValue = 11))
    private int channelBiggerBuffer(int constant){
        return 21;
    }

    @Inject(method = "createAttributes", at = @At(value = "INVOKE", target = "Ljava/nio/IntBuffer;put(I)Ljava/nio/IntBuffer;", ordinal = 5, shift = At.Shift.AFTER))
    private void channelSupportMoreSources(MemoryStack stack, boolean enableHrtf, CallbackInfoReturnable<IntBuffer> cir, @Local(name = "attr") IntBuffer attr) {
        attr.put(ALC11.ALC_MONO_SOURCES).put(1024);
    }
}
