package cn.ussshenzhou.channel.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MicBlockEntity extends ChanneledBlockEntity {

    public MicBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntityTypes.MIC_BLOCK_ENTITY_TYPE.get(), worldPosition, blockState);
    }
}
