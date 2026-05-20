package com.jr.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;

public class ElytraMinimap {

    private static final int MAP_SIZE = 100;
    private static final int RADIUS = 50;
    private static final int ROWS_PER_FRAME = 10;
    private static final long CACHE_TTL_MS = 5000;  
    private static final int MAX_CACHE_CHUNKS = 1024;
    private static NativeImage mapImage = null;
    private static DynamicTexture mapTexture = null;
    private static Identifier textureLocation = null;
    private static int currentRow = 0;
    private static int snapX, snapZ;
    private static final int[] prevRowHeights = new int[MAP_SIZE];
    private static final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
    private static final HashMap<Long, ChunkData> chunkCache = new HashMap<>();

    private static final class ChunkData {
        final int[] rawColors = new int[256]; // [lz*16 + lx]
        final int[] heights   = new int[256];
        long updatedAt = 0;
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("elytraavionics", "minimap"),
                ElytraMinimap::render
        );
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Level world = player.level();
        int playerX = (int) player.getX();
        int playerZ = (int) player.getZ();

        if (mapTexture == null) {
            mapImage = new NativeImage(MAP_SIZE, MAP_SIZE, false);
            mapTexture = new DynamicTexture(() -> "elytraavionics:minimap", mapImage);
            textureLocation = Identifier.fromNamespaceAndPath("elytraavionics", "minimap");
            Minecraft.getInstance().getTextureManager().register(textureLocation, mapTexture);
            snapX = playerX;
            snapZ = playerZ;
            java.util.Arrays.fill(prevRowHeights, 64);
        }

        long now = System.currentTimeMillis();

        for (int i = 0; i < ROWS_PER_FRAME; i++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int worldX = snapX - RADIUS + x;
                int worldZ = snapZ - RADIUS + currentRow;

                ChunkData chunk = getOrRefreshChunk(world, worldX >> 4, worldZ >> 4, now);
                int idx = (worldZ & 15) * 16 + (worldX & 15);

                int blockY = chunk.heights[idx];
                int color  = applyHeightShading(chunk.rawColors[idx], blockY, prevRowHeights[x]);
                prevRowHeights[x] = blockY;

                mapImage.setPixel(x, currentRow, color);
            }
            currentRow++;
            if (currentRow >= MAP_SIZE) {
                currentRow = 0;
                snapX = playerX;
                snapZ = playerZ;
                java.util.Arrays.fill(prevRowHeights, 64);
            }
        }

        mapTexture.upload();

        graphics.blit(RenderPipelines.GUI_TEXTURED, textureLocation, 0, 0, 0.0f, 0.0f, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        renderPlayerIcon(graphics, player);
    }

    private static void renderPlayerIcon(GuiGraphics graphics, LocalPlayer player) {
        boolean hasElytra = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        float angle = (float) Math.toRadians(player.getYRot() + 180f);

        graphics.pose().pushMatrix();
        graphics.pose().translate(MAP_SIZE / 2.0f, MAP_SIZE / 2.0f);
        graphics.pose().rotate(angle);

        if (hasElytra) {
            drawAirplaneIcon(graphics);
        } else {
            drawArrowIcon(graphics);
        }

        graphics.pose().popMatrix();
    }

    private static void drawAirplaneIcon(GuiGraphics g) {
        int w = 0xFFFFFFFF;
        int d = 0xFF222222;

        g.fill(-1, -6, 2, 6, d);
        g.fill( 0, -5, 1, 5, w);
        g.fill(-6, -1, 7, 1, d);
        g.fill(-5,  0, 6, 0, w);
        g.fill(-5, -1, 6, 2, w);
        g.fill(-4, 3, 0, 6, d);
        g.fill(-3, 4, 0, 5, w);
        g.fill( 1, 3, 5, 6, d);
        g.fill( 1, 4, 4, 5, w);
    }

    private static void drawArrowIcon(GuiGraphics g) {
        int w = 0xFFFFFFFF;
        int d = 0xFF222222;

        g.fill(-1, -6, 2, 5, d);
        g.fill(-4, -3, 5, 1, d);
        g.fill(-3, -2, 4, -1, w);
        g.fill(-2, -3, 3, -2, w);
        g.fill(-1, -4, 2, -3, w);
        g.fill( 0, -5, 1, -4, w);
        g.fill( 0,  0, 1, 4, w);
    }

    private static ChunkData getOrRefreshChunk(Level world, int chunkX, int chunkZ, long now) {
        long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        ChunkData data = chunkCache.get(key);

        if (data != null && (now - data.updatedAt) <= CACHE_TTL_MS) {
            return data;
        }

        if (chunkCache.size() >= MAX_CACHE_CHUNKS) {
            chunkCache.clear();
        }
        if (data == null) {
            data = new ChunkData();
            chunkCache.put(key, data);
        }

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int minY  = world.getMinY();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int wx = baseX + lx;
                int wz = baseZ + lz;
                int y  = getTopBlockY(world, wx, wz, minY);
                data.heights[lz * 16 + lx]   = y;
                data.rawColors[lz * 16 + lx] = getBlockColor(world, wx, y, wz);
            }
        }
        data.updatedAt = now;
        return data;
    }

    private static int getTopBlockY(Level world, int x, int z, int minY) {
        scratch.set(x, minY, z);
        if (!world.isLoaded(scratch)) return minY;
        int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        return Math.max(y, minY);
    }

    private static int getBlockColor(Level world, int x, int y, int z) {
        scratch.set(x, y, z);
        BlockState state = world.getBlockState(scratch);

        if (state.getFluidState().is(FluidTags.WATER)) {
            int floorY    = world.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
            int depth     = Math.min(20, Math.max(0, y - floorY));
            float dark    = Math.max(0.35f, 1.0f - depth * 0.04f);
            return 0xFF000000 | ((int)(64 * dark) << 16) | ((int)(120 * dark) << 8) | (int)(200 * dark);
        }

        int rgb = state.getMapColor(world, scratch).col;
        if (rgb == 0) return 0xFF101010;
        return 0xFF000000 | rgb;
    }

    private static int applyHeightShading(int color, int currentY, int prevY) {
        if (currentY == prevY) return color;
        float factor = currentY > prevY ? 1.28f : 0.72f;
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int)((color & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
