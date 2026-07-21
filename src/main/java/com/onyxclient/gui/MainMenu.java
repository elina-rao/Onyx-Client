package com.onyxclient.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.onyxclient.core.RankedStatsClient;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.OnyxFont;
import com.onyxclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Ranked-home title screen — soft atmosphere, custom typography, connect CTAs.
 */
public class MainMenu extends GuiScreen {

    /** Matches OnyxLauncher default serverIp (launcherConfig.js). */
    private static final String RANKED_SERVER_IP = "eu.onyxrbw.com";
    private static final String HYPIXEL_SERVER_IP = "mc.hypixel.net";
    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/steve.png");
    private static final int BUTTON_COUNT = 6;

    private final List<Particle> particles = new ArrayList<Particle>();
    private final Random random = new Random();
    private int tickCounter;
    private float openProgress;
    private ResourceLocation playerSkin;
    private boolean skinRequested;
    private int brandDrawY;

    @Override
    public void initGui() {
        buttonList.clear();
        playerSkin = null;
        skinRequested = false;

        int btnW = Math.min(220, width - 40);
        int footerReserve = 32;
        int topPad = Math.max(18, (int) (height * 0.10F));
        int brandGap = 10;
        int brandBlock = OnyxFont.TITLE.getHeight() + OnyxFont.SMALL.getHeight() + brandGap;

        // Fit all 6 buttons between brand and footer — never clip Quit.
        int available = height - topPad - brandBlock - footerReserve - 8;
        int btnH = 28;
        int spacing = 8;
        int stackH = BUTTON_COUNT * btnH + (BUTTON_COUNT - 1) * spacing;
        if (stackH > available && available > 0) {
            spacing = Math.max(4, (available - BUTTON_COUNT * btnH) / Math.max(1, BUTTON_COUNT - 1));
            stackH = BUTTON_COUNT * btnH + (BUTTON_COUNT - 1) * spacing;
            if (stackH > available) {
                btnH = Math.max(18, (available - (BUTTON_COUNT - 1) * spacing) / BUTTON_COUNT);
                stackH = BUTTON_COUNT * btnH + (BUTTON_COUNT - 1) * spacing;
            }
        }
        // Last resort: shrink brand top pad so Quit stays on-screen
        while (topPad + brandBlock + 8 + stackH + footerReserve > height && topPad > 8) {
            topPad -= 2;
        }
        while (topPad + brandBlock + 8 + stackH + footerReserve > height && brandGap > 4) {
            brandGap -= 1;
            brandBlock = OnyxFont.TITLE.getHeight() + OnyxFont.SMALL.getHeight() + brandGap;
        }

        int brandBottom = topPad + brandBlock;
        int startY = brandBottom + Math.max(6, (height - brandBottom - footerReserve - stackH) / 2);
        int maxStart = height - footerReserve - stackH - 2;
        if (startY > maxStart) {
            startY = Math.max(brandBottom + 4, maxStart);
        }
        // Absolute clamp: Quit bottom edge must stay above footer
        int quitBottom = startY + stackH;
        if (quitBottom > height - footerReserve) {
            startY = Math.max(4, height - footerReserve - stackH);
        }

        int centerX = width / 2;
        String[] labels = {
                "Join Hypixel",
                "Join Ranked",
                "Multiplayer",
                "Mod Menu",
                "Options",
                "Quit"
        };
        for (int i = 0; i < BUTTON_COUNT; i++) {
            buttonList.add(new OnyxButton(
                    i,
                    centerX - btnW / 2,
                    startY + i * (btnH + spacing),
                    btnW,
                    btnH,
                    labels[i]
            ));
        }

        // Stash brand Y for drawBrand (was height*0.16)
        this.brandDrawY = topPad;

        particles.clear();
        for (int i = 0; i < 48; i++) {
            particles.add(new Particle(random.nextInt(Math.max(1, width)), random.nextInt(Math.max(1, height))));
        }
        openProgress = 0.0F;
        requestPlayerSkin();
    }

    private void requestPlayerSkin() {
        if (skinRequested || mc == null || mc.getSession() == null) {
            return;
        }
        skinRequested = true;
        try {
            GameProfile profile = mc.getSession().getProfile();
            if (profile == null) {
                return;
            }
            UUID id = profile.getId();
            if (id != null) {
                playerSkin = DefaultPlayerSkin.getDefaultSkin(id);
            }
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures =
                    mc.getSkinManager().loadSkinFromCache(profile);
            if (textures != null && textures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                playerSkin = mc.getSkinManager().loadSkin(
                        textures.get(MinecraftProfileTexture.Type.SKIN),
                        MinecraftProfileTexture.Type.SKIN);
            } else {
                mc.getSkinManager().loadProfileTextures(profile, new SkinCallback(), true);
            }
        } catch (Exception ignored) {
            /* keep default / steve */
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Mod Menu uses nested GL scissors; clear any leftover clip before painting title UI.
        RenderUtils.clearScissor();

        tickCounter++;
        if (openProgress < 1.0F) {
            openProgress = Math.min(1.0F, openProgress + 0.045F);
        }
        float ease = openProgress * openProgress * (3.0F - 2.0F * openProgress);

        float pulse = (float) Math.sin(tickCounter * 0.015) * 0.5F + 0.5F;
        int top = Colors.lerpColor(0xFF0B0B10, 0xFF160A28, 0.35F + pulse * 0.25F);
        drawGradientRect(0, 0, width, height, top, Colors.BG_DEEP);
        RenderUtils.drawGradientRect(0, 0, width, height / 2,
                Colors.withAlpha(Colors.ACCENT_PRIMARY, 18 + (int) (10 * pulse)),
                Colors.withAlpha(0x000000, 0));

        drawParticles(partialTicks);
        RenderUtils.drawVignette(width, height, 0.55F);

        drawBrand(ease);
        drawButtons(mouseX, mouseY, ease);
        drawFooter();
    }

    private void drawBrand(float ease) {
        int brandAlpha = (int) (255 * ease);
        String onyx = "ONYX";
        String client = "CLIENT";
        int gap = 10;
        int onyxW = OnyxFont.TITLE.getStringWidth(onyx);
        int clientW = OnyxFont.TITLE.getStringWidth(client);
        int totalW = onyxW + gap + clientW;
        float brandX = (width - totalW) / 2.0F;
        float brandY = brandDrawY > 0 ? brandDrawY : height * 0.10F;

        float diamondY = brandY + OnyxFont.TITLE.getHeight() / 2.0F;
        RenderUtils.drawCircleF(brandX - 16, diamondY, 2.5F, Colors.withAlpha(Colors.ACCENT_PRIMARY, brandAlpha));
        RenderUtils.drawCircleF(brandX + totalW + 16, diamondY, 2.5F, Colors.withAlpha(Colors.ACCENT_PRIMARY, brandAlpha));

        OnyxFont.TITLE.drawString(onyx, brandX, brandY, Colors.withAlpha(Colors.TEXT_PRIMARY, brandAlpha));
        OnyxFont.TITLE.drawString(client, brandX + onyxW + gap, brandY, Colors.withAlpha(Colors.ACCENT_BRIGHT, brandAlpha));

        String subtitle = rankedSubtitle();
        OnyxFont.SMALL.drawCenteredString(
                subtitle,
                width / 2.0F,
                brandY + OnyxFont.TITLE.getHeight() + 10,
                Colors.withAlpha(Colors.TEXT_MUTED, (int) (220 * ease))
        );
    }

    private String rankedSubtitle() {
        RankedStatsClient.RankedStats cached = RankedStatsClient.getLastFetched();
        if (cached != null && cached.elo != null && !"—".equals(cached.elo) && !cached.elo.isEmpty()) {
            String line = cached.elo + " ELO";
            if (cached.rank != null && !"—".equals(cached.rank) && !cached.rank.isEmpty()) {
                line = line + "  ·  " + cached.rank;
            }
            return line;
        }
        return "Ranked ready";
    }

    private void drawButtons(int mouseX, int mouseY, float ease) {
        GlStateManager.pushMatrix();
        float offset = (1.0F - ease) * 10.0F;
        GlStateManager.translate(0, offset, 0);
        for (int i = 0; i < this.buttonList.size(); ++i) {
            ((GuiButton) this.buttonList.get(i)).drawButton(this.mc, mouseX, mouseY);
        }
        GlStateManager.popMatrix();
    }

    private void drawFooter() {
        String prefix = "Onyx Client  |  Made with ";
        OnyxFont.SMALL.drawString(prefix, 12, height - 18, Colors.TEXT_MUTED);
        float heartX = 12 + OnyxFont.SMALL.getStringWidth(prefix) + 1;
        float heartY = height - 18 + (OnyxFont.SMALL.getHeight() - 9) / 2.0F;
        RenderUtils.drawHeart(heartX, heartY, 10, Colors.ACCENT_BRIGHT);

        Minecraft mcInst = Minecraft.getMinecraft();
        if (mcInst.getSession() != null) {
            String user = mcInst.getSession().getUsername();
            int userW = OnyxFont.SMALL.getStringWidth(user);
            int headSize = 14;
            int headX = width - 14 - headSize;
            int headY = height - 20;
            drawPlayerHead(headX, headY, headSize);
            OnyxFont.SMALL.drawString(user, width - 20 - headSize - userW, height - 18, Colors.TEXT_SECONDARY);
        }
    }

    private void drawParticles(float partialTicks) {
        for (Particle p : particles) {
            p.update(width, height, partialTicks);
            float twinkle = 0.55F + 0.45F * (float) Math.sin(tickCounter * 0.04 + p.offset);
            int alpha = (int) (40 + 50 * twinkle);
            RenderUtils.drawCircleF(p.x, p.y, p.size, Colors.withAlpha(Colors.ACCENT_GLOW, alpha));
        }
    }

    private void drawPlayerHead(int x, int y, int size) {
        Minecraft mcInst = Minecraft.getMinecraft();
        ResourceLocation skin = playerSkin;
        if (skin == null) {
            try {
                if (mcInst.getSession() != null && mcInst.getSession().getProfile() != null
                        && mcInst.getSession().getProfile().getId() != null) {
                    skin = DefaultPlayerSkin.getDefaultSkin(mcInst.getSession().getProfile().getId());
                }
            } catch (Exception ignored) {
            }
        }
        if (skin == null) {
            skin = STEVE;
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        RenderUtils.drawRoundedRect(x - 1, y - 1, size + 2, size + 2, 4, Colors.BG_CARD);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mcInst.getTextureManager().bindTexture(skin);
        drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64, 64);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                connectTo(HYPIXEL_SERVER_IP, "Hypixel");
                break;
            case 1:
                connectTo(RANKED_SERVER_IP, "Onyx Ranked");
                break;
            case 2:
                mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case 3:
                mc.displayGuiScreen(new ModMenu());
                break;
            case 4:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;
            case 5:
                mc.shutdown();
                break;
            default:
                break;
        }
    }

    private void connectTo(String ip, String name) {
        ServerData data = new ServerData(name, ip, false);
        mc.displayGuiScreen(new GuiConnecting(this, mc, data));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private class SkinCallback implements net.minecraft.client.resources.SkinManager.SkinAvailableCallback {
        @Override
        public void skinAvailable(MinecraftProfileTexture.Type typeIn, ResourceLocation location,
                                  MinecraftProfileTexture profileTexture) {
            if (typeIn == MinecraftProfileTexture.Type.SKIN && location != null) {
                playerSkin = location;
            }
        }
    }

    private static class Particle {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float size;
        private final float offset;

        private Particle(float x, float y) {
            this.x = x;
            this.y = y;
            this.vx = (float) (Math.random() * 0.25 - 0.12);
            this.vy = (float) (Math.random() * 0.2 - 0.08);
            this.size = 1.2F + (float) Math.random() * 1.8F;
            this.offset = (float) (Math.random() * Math.PI * 2);
        }

        private void update(int width, int height, float partialTicks) {
            x += vx * (0.6F + partialTicks * 0.4F);
            y += vy * (0.6F + partialTicks * 0.4F);
            if (x < -4) x = width + 4;
            if (x > width + 4) x = -4;
            if (y < -4) y = height + 4;
            if (y > height + 4) y = -4;
        }
    }
}
