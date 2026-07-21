package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.modules.settings.ColorSetting;
import com.onyxclient.modules.settings.NumberSetting;
import com.onyxclient.utils.BedwarsMapHeights;
import com.onyxclient.utils.Colors;
import com.onyxclient.utils.GameContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Unified Bedwars hub — height overlay, colored beds (via mixin), and toggles that gate related HUDs.
 */
public class HypixelBedwarsModule extends Module {

    public static HypixelBedwarsModule INSTANCE;

    public final BooleanSetting statsHud = addSetting(new BooleanSetting("Stats HUD", true));
    public final BooleanSetting resourceCounter = addSetting(new BooleanSetting("Resource Counter", true));
    public final BooleanSetting heightOverlay = addSetting(new BooleanSetting("Height Limit Overlay", true));
    public final BooleanSetting heightHud = addSetting(new BooleanSetting("Height HUD Text", true));
    public final BooleanSetting coloredBeds = addSetting(new BooleanSetting("Colored Beds", true));
    public final BooleanSetting hardcoreHearts = addSetting(new BooleanSetting("Hardcore Hearts", false));
    public final NumberSetting buildHeight = addSetting(new NumberSetting("Build Height", 112.0, 64.0, 256.0, 1.0));
    public final BooleanSetting mapAwareHeight = addSetting(new BooleanSetting("Map-Aware Height", true));
    public final NumberSetting planeSize = addSetting(new NumberSetting("Plane Size", 24.0, 8.0, 48.0, 1.0));
    public final ColorSetting planeColor = addSetting(new ColorSetting("Plane Color", 0xFFFF5533));
    public final NumberSetting nearWarnBlocks = addSetting(new NumberSetting("Near Warn Blocks", 2.0, 1.0, 8.0, 1.0));

    private boolean bedBrokenHint;

    public HypixelBedwarsModule() {
        super("HypixelBedwars", "Unified Bedwars helper hub", ModuleCategory.BEDWARS, true);
        INSTANCE = this;
    }

    /** Effective build limit — map table when enabled, else manual setting. */
    public int effectiveBuildHeight() {
        int fallback = buildHeight.getIntValue();
        if (!mapAwareHeight.getValue()) {
            return fallback;
        }
        return BedwarsMapHeights.resolve(Minecraft.getMinecraft(), fallback);
    }

    public boolean isInBedwars() {
        GameContext.Mode mode = GameContext.detect();
        if (mode == GameContext.Mode.BEDWARS || mode == GameContext.Mode.RANKED) {
            return true;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            return false;
        }
        if (mc.getCurrentServerData() != null) {
            String ip = mc.getCurrentServerData().serverIP == null ? "" : mc.getCurrentServerData().serverIP.toLowerCase();
            if (ip.contains("hypixel") || ip.contains("rbw") || ip.contains("onyx")) {
                ScoreObjective objective = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
                if (objective != null) {
                    String title = objective.getDisplayName();
                    if (title != null && title.toLowerCase().contains("bed war")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean showStatsHud() {
        return isEnabled() && statsHud.getValue();
    }

    public boolean showResourceCounter() {
        return isEnabled() && resourceCounter.getValue();
    }

    public boolean useHardcoreHearts() {
        return isEnabled() && hardcoreHearts.getValue() && isInBedwars() && bedBrokenHint;
    }

    public void setBedBrokenHint(boolean broken) {
        this.bedBrokenHint = broken;
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (!isEnabled() || !heightOverlay.getValue() || !isInBedwars()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getRenderManager() == null) {
            return;
        }
        double px = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double pz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;

        int max = effectiveBuildHeight();
        boolean near = Math.floor(mc.thePlayer.posY) >= max - nearWarnBlocks.getIntValue();
        long pulse = System.currentTimeMillis() / 200L;
        boolean flashOn = !near || (pulse % 2L) == 0L;

        double cx = px - mc.getRenderManager().viewerPosX;
        double cy = max - mc.getRenderManager().viewerPosY;
        double cz = pz - mc.getRenderManager().viewerPosZ;
        double half = planeSize.getFloatValue() / 2.0;

        int argb = planeColor.getValue();
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        if (near) {
            r = 1.0F;
            g = 0.25F;
            b = 0.2F;
        }
        float fillA = near ? 0.22F : 0.12F;
        float borderA = flashOn ? 0.95F : 0.35F;
        float gridA = flashOn ? 0.55F : 0.2F;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(near ? 3.0F : 2.0F);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx - half, cy, cz - half).color(r, g, b, fillA).endVertex();
        wr.pos(cx + half, cy, cz - half).color(r, g, b, fillA).endVertex();
        wr.pos(cx + half, cy, cz + half).color(r, g, b, fillA).endVertex();
        wr.pos(cx - half, cy, cz + half).color(r, g, b, fillA).endVertex();
        tess.draw();

        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx - half, cy, cz - half).color(r, g, b, borderA).endVertex();
        wr.pos(cx + half, cy, cz - half).color(r, g, b, borderA).endVertex();
        wr.pos(cx + half, cy, cz + half).color(r, g, b, borderA).endVertex();
        wr.pos(cx - half, cy, cz + half).color(r, g, b, borderA).endVertex();
        tess.draw();

        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx - half, cy, cz).color(r, g, b, gridA).endVertex();
        wr.pos(cx + half, cy, cz).color(r, g, b, gridA).endVertex();
        wr.pos(cx, cy, cz - half).color(r, g, b, gridA).endVertex();
        wr.pos(cx, cy, cz + half).color(r, g, b, gridA).endVertex();
        tess.draw();

        GL11.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        if (!isEnabled() || !heightHud.getValue() || !isInBedwars()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        int y = (int) Math.floor(mc.thePlayer.posY);
        int max = effectiveBuildHeight();
        String map = BedwarsMapHeights.detectMapName(mc);
        boolean near = y >= max - nearWarnBlocks.getIntValue();
        String text = map.isEmpty()
                ? ("Height  " + y + " / " + max)
                : ("Height  " + y + " / " + max + "  ·  " + capitalize(map));
        if (near) {
            text = text + "  NEAR LIMIT";
        }
        int color = near ? Colors.DANGER : Colors.ACCENT_BRIGHT;
        ScaledResolution sr = new ScaledResolution(mc);
        mc.fontRendererObj.drawStringWithShadow(text,
                sr.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(text) / 2,
                6, color);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
