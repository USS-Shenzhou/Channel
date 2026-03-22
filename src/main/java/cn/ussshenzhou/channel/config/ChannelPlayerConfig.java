package cn.ussshenzhou.channel.config;

import cn.ussshenzhou.channel.gui.OutputConfigPanel;
import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.config.TConfig;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("FieldMayBeFinal")
public class ChannelPlayerConfig implements TConfig {

    private HashMap<PlayerId, Float> playerVolumeAdjust = new HashMap<>();

    public static float getOrDefault(UUID uuid) {
        var cfg = get();
        var p = new PlayerId(uuid, "UNKNOWN");
        if (cfg.playerVolumeAdjust.containsKey(p)) {
            return cfg.playerVolumeAdjust.get(p);
        }
        return ChannelClientConfig.get().outputAdjust;
    }

    public static void set(Player player, float db) {
        write(thiz -> thiz.playerVolumeAdjust.put(PlayerId.from(player), db));
    }

    private static ChannelPlayerConfig get() {
        return ConfigHelper.getConfigRead(ChannelPlayerConfig.class);
    }

    public static void clear() {
        write(thiz -> thiz.playerVolumeAdjust.clear());
    }

    private static void write(Consumer<ChannelPlayerConfig> writer) {
        ConfigHelper.getConfigWrite(ChannelPlayerConfig.class, writer);
    }

    @JsonAdapter(PlayerIdAdapter.class)
    public static class PlayerId {
        private UUID uuid;
        private String name;

        public PlayerId(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public static PlayerId from(Player player) {
            return new PlayerId(player.getUUID(), player.getGameProfile().name());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj instanceof PlayerId p && p.uuid.equals(uuid);
        }

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }
    }

    public static class PlayerIdAdapter extends TypeAdapter<PlayerId> {
        @Override
        public void write(JsonWriter out, PlayerId value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.name + "|" + value.uuid.toString());
        }

        @Override
        public PlayerId read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String str = in.nextString();

            int separatorIndex = str.lastIndexOf('|');
            if (separatorIndex != -1) {
                String name = str.substring(0, separatorIndex);
                UUID uuid = UUID.fromString(str.substring(separatorIndex + 1));
                return new PlayerId(uuid, name);
            } else {
                return new PlayerId(UUID.fromString(str), "UNKNOWN");
            }
        }
    }
}
