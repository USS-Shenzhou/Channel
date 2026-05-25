package cn.ussshenzhou.channel.input;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.audio.Trigger;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.gui.ConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(Dist.CLIENT)
public class ModKeyMappingRegistry {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Channel.MODID, "channel"));
    public static final KeyMapping CONFIG = new KeyMapping(
            "key.channel.config_screen", KeyConflictContext.UNIVERSAL, KeyModifier.ALT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY
    );
    public static final KeyMapping PTT = new KeyMapping(
            "key.channel.ptt", KeyConflictContext.UNIVERSAL, KeyModifier.NONE,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY
    );
    private static long lastSwitch = 0;

    @SubscribeEvent
    public static void onClientSetup(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappingRegistry.CONFIG);
        event.register(ModKeyMappingRegistry.PTT);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (CONFIG.consumeClick()) {
            minecraft.setScreen(new ConfigScreen());
        }
    }

    @SubscribeEvent
    public static void mute(InputEvent.Key event) {
        var cfg = ChannelClientConfig.get();
        if (event.getKey() == PTT.getKey().getValue() && modifierMatch(event.getModifiers(), PTT.getKeyModifier().codes())) {
            if (event.getAction() == InputConstants.PRESS || event.getAction() == InputConstants.REPEAT) {
                switch (cfg.trigger) {
                    case PUSH_TO_TALK -> ChannelClientConfig.write(c -> c.onAir = true);
                    case SWITCH -> {
                        if (Util.getMillis() - lastSwitch >= 100) {
                            ChannelClientConfig.write(c -> c.onAir = !c.onAir);
                            lastSwitch = Util.getMillis();
                        }
                    }
                    default -> {
                    }
                }
            } else {
                if (cfg.trigger == Trigger.PUSH_TO_TALK) {
                    ChannelClientConfig.write(c -> c.onAir = false);
                }
            }
        }
    }

    public static boolean modifierMatch(int combined, InputConstants.Key[] modifiers) {
        for (var m : modifiers) {
            if (combined == m.getValue()) {
                return true;
            }
        }
        return combined == 0;
    }
}
