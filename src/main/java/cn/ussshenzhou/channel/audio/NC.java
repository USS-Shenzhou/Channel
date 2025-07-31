package cn.ussshenzhou.channel.audio;

import cn.ussshenzhou.t88.gui.util.ITranslatable;

/**
 * @author USS_Shenzhou
 */
public enum NC implements ITranslatable {
    LOW("channel.config.pre.0"),
    MID("channel.config.pre.1"),
    HIGH("channel.config.pre.2"),
    AGGR("channel.config.pre.3"),
    AI("channel.config.ai"),
    OFF("channel.config.off");

    private final String translateKey;

    NC(String translateKey) {
        this.translateKey = translateKey;
    }

    @Override
    public String translateKey() {
        return translateKey;
    }
}
