package com.onyxclient.modules.visual;

import com.onyxclient.core.ServerGate;
import com.onyxclient.modules.Module;
import com.onyxclient.modules.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Perspective (Freelook): hold Left Alt to orbit camera while movement yaw stays locked.
 * Default OFF. Hard-blocked on Hypixel.
 */
public class PerspectiveModule extends Module {

    public static PerspectiveModule INSTANCE;

    private boolean active;
    private float bodyYaw;
    private float bodyPitch;
    private float cameraYaw;
    private float cameraPitch;
    private float savedYaw;
    private float savedPitch;
    private float savedPrevYaw;
    private float savedPrevPitch;
    private boolean anglesApplied;

    public PerspectiveModule() {
        super("Perspective", "Freelook camera orbit (banned on Hypixel — only where allowed)",
                ModuleCategory.VISUAL, false);
        INSTANCE = this;
    }

    public boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || mc.currentScreen != null) {
            active = false;
            return;
        }
        if (!ServerGate.isPerspectiveAllowed()) {
            active = false;
            return;
        }
        boolean hold = Keyboard.isKeyDown(Keyboard.KEY_LMENU);
        if (!hold) {
            active = false;
            return;
        }
        if (!active) {
            active = true;
            bodyYaw = mc.thePlayer.rotationYaw;
            bodyPitch = mc.thePlayer.rotationPitch;
            cameraYaw = bodyYaw;
            cameraPitch = bodyPitch;
        }
        float dx = mc.thePlayer.rotationYaw - bodyYaw;
        float dy = mc.thePlayer.rotationPitch - bodyPitch;
        cameraYaw += dx;
        cameraPitch = MathHelper.clamp_float(cameraPitch + dy, -90.0F, 90.0F);
        mc.thePlayer.rotationYaw = bodyYaw;
        mc.thePlayer.rotationPitch = bodyPitch;
        mc.thePlayer.prevRotationYaw = bodyYaw;
        mc.thePlayer.prevRotationPitch = bodyPitch;
    }

    public void applyCameraToViewEntity() {
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null || !active) {
            anglesApplied = false;
            return;
        }
        savedYaw = view.rotationYaw;
        savedPitch = view.rotationPitch;
        savedPrevYaw = view.prevRotationYaw;
        savedPrevPitch = view.prevRotationPitch;
        view.rotationYaw = cameraYaw;
        view.rotationPitch = cameraPitch;
        view.prevRotationYaw = cameraYaw;
        view.prevRotationPitch = cameraPitch;
        anglesApplied = true;
    }

    public void restoreViewEntity() {
        if (!anglesApplied) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view != null) {
            view.rotationYaw = bodyYaw;
            view.rotationPitch = bodyPitch;
            view.prevRotationYaw = bodyYaw;
            view.prevRotationPitch = bodyPitch;
        }
        anglesApplied = false;
    }
}
