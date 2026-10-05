package cn.ussshenzhou.channel.Item;

import cn.ussshenzhou.channel.gui.ItemChannelSettingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class WalkieTalkie extends Item {
    public WalkieTalkie(Properties properties) {
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
                Minecraft.getInstance().setScreenAndShow(new ItemChannelSettingScreen(hand, player.getItemInHand(hand)));
                return InteractionResult.SUCCESS;
            }
        }.get();

    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("item.channel.walkie_talkie.tooltip"));
    }
}
