package cn.ussshenzhou.channel.audio;

import cn.ussshenzhou.channel.util.AudioHelper;
import cn.ussshenzhou.t88.gui.util.ITranslatable;

/**
 * @author USS_Shenzhou
 */

public enum Unit implements ITranslatable {
    DB("Channel.config.unit.db"),
    PERCENT("Channel.config.unit.per");

    private final String translateKey;

    Unit(String translateKey) {
        this.translateKey = translateKey;
    }

    public String get(double db) {
        return switch (this) {
            case DB -> String.format("%.1f", db) + " dB";
            case PERCENT -> String.format("%.2f", AudioHelper.db2factor((float) db) * 100) + "%";
        };
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public String getFS(double value) {
        return switch (this) {
            case DB -> value == 0 ? "-∞ dBFS" : String.format("%.1f dBFS", value);
            case PERCENT -> String.format("%.2f", AudioHelper.db2factor((float) value) * 100) + "%";
        };
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    public String getFS90(double value) {
        return switch (this) {
            case DB -> value == 0 ? "-∞ dBFS" : String.format("%.1f dBFS", value - 90);
            case PERCENT -> String.format("%.2f", AudioHelper.db2factor((float) value - 90) * 100) + "%";
        };
    }

    @Override
    public String translateKey() {
        return translateKey;
    }
}
