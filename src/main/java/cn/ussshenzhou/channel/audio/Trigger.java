package cn.ussshenzhou.channel.audio;

import cn.ussshenzhou.channel.input.ModKeyMappingRegistry;
import cn.ussshenzhou.t88.gui.util.ITranslatable;
import net.minecraft.locale.Language;

/**
 * @author USS_Shenzhou
 */
public enum Trigger implements ITranslatable {
    ALWAYS("channel.config.pre.trigger.always"),
    VAD("channel.config.pre.trigger.vad"),
    PUSH_TO_TALK("channel.config.pre.trigger.ptt"),
    THRESHOLD("channel.config.pre.trigger.threshold"),;
    //TODO switch

    private final String translateKey;

    Trigger(String translateKey) {
        this.translateKey = translateKey;
    }

    @Override
    public String translateKey() {
        if (this == PUSH_TO_TALK) {
            return Language.getInstance().getOrDefault(translateKey).replace("%1$s", ModKeyMappingRegistry.PTT.getKey().getDisplayName().getString());
        }
        return translateKey;
    }

    public String directTranslateKey() {
        return translateKey;
    }
}
