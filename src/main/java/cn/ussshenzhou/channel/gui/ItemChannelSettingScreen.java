package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.Item.ModItems;
import cn.ussshenzhou.channel.blockentity.ChanneledBlockEntity;
import cn.ussshenzhou.channel.network.SetBlockChannelPacket;
import cn.ussshenzhou.channel.network.SetItemChannelPacket;
import cn.ussshenzhou.t88.gui.advanced.TConstrainedEditBox;
import cn.ussshenzhou.t88.gui.screen.TScreen;
import cn.ussshenzhou.t88.gui.util.LayoutHelper;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import cn.ussshenzhou.t88.gui.widegt.TSlider;
import cn.ussshenzhou.t88.network.NetworkHelper;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ItemChannelSettingScreen extends TScreen {
    private final TLabel title;
    private final TConstrainedEditBox channelEditBox;
    private TSlider channelSlider;
    private boolean updated = false;
    private InteractionHand hand;

    public ItemChannelSettingScreen(InteractionHand hand, ItemStack itemStack) {
        super(Component.literal("ChannelSettingScreen"));
        this.hand = hand;
        this.title = new TLabel(Component.translatable("channel.setting.title"));
        this.add(title);
        this.channelEditBox = new TConstrainedEditBox() {
            @Override
            public void checkAndThrow(String value) throws CommandSyntaxException {
                if (getUnset().equals(value)) {
                    return;
                }
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    var msg = new LiteralMessage("");
                    throw new CommandSyntaxException(new SimpleCommandExceptionType(msg), msg);
                }
            }
        };
        var channel = itemStack.getOrDefault(ModItems.CHANNEL.get(), 0);
        channelEditBox.setValue(channel == 0 ? getUnset() : String.valueOf(channel));
        channelEditBox.addResponder(s -> {
            if (channelEditBox.check(s)) {
                var zero = getUnset();
                if (zero.equals(s)) {
                    return;
                }
                if ("0".equals(s)) {
                    channelEditBox.setValue(zero);
                } else {
                    var c = Integer.parseInt(s);
                    channelSlider.setAbsValueWithoutRespond(c);
                }
            }
            updated = true;
        });
        this.add(channelEditBox);
        this.channelSlider = new TSlider("", 0, 64, false, null, false);
        this.channelSlider.addResponder(_ -> {
            int c = (int) channelSlider.getAbsValue();
            if (c != getEditBoxValue()) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.85f, 0.2f));
            }
            channelEditBox.setValue(String.valueOf(c));
            updated = true;
        });
        if (channel >= 0 && channel <= 64) {
            this.channelSlider.setAbsValueWithoutRespond(channel);
        } else {
            this.channelSlider.setAbsValueWithoutRespond(0);
        }

        this.add(channelSlider);
    }

    private String getUnset() {
        return Component.translatable("channel.setting.none").getString();
    }

    private int getEditBoxValue() {
        var s = channelEditBox.getValue();
        if (channelEditBox.check(s)) {
            return s.equals(getUnset()) ? 0 : Integer.parseInt(s);
        }
        return -1;
    }

    @Override
    public void tick() {
        if (updated) {
            var s = channelEditBox.getValue();
            if (channelEditBox.check(s)) {
                NetworkHelper.sendToServer(new SetItemChannelPacket(hand, getEditBoxValue()));
            }
            updated = false;
        }
        super.tick();
    }

    @Override
    public void layout() {
        this.channelEditBox.setBounds(width / 2 - 100, height / 2 - 10, 200, 20);
        LayoutHelper.BTopOfA(title, 4, channelEditBox);
        LayoutHelper.BBottomOfA(channelSlider, 4, channelEditBox);
        super.layout();
    }
}
