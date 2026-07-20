package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Deque;

public class CPSCounterModule extends HudModule {

    private final Deque<Long> leftClicks = new ArrayDeque<Long>();
    private final Deque<Long> rightClicks = new ArrayDeque<Long>();
    private boolean wasLeft;
    private boolean wasRight;

    public CPSCounterModule() {
        super("CPS Counter", "Mouse clicks per second", true);
        setHudSize(80, 20);
        hudY = 60;
    }

    @Override
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean left = Mouse.isButtonDown(0);
        boolean right = Mouse.isButtonDown(1);
        long now = System.currentTimeMillis();

        if (left && !wasLeft) {
            leftClicks.addLast(now);
        }
        if (right && !wasRight) {
            rightClicks.addLast(now);
        }
        wasLeft = left;
        wasRight = right;

        prune(leftClicks, now);
        prune(rightClicks, now);
    }

    private void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.pollFirst();
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String text = "CPS: " + leftClicks.size() + " | " + rightClicks.size();
        mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.TEXT_PRIMARY);
        setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
    }
}
