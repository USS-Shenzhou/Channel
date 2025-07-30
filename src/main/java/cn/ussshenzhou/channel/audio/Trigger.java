package cn.ussshenzhou.channel.audio;

import cn.ussshenzhou.channel.input.ModKeyMappingRegistry;
import cn.ussshenzhou.t88.gui.util.ITranslatable;
import net.minecraft.locale.Language;

public enum Trigger implements ITranslatable {
    ALWAYS("channel.config.pre.trigger.always"),
    VAD("channel.config.pre.trigger.vad"),
    PTT("channel.config.pre.trigger.ptt");

    private final String translateKey;

    Trigger(String translateKey) {
        this.translateKey = translateKey;
    }

    @Override
    public String translateKey() {
        if (this == PTT) {
            return Language.getInstance().getOrDefault(translateKey).replace("?", ModKeyMappingRegistry.PTT.getKey().getDisplayName().getString());
        }
        return translateKey;
    }
}
