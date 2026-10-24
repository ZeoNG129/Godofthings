package com.godofthings.overloadedinterface.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import appeng.client.render.overlay.OverlayRenderType;

import com.godofthings.Godofthings;
import com.godofthings.item.GodBinderItem;
import com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity;

/**
 * 过载接口的世界内连接渲染（照抄供应器的 {@code InfConnectorRenderer} /
 * 上游 {@code WirelessConnectorRenderer}，只支持过载接口宿主）：
 *
 * <ul>
 *   <li>手持同心契时，范围内的过载接口（无线模式）显示半透明内芯：
 *       未选中 = 蓝，已选中 = 黄；</li>
 *   <li>已连接的机器面显示蓝色平面 + 宿主到该面的连线；</li>
 *   <li>准星指向机器时显示 Ctrl 批量预览（黄色面 + 黄线，已连接的不画）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class OverloadedInterfaceRenderer
{
    // 已连接面（蓝）/ 连线（蓝）/ 内芯蓝 / 内芯黄（选中）/ 预览黄 / 预览线黄（同供应器色板）
    private static final int COLOR_CONNECTED = 0x600080FF;
    private static final int COLOR_HOST = 0x800080FF;
    private static final int COLOR_HOST_SELECTED = 0x80FFFF00;
    private static final int COLOR_LINE = 0xC00080FF;
    private static final int COLOR_PREVIEW = 0x60FFFF00;
    private static final int COLOR_PREVIEW_LINE = 0xC0FFFF00;

    private static final int SCAN_RANGE = 64;
    private static final int RESCAN_INTERVAL_TICKS = 4;

    private static final List<BlockPos> cachedHostPositions = new ArrayList<>();
    private static long lastScanTick = -1L;
    private static ResourceKey<Level> lastScanDimension = null;

    private static final Quaternionf scratchRotation = new Quaternionf();
    private static final Set<BlockPos> scratchConnectionSet = new HashSet<>();

    /** 照抄供应器 Ae2ltRenderTypes：继承 RenderType 才能访问受保护的 RenderStateShard 常量。 */
    private static final class SeeThroughRenderType extends RenderType
    {
        private static RenderType instance;

        private SeeThroughRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                Runnable setup, Runnable clear)
        {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
        }

        /** GREATER 深度测试的透视 QUADS：内芯隔着不透明方块也可见。 */
        static RenderType get()
        {
            if (instance == null)
            {
                instance = create("godofthings_iface_see_through",
                        DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS,
                        65536, false, false,
                        CompositeState.builder()
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setTextureState(NO_TEXTURE)
                                .setLightmapState(NO_LIGHTMAP)
                                .setDepthTestState(GREATER_DEPTH_TEST)
                                .setWriteMaskState(COLOR_WRITE)
                                .setCullState(NO_CULL)
                                .setShaderState(POSITION_COLOR_SHADER)
                                .createCompositeState(false));
            }
            return instance;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
        {
            return;
        }

        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null)
        {
            return;
        }

        // 手持同心契才渲染
        var binderOpt = net.minecraft.world.item.ItemStack.EMPTY;
        for (var hand : net.minecraft.world.InteractionHand.values())
        {
            var held = player.getItemInHand(hand);
            if (held.getItem() instanceof GodBinderItem)
            {
                binderOpt = held;
                break;
            }
        }
        // 只有当前选中的是过载接口（或什么都没选中）才画接口视图，
        // 选中的是供应器时由 InfConnectorRenderer 画，两边互不干扰
        var ifaceSelection = GodBinderItem.getOverloadedInterfaceSelection(binderOpt);
        if (binderOpt.isEmpty())
        {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        var buffer = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        scratchRotation.set(mc.gameRenderer.getMainCamera().rotation());
        scratchRotation.invert();
        poseStack.mulPose(scratchRotation);
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        var selectedPos = ifaceSelection != null ? ifaceSelection.pos() : null;
        boolean selectionInCurrentDimension = ifaceSelection != null
                && mc.level.dimension().equals(ifaceSelection.dimension());

        long gameTime = mc.level.getGameTime();
        var currentDim = mc.level.dimension();
        if (lastScanTick < 0L || lastScanDimension == null
                || !lastScanDimension.equals(currentDim)
                || gameTime - lastScanTick >= RESCAN_INTERVAL_TICKS)
        {
            rescanHosts(mc.level, player.blockPosition());
            lastScanTick = gameTime;
            lastScanDimension = currentDim;
        }

        boolean selectedRendered = false;
        for (BlockPos bePos : cachedHostPositions)
        {
            if (!mc.level.isLoaded(bePos))
            {
                continue;
            }
            if (!(mc.level.getBlockEntity(bePos) instanceof OverloadedInterfaceBlockEntity iface))
            {
                continue;
            }
            if (iface.getInterfaceMode()
                    != OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS)
            {
                continue;
            }
            boolean isSelected = selectionInCurrentDimension && bePos.equals(selectedPos);
            if (selectionInCurrentDimension && !isSelected)
            {
                continue;
            }
            renderInterfaceHost(poseStack, buffer, mc.level, bePos, iface, isSelected);
            selectedRendered |= isSelected;
        }
        if (selectionInCurrentDimension && !selectedRendered
                && mc.level.isLoaded(selectedPos)
                && mc.level.getBlockEntity(selectedPos)
                        instanceof OverloadedInterfaceBlockEntity iface
                && iface.getInterfaceMode()
                        == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS)
        {
            renderInterfaceHost(poseStack, buffer, mc.level, selectedPos, iface, true);
        }

        // Ctrl 批量预览：准星指向机器时，黄色标出将要连接的面并画预览线（已连接的不画）
        if (selectionInCurrentDimension
                && mc.hitResult instanceof BlockHitResult bhr
                && bhr.getType() == HitResult.Type.BLOCK
                && !bhr.getBlockPos().equals(selectedPos)
                && mc.level.getBlockEntity(bhr.getBlockPos()) != null)
        {
            var previewTargets = collectTargets(mc.level, bhr.getBlockPos(),
                    net.minecraft.client.gui.screens.Screen.hasControlDown());
            Direction lookFace = bhr.getDirection();
            scratchConnectionSet.clear();
            if (mc.level.getBlockEntity(selectedPos) instanceof OverloadedInterfaceBlockEntity iface)
            {
                for (var conn : iface.getConnections())
                {
                    if (conn.dimension().equals(mc.level.dimension()) && conn.boundFace() == lookFace)
                    {
                        scratchConnectionSet.add(conn.pos());
                    }
                }
            }
            for (var lookPos : previewTargets)
            {
                if (!scratchConnectionSet.contains(lookPos))
                {
                    renderFaceOverlay(poseStack, buffer, lookPos, lookFace, COLOR_PREVIEW);
                    renderLine(poseStack, buffer, selectedPos, lookPos, lookFace, COLOR_PREVIEW_LINE);
                }
            }
        }

        poseStack.popPose();

        buffer.endBatch(SeeThroughRenderType.get());
        buffer.endBatch(OverlayRenderType.getBlockHilightFace());
        buffer.endBatch(OverlayRenderType.getBlockHilightLine());
    }

    // ---- 渲染原语（照抄供应器渲染器）----

    /** 接口内芯 + 全部已连接面与连线。 */
    private static void renderInterfaceHost(PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            Level level, BlockPos hostPos, OverloadedInterfaceBlockEntity iface, boolean selected)
    {
        renderInnerCube(poseStack, buffer, hostPos, selected ? COLOR_HOST_SELECTED : COLOR_HOST);

        for (var conn : iface.getConnections())
        {
            if (!conn.dimension().equals(level.dimension()))
            {
                continue;
            }
            renderFaceOverlay(poseStack, buffer, conn.pos(), conn.boundFace(), COLOR_CONNECTED);
            renderLine(poseStack, buffer, hostPos, conn.pos(), conn.boundFace(), COLOR_LINE);
        }
    }

    /** 方块内 0.25~0.75 的透视内芯立方体。 */
    private static void renderInnerCube(PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer, BlockPos pos, int color)
    {
        VertexConsumer vc = buffer.getBuffer(SeeThroughRenderType.get());
        int[] c = OverlayRenderType.decomposeColor(color);

        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
        Matrix4f mat = poseStack.last().pose();

        float lo = 0.25f, hi = 0.75f;

        quad(vc, mat, c, lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi, 0, -1, 0);
        quad(vc, mat, c, lo, hi, hi, hi, hi, hi, hi, hi, lo, lo, hi, lo, 0, 1, 0);
        quad(vc, mat, c, lo, lo, lo, lo, hi, lo, hi, hi, lo, hi, lo, lo, 0, 0, -1);
        quad(vc, mat, c, hi, lo, hi, hi, hi, hi, lo, hi, hi, lo, lo, hi, 0, 0, 1);
        quad(vc, mat, c, lo, lo, hi, lo, hi, hi, lo, hi, lo, lo, lo, lo, -1, 0, 0);
        quad(vc, mat, c, hi, lo, lo, hi, hi, lo, hi, hi, hi, hi, lo, hi, 1, 0, 0);

        poseStack.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f mat, int[] c,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float nx, float ny, float nz)
    {
        vc.addVertex(mat, x1, y1, z1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
        vc.addVertex(mat, x4, y4, z4).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
    }

    /** 在指定方块面上画一层半透明高亮（照抄供应器六向 switch）。 */
    private static void renderFaceOverlay(PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            BlockPos pos, Direction face, int color)
    {
        VertexConsumer vc = buffer.getBuffer(OverlayRenderType.getBlockHilightFace());
        int[] c = OverlayRenderType.decomposeColor(color);

        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
        Matrix4f mat = poseStack.last().pose();

        float offset = 0.001f;
        float nx = face.getStepX();
        float ny = face.getStepY();
        float nz = face.getStepZ();

        switch (face)
        {
            case DOWN ->
            {
                vc.addVertex(mat, 0, -offset, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, -offset, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, -offset, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, -offset, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case UP ->
            {
                vc.addVertex(mat, 0, 1 + offset, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 1 + offset, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 1 + offset, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, 1 + offset, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case NORTH ->
            {
                vc.addVertex(mat, 0, 0, -offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, 1, -offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 1, -offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 0, -offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case SOUTH ->
            {
                vc.addVertex(mat, 1, 0, 1 + offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1, 1, 1 + offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, 1, 1 + offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 0, 0, 1 + offset).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case WEST ->
            {
                vc.addVertex(mat, -offset, 0, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, -offset, 1, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, -offset, 1, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, -offset, 0, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
            case EAST ->
            {
                vc.addVertex(mat, 1 + offset, 0, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1 + offset, 1, 0).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1 + offset, 1, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
                vc.addVertex(mat, 1 + offset, 0, 1).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
            }
        }

        poseStack.popPose();
    }

    /** 接口中心 → 目标面中心的连线（照抄供应器）。 */
    private static void renderLine(PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            BlockPos from, BlockPos to, Direction face, int color)
    {
        VertexConsumer vc = buffer.getBuffer(OverlayRenderType.getBlockHilightLine());
        int[] c = OverlayRenderType.decomposeColor(color);

        Matrix4f mat = poseStack.last().pose();

        float fx = from.getX() + 0.5f;
        float fy = from.getY() + 0.5f;
        float fz = from.getZ() + 0.5f;

        float tx = to.getX() + 0.5f + face.getStepX() * 0.501f;
        float ty = to.getY() + 0.5f + face.getStepY() * 0.501f;
        float tz = to.getZ() + 0.5f + face.getStepZ() * 0.501f;

        float dx = tx - fx, dy = ty - fy, dz = tz - fz;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f)
        {
            return;
        }
        float nx = dx / len, ny = dy / len, nz = dz / len;

        vc.addVertex(mat, fx, fy, fz).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
        vc.addVertex(mat, tx, ty, tz).setColor(c[1], c[2], c[3], c[0]).setNormal(nx, ny, nz);
    }

    // ---- 扫描（照抄供应器）----

    /** 与服务端 GodBinderItem.collectTargets 相同的 Ctrl 批量收集逻辑（客户端版）。 */
    private static Set<BlockPos> collectTargets(Level level, BlockPos origin, boolean contiguous)
    {
        if (!level.isLoaded(origin) || level.getBlockEntity(origin) == null)
        {
            return Set.of();
        }
        if (!contiguous)
        {
            return Set.of(origin.immutable());
        }
        var originState = level.getBlockState(origin);
        var visited = new LinkedHashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(origin.immutable());
        while (!queue.isEmpty())
        {
            var current = queue.removeFirst();
            if (!visited.add(current))
            {
                continue;
            }
            for (var direction : Direction.values())
            {
                var next = current.relative(direction);
                if (visited.contains(next) || !level.isLoaded(next))
                {
                    continue;
                }
                if (level.getBlockEntity(next) == null)
                {
                    continue;
                }
                if (!level.getBlockState(next).is(originState.getBlock()))
                {
                    continue;
                }
                queue.addLast(next.immutable());
            }
        }
        return visited;
    }

    private static void rescanHosts(net.minecraft.client.multiplayer.ClientLevel level, BlockPos playerPos)
    {
        cachedHostPositions.clear();

        int minCX = (playerPos.getX() - SCAN_RANGE) >> 4;
        int maxCX = (playerPos.getX() + SCAN_RANGE) >> 4;
        int minCZ = (playerPos.getZ() - SCAN_RANGE) >> 4;
        int maxCZ = (playerPos.getZ() + SCAN_RANGE) >> 4;

        for (int cx = minCX; cx <= maxCX; cx++)
        {
            for (int cz = minCZ; cz <= maxCZ; cz++)
            {
                if (!level.hasChunk(cx, cz))
                {
                    continue;
                }
                var chunk = level.getChunk(cx, cz);
                for (var bePos : chunk.getBlockEntitiesPos())
                {
                    if (chunk.getBlockEntity(bePos) instanceof OverloadedInterfaceBlockEntity)
                    {
                        cachedHostPositions.add(bePos.immutable());
                    }
                }
            }
        }
    }

    /** 换世界 / 断线时清缓存（照抄供应器）。 */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        cachedHostPositions.clear();
        scratchConnectionSet.clear();
        lastScanTick = -1L;
        lastScanDimension = null;
    }
}
