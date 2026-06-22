package cn.ussshenzhou.channel.Item;

import cn.ussshenzhou.channel.Channel;
import cn.ussshenzhou.channel.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber
public class ModItems {
    //----------Data Component----------
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Channel.MODID);

    public static final Supplier<DataComponentType<Integer>> CHANNEL = DATA_COMPONENTS.registerComponentType("channel", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    //----------Item----------
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Channel.MODID);

    public static final Supplier<Item> HANDHELD_MIC_ITEM = ITEMS.register("handheld_mic", () -> new HandheldMic(new Item.Properties()
            .stacksTo(1)
            .component(CHANNEL.get(), 0)
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Channel.MODID, "handheld_mic")))
    ));
    public static final Supplier<Item> MIC_ITEM = ITEMS.register("mic", () -> new BlockItem(ModBlocks.MIC_BLOCK.get(), new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Channel.MODID, "mic")))
    ));
    public static final Supplier<Item> SPEAKER_ITEM = ITEMS.register("speaker", () -> new BlockItem(ModBlocks.SPEAKER_BLOCK.get(), new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Channel.MODID, "speaker")))
    ));
    public static final Supplier<Item> SPEAKER_STAND_ITEM = ITEMS.register("speaker_stand", () -> new BlockItem(ModBlocks.SPEAKER_STAND_BLOCK.get(), new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Channel.MODID, "speaker_stand")))
    ));
    public static final Supplier<Item> SPEAKER_HANG_ITEM = ITEMS.register("speaker_hang", () -> new BlockItem(ModBlocks.SPEAKER_HANG_BLOCK.get(), new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Channel.MODID, "speaker_hang")))
    ));

    //----------Creative Mode Tab----------
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Channel.MODID);

    public static final Supplier<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(MIC_ITEM.get()))
            .title(Component.translatable("channel.tab.title"))
            .build());

    @SubscribeEvent
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab());
        if (tab != null && tab.equals(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(TAB.get()))) {
            event.acceptAll(Stream.of(
                    HANDHELD_MIC_ITEM,
                    MIC_ITEM,
                    SPEAKER_ITEM,
                    SPEAKER_STAND_ITEM,
                    SPEAKER_HANG_ITEM
            ).map(s -> new ItemStack(s.get())).toList());
        }
    }
}
