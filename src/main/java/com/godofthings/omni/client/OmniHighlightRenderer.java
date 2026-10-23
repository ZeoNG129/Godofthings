package com.godofthings.omni.client;

import com.godofthings.omni.GodofthingsOmni;
import com.godofthings.omni.NeighborOffsets;
import com.godofthings.omni.OmniBlacklist;
import com.godofthings.omni.OmniCompatManager;
import com.godofthings.omni.OmniConfig;
import com.godofthings.omni.UpgradeCompat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

import org.joml.Matrix4f;

/**
 * 批量操作高亮预览（原样移植自 Omni Upgrades 的 {@code BatchInstallHighlightRenderer}）：
 * 手持受支持的插件 / 安装器时，把即将被批量操作影响的机器围一圈半透明线框，
 * 结果按 250ms 缓存（视线 / 潜行状态变化或超时才重算 BFS）。
 */
@EventBusSubscriber(modid = GodofthingsOmni.MODID, value = Dist.CLIENT)
public class OmniHighlightRenderer {

    private static BlockPos cachedSeedPos = null;
    private static final List<RenderBox> cachedBoxes = new ArrayList<>();
    private static long lastCacheTime = 0L;
    private static boolean wasShiftDown = false;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) {
            return;
        }
        if (!OmniConfig.isEnabled) {
            clearCache();
            return;
        }
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        UpgradeCompat compat = OmniCompatManager.getCompatFor(heldItem);
        if (compat == null) {
            heldItem = player.getItemInHand(InteractionHand.OFF_HAND);
            compat = OmniCompatManager.getCompatFor(heldItem);
            if (compat == null) {
                clearCache();
                return;
            }
        }
        if (OmniBlacklist.isBlacklisted(true, player, heldItem)) {
            clearCache();
            return;
        }
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != Type.BLOCK) {
            clearCache();
            return;
        }
        BlockPos hitPos = ((BlockHitResult) hit).getBlockPos();
        hitPos = compat.resolveMainPos(level, hitPos);
        boolean isShiftDown = player.isShiftKeyDown();
        long currentTime = Util.getMillis();
        if (cachedSeedPos == null || !cachedSeedPos.equals(hitPos) || isShiftDown != wasShiftDown
                || currentTime - lastCacheTime > 250L) {
            cachedSeedPos = hitPos;
            wasShiftDown = isShiftDown;
            lastCacheTime = currentTime;
            Vec3 clickVec = isShiftDown ? null : ((BlockHitResult) hit).getLocation();
            updateCache(level, hitPos, heldItem, isShiftDown, compat, clickVec);
        }

        if (cachedBoxes.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pose = poseStack.last().pose();
        int[] rgb = compat.getColor(heldItem);
        int r = rgb[0];
        int g = rgb[1];
        int b = rgb[2];
        int a = 200;
        float inflate = 0.04F;
        float thick = 0.05F;
        for (RenderBox box : cachedBoxes) {
            if (box.centerPos.closerToCenterThan(camPos, 64.0)) {
                drawBoxOutline(buffer, pose, box.x, box.y, box.z, box.w, box.h, box.d, inflate, thick, r, g, b, a);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void clearCache() {
        cachedBoxes.clear();
        cachedSeedPos = null;
    }

    private static void updateCache(Level level, BlockPos startPos, ItemStack testItem, boolean isBatch,
            UpgradeCompat compat, Vec3 clickPos) {
        cachedBoxes.clear();
        Set<BlockPos> validBases = new LinkedHashSet<>();
        if (!isBatch) {
            if (compat.canMachineAcceptUpgrade(level, startPos, testItem, clickPos)) {
                validBases.add(startPos);
            }
        } else {
            Queue<BlockPos> queue = new ArrayDeque<>(128);
            Set<BlockPos> visitedParts = new HashSet<>();
            queue.add(startPos);
            visitedParts.add(startPos);
            List<BlockPos> offsets = NeighborOffsets.get(OmniConfig.isDiagonalDetectionEnabled);
            while (!queue.isEmpty() && validBases.size() < 1000) {
                BlockPos currentPart = queue.poll();
                BlockPos currentBase = compat.resolveMainPos(level, currentPart);
                if (compat.isMachine(level, currentBase)) {
                    if (compat.canMachineAcceptUpgrade(level, currentBase, testItem)) {
                        validBases.add(currentBase);
                    }
                    for (BlockPos offset : offsets) {
                        BlockPos neighbor = currentPart.offset(offset.getX(), offset.getY(), offset.getZ());
                        if (startPos.distManhattan(neighbor) <= 16 && visitedParts.add(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        for (BlockPos basePos : validBases) {
            cachedBoxes.add(new RenderBox(basePos, 1.0F, 1.0F, 1.0F));
        }
    }

    private static void drawBoxOutline(BufferBuilder buffer, Matrix4f pose, float x, float y, float z,
            float w, float h, float d, float inf, float t, int r, int g, int b, int a) {
        float minX = x - inf;
        float minY = y - inf;
        float minZ = z - inf;
        float maxX = x + w + inf;
        float maxY = y + h + inf;
        float maxZ = z + d + inf;
        drawEdge(buffer, pose, minX, minY, minZ, maxX, minY + t, minZ + t, r, g, b, a);
        drawEdge(buffer, pose, minX, minY, maxZ - t, maxX, minY + t, maxZ, r, g, b, a);
        drawEdge(buffer, pose, minX, minY, minZ, minX + t, minY + t, maxZ, r, g, b, a);
        drawEdge(buffer, pose, maxX - t, minY, minZ, maxX, minY + t, maxZ, r, g, b, a);
        drawEdge(buffer, pose, minX, maxY - t, minZ, maxX, maxY, minZ + t, r, g, b, a);
        drawEdge(buffer, pose, minX, maxY - t, maxZ - t, maxX, maxY, maxZ, r, g, b, a);
        drawEdge(buffer, pose, minX, maxY - t, minZ, minX + t, maxY, maxZ, r, g, b, a);
        drawEdge(buffer, pose, maxX - t, maxY - t, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawEdge(buffer, pose, minX, minY, minZ, minX + t, maxY, minZ + t, r, g, b, a);
        drawEdge(buffer, pose, maxX - t, minY, minZ, maxX, maxY, minZ + t, r, g, b, a);
        drawEdge(buffer, pose, minX, minY, maxZ - t, minX + t, maxY, maxZ, r, g, b, a);
        drawEdge(buffer, pose, maxX - t, minY, maxZ - t, maxX, maxY, maxZ, r, g, b, a);
    }

    private static void drawEdge(BufferBuilder buffer, Matrix4f pose, float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ, int r, int g, int b, int a) {
        vertex(buffer, pose, minX, minY, minZ, r, g, b, a);
        vertex(buffer, pose, maxX, minY, minZ, r, g, b, a);
        vertex(buffer, pose, maxX, minY, maxZ, r, g, b, a);
        vertex(buffer, pose, minX, minY, maxZ, r, g, b, a);
        vertex(buffer, pose, minX, maxY, maxZ, r, g, b, a);
        vertex(buffer, pose, maxX, maxY, maxZ, r, g, b, a);
        vertex(buffer, pose, maxX, maxY, minZ, r, g, b, a);
        vertex(buffer, pose, minX, maxY, minZ, r, g, b, a);
        vertex(buffer, pose, minX, maxY, minZ, r, g, b, a);
        vertex(buffer, pose, maxX, maxY, minZ, r, g, b, a);
        vertex(buffer, pose, maxX, minY, minZ, r, g, b, a);
        vertex(buffer, pose, minX, minY, minZ, r, g, b, a);
        vertex(buffer, pose, minX, minY, maxZ, r, g, b, a);
        vertex(buffer, pose, maxX, minY, maxZ, r, g, b, a);
        vertex(buffer, pose, maxX, maxY, maxZ, r, g, b, a);
        vertex(buffer, pose, minX, maxY, maxZ, r, g, b, a);
        vertex(buffer, pose, minX, minY, maxZ, r, g, b, a);
        vertex(buffer, pose, minX, maxY, maxZ, r, g, b, a);
        vertex(buffer, pose, minX, maxY, minZ, r, g, b, a);
        vertex(buffer, pose, minX, minY, minZ, r, g, b, a);
        vertex(buffer, pose, maxX, minY, minZ, r, g, b, a);
        vertex(buffer, pose, maxX, maxY, minZ, r, g, b, a);
        vertex(buffer, pose, maxX, maxY, maxZ, r, g, b, a);
        vertex(buffer, pose, maxX, minY, maxZ, r, g, b, a);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f pose, float x, float y, float z,
            int r, int g, int b, int a) {
        buffer.addVertex(pose, x, y, z).setColor(r, g, b, a);
    }

    private static class RenderBox {
        final float x;
        final float y;
        final float z;
        final float w;
        final float h;
        final float d;
        final BlockPos centerPos;

        RenderBox(BlockPos basePos, float w, float h, float d) {
            this.x = basePos.getX();
            this.y = basePos.getY();
            this.z = basePos.getZ();
            this.w = w;
            this.h = h;
            this.d = d;
            this.centerPos = basePos.above((int) (h / 2.0F));
        }
    }
}
