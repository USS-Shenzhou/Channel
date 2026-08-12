package cn.ussshenzhou.channel.util;

import net.minecraft.client.Minecraft;

public class CompatHelper {

    public static final Class<?> MEK_BLOCK_CLASS;
    public static final Class<?> REPLAY_CAMERA_CLASS;

    static {
        Class<?> mek;
        try {
            mek = Class.forName("mekanism.common.block.BlockMekanism");
        } catch (ClassNotFoundException e) {
            mek = null;
        }
        MEK_BLOCK_CLASS = mek;

        Class<?> replayCamera;
        try {
            replayCamera = Class.forName("com.replaymod.replay.camera.CameraEntity");
        } catch (ClassNotFoundException e) {
            replayCamera = null;
        }
        REPLAY_CAMERA_CLASS = replayCamera;
    }

    public static boolean isClientLevelValid() {
        return Minecraft.getInstance().level != null && (REPLAY_CAMERA_CLASS == null || !REPLAY_CAMERA_CLASS.isInstance(Minecraft.getInstance().getCameraEntity()));
    }
}
