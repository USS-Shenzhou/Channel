package cn.ussshenzhou.channel.gui;

import cn.ussshenzhou.channel.audio.OpusManager;
import cn.ussshenzhou.channel.config.ChannelClientConfig;
import cn.ussshenzhou.channel.subspace.client.SubspaceConnection;
import cn.ussshenzhou.channel.util.ModConstant;
import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.gui.advanced.TOptionsPanel;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import cn.ussshenzhou.t88.util.T88Config;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author USS_Shenzhou
 */
public class TransmitConfigPanel extends TOptionsPanel {
    private TLabel rawBitrate;
    private final TLabel opusBitrate;
    private final TLabel speed;

    private final HorizontalTitledOption<TLabel> off, address, port, protocol, security;

    private int life = 0;

    @SuppressWarnings("unchecked")
    public TransmitConfigPanel() {
        var cfg = ChannelClientConfig.get();

        addOptionSplitter(Component.translatable("channel.config.net"));
        addOptionCycleButtonInit(
                Component.translatable("channel.config.net.length"),
                Stream.concat(ModConstant.USABLE_FRAME_LENGTH.stream(), Stream.of(cfg.frameLengthMs)).distinct().collect(Collectors.toList()),
                length -> _ -> ChannelClientConfig.write(c -> c.frameLengthMs = length),
                entry -> entry.getContent() == cfg.frameLengthMs
        ).getB().setTooltip(Tooltip.create(Component.translatable("channel.config.net.length.tooltip")));
        var netSampleRate = addOptionCycleButtonInit(
                Component.translatable("channel.config.net.samplerate"),
                ModConstant.USABLE_NETWORK_SAMPLE_RATE,
                f -> _ -> {
                    ChannelClientConfig.write(c -> c.networkSampleRate = f);
                    rawBitrate.setText(Component.literal(getRawBitRate()));
                },
                entry -> entry.getContent() == cfg.networkSampleRate
        ).getB();
        rawBitrate = addOption(Component.translatable("channel.config.net.bitrate"), new TLabel(Component.literal(getRawBitRate()))).getB();
        opusBitrate = addOption(Component.translatable("channel.config.net.opus_bitrate"), new TLabel(Component.literal(getOpusBitRate()))).getB();
        speed = addOption(Component.translatable("channel.config.net.flow"), new TLabel(Component.empty())).getB();
        netSampleRate.setTooltip(Tooltip.create(Component.translatable("channel.config.net.samplerate.tooltip")));

        addOptionSplitter(Component.translatable("channel.config.subspace"));
        off = (HorizontalTitledOption<TLabel>) addOption(Component.empty(), new TLabel(Component.translatable("channel.config.subspace.off"))).getB().getParent();
        address = (HorizontalTitledOption<TLabel>) addOption(Component.translatable("channel.config.subspace.address"), new TLabel(Component.empty())).getB().getParent();
        port = (HorizontalTitledOption<TLabel>) addOption(Component.translatable("channel.config.subspace.port"), new TLabel(Component.empty())).getB().getParent();
        protocol = (HorizontalTitledOption<TLabel>) addOption(Component.translatable("channel.config.subspace.protocol"), new TLabel(Component.empty())).getB().getParent();
        security = (HorizontalTitledOption<TLabel>) addOption(Component.translatable("channel.config.subspace.security"), new TLabel(Component.empty())).getB().getParent();
        updateSubspace();
    }

    private String getRawBitRate() {
        var cfg = ChannelClientConfig.get();
        var df = new DecimalFormat("0.#");
        return df.format(cfg.networkSampleRate * 16 / 1000f);
    }

    private String getOpusBitRate() {
        int rate = OpusManager.getOpusBitRate();
        var df = new DecimalFormat("0.#");
        return df.format(rate / 1000f);
    }

    @Override
    public void tickT() {
        if (life % 10 == 0) {
            OpusManager.SEND_SPEED.update();
            opusBitrate.setText(Component.literal(getOpusBitRate()));
            speed.setText(Component.literal(getReadableSize(OpusManager.SEND_SPEED.averageIn1s())));
            updateSubspace();
        }
        life++;
        super.tickT();
    }

    private void updateSubspace() {
        boolean on = SubspaceConnection.getProtocol() != null;
        off.setVisibleT(!on);
        address.setVisibleT(on);
        port.setVisibleT(on);
        protocol.setVisibleT(on);
        security.setVisibleT(on);
        if (on) {
            if (SubspaceConnection.getChannel() != null && SubspaceConnection.getChannel().remoteAddress() instanceof InetSocketAddress add) {
                address.getController().setText(Component.literal(add.getHostString()));
                port.getController().setText(Component.literal(String.valueOf(add.getPort())));
            }
            protocol.getController().setText(Component.literal(SubspaceConnection.getProtocol().name()));
            security.getController().setText(Component.literal(SubspaceConnection.getSecurityLevel().name()));
        }
    }


    private String getReadableSize(double bytes) {
        var s = new StringBuilder();
        if (ConfigHelper.getConfigRead(T88Config.class).networkUnit == T88Config.NetworkUnit.BIT) {
            bytes *= 8;
            if (bytes < 1000) {
                s.append(bytes).append(" §7bps§r");
            } else if (bytes < 1000 * 1000) {
                s.append(String.format("%.1f §7Kbps§r", bytes / 1024));
            } else {
                s.append(String.format("%.2f §7Mbps§r", bytes / (1024 * 1024)));
            }
        } else {
            if (bytes < 1000) {
                s.append(bytes).append(" §7Bytes/S§r");
            } else if (bytes < 1000 * 1000) {
                s.append(String.format("%.1f §7KiB/S§r", bytes / 1024));
            } else {
                s.append(String.format("%.2f §7MiB/S§r", bytes / (1024 * 1024)));
            }
        }
        return s.toString();
    }
}
