package cn.ussshenzhou.channel.audio.client.rt;

import net.minecraft.world.phys.Vec3;

public record HitPoint(
        int round,
        Vec3 pos,
        float journey,
        float distance,
        BlockSoundProperty soundProperty
) {


    public HitPoint(int round, Vec3 pos, float journey, float distance, BlockSoundProperty soundProperty) {
        this.round = round;
        this.pos = pos;
        this.journey = journey;
        this.distance = distance;
        this.soundProperty = soundProperty;
    }

    public double weight() {
        float stdJourney = Math.max(1, journey);
        return 1 / (stdJourney * stdJourney);
    }
}
