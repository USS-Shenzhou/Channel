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
    //TODO ALWAYS_MUTE/temp mute

    private final String translateKey;

    Trigger(String translateKey) {
        this.translateKey = translateKey;
    }

    @Override
    public String translateKey() {
        return translateKey;
    }
}
