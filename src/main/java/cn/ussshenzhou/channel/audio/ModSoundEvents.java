package cn.ussshenzhou.channel.audio;

import cn.ussshenzhou.channel.Channel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author USS_Shenzhou
 */
public class ModSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Channel.MODID);

    public static final Supplier<SoundEvent> PLAYER_TALKING = SOUND_EVENTS.register("player_talking", () -> new SoundEvent(ResourceLocation.fromNamespaceAndPath(Channel.MODID, "player_talking"), Optional.empty()));
}
