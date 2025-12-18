package cn.ussshenzhou.channel.audio.client.rt;

import net.minecraft.world.phys.Vec3;

public record SourceAudioData(
        float directGain,
        float directHF,
        float reverbGain,
        Vec3 virtualPos
) {
}
