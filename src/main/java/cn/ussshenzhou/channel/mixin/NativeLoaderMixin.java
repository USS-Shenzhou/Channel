package cn.ussshenzhou.channel.mixin;

import dev.onvoid.webrtc.internal.NativeLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = NativeLoader.class, remap = false)
public class NativeLoaderMixin {

    @Inject(method = "loadLibrary", at = @At(value = "HEAD"), cancellable = true)
    private static void channelCancelLoad(String libName, CallbackInfo ci) {
        ci.cancel();
    }
}
