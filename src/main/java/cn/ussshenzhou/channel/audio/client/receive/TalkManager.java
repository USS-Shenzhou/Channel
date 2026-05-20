package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.audio.client.rt.RayTraceManager;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.config.ChannelPlayerConfig;
import cn.ussshenzhou.channel.gui.OutputConfigPanel;
import cn.ussshenzhou.channel.util.AudioHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.lwjgl.openal.AL10.*;

/**
 * @author USS_Shenzhou
 */
public class TalkManager extends BaseAudioManager {
    @Override
    protected boolean play(Level level, UUID playerId, PlayerAudio audio) {
        var pos = audio.getPos(level);
        if (ChannelClientConfig.get().rayTraceAudio) {
            RayTraceManager.play(pos.x, pos.y, pos.z, audio);
        } else {
            simplePlay(audio, pos);
        }
        return false;
    }

    private void simplePlay(PlayerAudio audio, Vec3 pos) {
        alSource3f(audio.alSource, AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
        audio.play();
    }
}
