package cn.ussshenzhou.channel.audio.client.receive;

import cn.ussshenzhou.channel.audio.ModSoundEvents;
import cn.ussshenzhou.channel.util.DumbAudioStream;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PlayerTalkSoundInstance extends AbstractSoundInstance {
    public final int player;

    public PlayerTalkSoundInstance(Player player) {
        super(ModSoundEvents.PLAYER_TALKING.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player.getId();
    }

    public Optional<Player> getPlayer() {
        if (Minecraft.getInstance().level.getEntity(player) instanceof Player p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    @Override
    public float getVolume() {
        return 1;
    }

    @Override
    public float getPitch() {
        return 1;
    }

    @Override
    public double getX() {
        return getPlayer().map(p -> p.position().x).orElse(0d);
    }

    @Override
    public double getY() {
        return getPlayer().map(p -> p.position().y).orElse(0d);
    }

    @Override
    public double getZ() {
        return getPlayer().map(p -> p.position().z).orElse(0d);
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        if (getPlayer().isEmpty()) {
            PlayerTalkAudioStreamManager.remove(player);
            return CompletableFuture.completedFuture(DumbAudioStream.INSTANCE);
        }
        return CompletableFuture.completedFuture(PlayerTalkAudioStreamManager.get(this.getPlayer().get()));
    }
}
