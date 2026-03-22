package cn.ussshenzhou.channel.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.BitSet;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = BitSetDiscreteVoxelShape.class, priority = 9999)
public abstract class BitSetDiscreteVoxelShapeMixin extends DiscreteVoxelShape {
    @Unique
    private static final ThreadLocal<BitSetDiscreteVoxelShape> LOCAL_THIS = ThreadLocal.withInitial(() -> new BitSetDiscreteVoxelShape(0, 0, 0));

    protected BitSetDiscreteVoxelShapeMixin(int xSize, int ySize, int zSize) {
        super(xSize, ySize, zSize);
    }

    @Redirect(method = "forAllBoxes", at = @At(value = "NEW", target = "net/minecraft/world/phys/shapes/BitSetDiscreteVoxelShape"), require = 0)
    private static BitSetDiscreteVoxelShape channelUseThreadLocalInstead(DiscreteVoxelShape voxelShape) {
        var instance = LOCAL_THIS.get();

        if (voxelShape.xSize >= 0 && voxelShape.ySize >= 0 && voxelShape.zSize >= 0) {
            instance.xSize = voxelShape.xSize;
            instance.ySize = voxelShape.ySize;
            instance.zSize = voxelShape.zSize;
        } else {
            throw new IllegalArgumentException("Need all positive sizes: x: " + voxelShape.xSize + ", y: " + voxelShape.ySize + ", z: " + voxelShape.zSize);
        }
        if (voxelShape instanceof BitSetDiscreteVoxelShape) {
            instance.storage = (BitSet) ((BitSetDiscreteVoxelShape) voxelShape).storage.clone();
        } else {
            instance.storage = new BitSet(instance.xSize * instance.ySize * instance.zSize);

            for (int x = 0; x < instance.xSize; ++x) {
                for (int y = 0; y < instance.ySize; ++y) {
                    for (int z = 0; z < instance.zSize; ++z) {
                        if (voxelShape.isFull(x, y, z)) {
                            instance.storage.set(instance.getIndex(x, y, z));
                        }
                    }
                }
            }
        }

        instance.xMin = voxelShape.firstFull(Direction.Axis.X);
        instance.yMin = voxelShape.firstFull(Direction.Axis.Y);
        instance.zMin = voxelShape.firstFull(Direction.Axis.Z);
        instance.xMax = voxelShape.lastFull(Direction.Axis.X);
        instance.yMax = voxelShape.lastFull(Direction.Axis.Y);
        instance.zMax = voxelShape.lastFull(Direction.Axis.Z);

        return instance;
    }

}
