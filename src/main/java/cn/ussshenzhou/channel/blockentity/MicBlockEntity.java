package cn.ussshenzhou.channel.blockentity;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MicBlockEntity extends ChanneledBlockEntity {

    public MicBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntityTypes.MIC_BLOCK_ENTITY_TYPE.get(), worldPosition, blockState);
    }
}
