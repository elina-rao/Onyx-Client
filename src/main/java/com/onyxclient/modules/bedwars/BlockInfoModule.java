package com.onyxclient.modules.bedwars;

import com.onyxclient.modules.hud.HudModule;
import com.onyxclient.modules.settings.BooleanSetting;
import com.onyxclient.utils.Colors;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

public class BlockInfoModule extends HudModule {

    private final BooleanSetting showCoords;
    private final BooleanSetting showBreakTime;
    private final BooleanSetting showLightLevel;
    private final BooleanSetting showBestTool;

    public BlockInfoModule() {
        super("BlockInfo", "Look-at block info (name/tool/light)", false);
        showCoords = addSetting(new BooleanSetting("Block Coords", true));
        showBreakTime = addSetting(new BooleanSetting("Break Time", true));
        showLightLevel = addSetting(new BooleanSetting("Light Level", true));
        showBestTool = addSetting(new BooleanSetting("Correct Tool", true));
        setHudSize(130, 42);
        setHudPosition(2, 220);
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mc.theWorld == null || mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        BlockPos pos = mop.getBlockPos();
        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();
        String[] lines = new String[5];
        int idx = 0;
        lines[idx++] = "Block: " + block.getLocalizedName();
        if (showCoords.getValue()) {
            lines[idx++] = "XYZ: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
        if (showBestTool.getValue()) {
            ItemStack held = mc.thePlayer != null ? mc.thePlayer.getHeldItem() : null;
            boolean good = held != null && held.getStrVsBlock(block) > 1.0F;
            lines[idx++] = "Best Tool: " + (good ? "Yes" : "No");
        }
        if (showBreakTime.getValue()) {
            float hardness = block.getBlockHardness(mc.theWorld, pos);
            lines[idx++] = "Hardness: " + (hardness < 0 ? "Unbreakable" : String.format(java.util.Locale.US, "%.2f", hardness));
        }
        if (showLightLevel.getValue()) {
            lines[idx++] = "Light: " + mc.theWorld.getLight(pos);
        }

        int y = hudY;
        int maxW = 60;
        for (int i = 0; i < idx; i++) {
            mc.fontRendererObj.drawStringWithShadow(lines[i], hudX, y, Colors.TEXT_PRIMARY);
            maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(lines[i]) + 4);
            y += 10;
        }
        setHudSize(maxW, Math.max(12, y - hudY));
    }
}
