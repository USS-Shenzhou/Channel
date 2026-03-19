package cn.ussshenzhou.channel.input;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.gui.ConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Channel.MODID, "key.categories.channel"));
    public static final KeyMapping CONFIG = new KeyMapping(
            "key.channel.config_screen", KeyConflictContext.UNIVERSAL, KeyModifier.ALT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C,CATEGORY
            );
    public static final KeyMapping PTT = new KeyMapping(
            "key.channel.ptt", KeyConflictContext.UNIVERSAL, KeyModifier.NONE,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY
    );

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
        } else if (PTT.consumeClick()) {
            //TODO
        }
    }
}
