package com.onyxclient.modules.hud;

import com.onyxclient.utils.Colors;
import com.onyxclient.utils.HudLayoutTokens;
import com.onyxclient.utils.HudTheme;
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
        setUseScaledBounds(true);
        setHudSize(80, 20);
        hudY = 60;
        enablePremiumDefaults();
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
        String text = "CPS  " + leftClicks.size() + " | " + rightClicks.size();
        if (!usePremiumRenderer()) {
            mc.fontRendererObj.drawStringWithShadow(text, hudX, hudY, Colors.TEXT_PRIMARY);
            setHudSize(mc.fontRendererObj.getStringWidth(text) + 4, 12);
            return;
        }
        beginHudScale();
        int lineH = hudLineHeight(mc);
        int contentW = measureHudText(mc, text);
        int cardW = Math.max(HudLayoutTokens.CARD_MIN_WIDTH, contentW + HudLayoutTokens.CARD_PADDING_X * 2);
        int cardH = lineH + HudLayoutTokens.CARD_PADDING_Y * 2;
        if (usePremiumCard()) {
            drawHudCard(hudX, hudY, cardW, cardH);
        }
        drawHudAccentText(mc, text, hudX + HudLayoutTokens.CARD_PADDING_X,
                hudY + HudLayoutTokens.CARD_PADDING_Y, HudTheme.VALUE_ACCENT);
        setHudSize(cardW, cardH);
        endHudScale();
    }
}
