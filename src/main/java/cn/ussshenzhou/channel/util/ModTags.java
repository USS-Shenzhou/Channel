package cn.ussshenzhou.channel.util;

import cn.ussshenzhou.channel.Channel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static final TagKey<Block> AUDIO_SOURCE = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Channel.MODID, "audio_source")
    );
}
