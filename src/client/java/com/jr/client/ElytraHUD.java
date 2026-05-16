package com.jr.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public class ElytraHUD {

    public static int groundHeight = 0;
    public static double verticalSpeed = 0;

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("elytraavionics", "hud"),
                ElytraHUD::render
        );
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        graphics.drawString(Minecraft.getInstance().font, "V/S: " + ElytraHUD.verticalSpeed, screenWidth - 60, screenHeight - 10, 0xFF00FF00);
        graphics.drawString(Minecraft.getInstance().font, "GRND: " + ElytraHUD.groundHeight, screenWidth - 60, screenHeight - 20, 0xFF00FF00);
    }
}
