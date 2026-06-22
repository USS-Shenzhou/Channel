package cn.ussshenzhou.channel.Item;

import cn.ussshenzhou.channel.gui.ItemChannelSettingScreen;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HandheldMic extends Item {
    public HandheldMic(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            return InteractionResult.PASS;
        }
        return new Supplier<InteractionResult>() {
            @Override
            public InteractionResult get() {
                Minecraft.getInstance().setScreen(new ItemChannelSettingScreen(hand, player.getItemInHand(hand)));
                return InteractionResult.SUCCESS;
            }
        }.get();

    }
}
