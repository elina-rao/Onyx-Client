package com.onyxclient.modules.rendering;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

/**
 * Sets TCP_NODELAY on the client Netty channel. No packet content changes.
 */
public class TCPNoDelayModule extends Module {

    private boolean applied;

    public TCPNoDelayModule() {
        super("TCPNoDelay", "Disable Nagle on client socket", ModuleCategory.RENDERING, true);
    }

    @Override
    public void onEnable() {
        applied = false;
        apply();
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!applied) {
            apply();
        }
    }

    private void apply() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getNetHandler() == null) {
                return;
            }
            NetworkManager nm = mc.getNetHandler().getNetworkManager();
            Channel channel = getChannel(nm);
            if (channel != null && channel.isOpen()) {
                channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                channel.config().setOption(ChannelOption.SO_RCVBUF, 65535);
                channel.config().setOption(ChannelOption.SO_SNDBUF, 65535);
                applied = true;
            }
        } catch (Throwable t) {
            // Reflective access may fail on some environments
        }
    }

    private Channel getChannel(NetworkManager nm) throws Exception {
        for (Field field : NetworkManager.class.getDeclaredFields()) {
            if (Channel.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return (Channel) field.get(nm);
            }
        }
        return null;
    }
}
