package com.jr.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ElytraHUD {

    public static int groundHeight = 0;
    public static double verticalSpeed = 0;
    public static double groundSpeed = 0;
    public static boolean pullUpActive = false;

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("elytraavionics", "hud"),
                ElytraHUD::render
        );
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!chest.is(Items.ELYTRA)){
            return;
        }
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        graphics.drawString(Minecraft.getInstance().font, "V/S: " + ElytraHUD.verticalSpeed, screenWidth - 70, screenHeight - 10, 0xFF00FF00);
        graphics.drawString(Minecraft.getInstance().font, "GRND: " + ElytraHUD.groundHeight, screenWidth - 70, screenHeight - 20, 0xFF00FF00);
        graphics.drawString(Minecraft.getInstance().font, "G/S: " + ElytraHUD.groundSpeed  + " m/s", screenWidth - 70, screenHeight - 30, 0xFF00FF00);
        if (ElytraHUD.pullUpActive && (System.currentTimeMillis() % 1000) < 500) {
            String warn = "PULL UP";
            int warnX = screenWidth / 2 - Minecraft.getInstance().font.width(warn) / 2;
            graphics.drawString(Minecraft.getInstance().font, warn, warnX, screenHeight / 2 - 20, 0xFFFF0000);
        }
    }
}
