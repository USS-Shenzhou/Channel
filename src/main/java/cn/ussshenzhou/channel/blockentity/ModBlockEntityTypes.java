package cn.ussshenzhou.channel.blockentity;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


/**
 * @author USS_Shenzhou
 */
public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Channel.MODID);

    public static final Supplier<BlockEntityType<MicBlockEntity>> MIC_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register("mic", () -> new BlockEntityType<>(
            MicBlockEntity::new, ModBlocks.MIC_BLOCK.get()
    ));

    public static final Supplier<BlockEntityType<SpeakerBlockEntity>> SPEAKER_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register("speaker", () -> new BlockEntityType<>(
            SpeakerBlockEntity::new,
            ModBlocks.SPEAKER_BLOCK.get(),
            ModBlocks.SPEAKER_STAND_BLOCK.get(),
            ModBlocks.SPEAKER_HANG_BLOCK.get()
    ));

}
