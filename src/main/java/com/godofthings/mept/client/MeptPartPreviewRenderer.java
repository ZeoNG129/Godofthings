package com.godofthings.mept.client;

import com.godofthings.mept.MeptItems;
import static net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE;
import com.godofthings.mept.MeptItems;
import static net.minecraft.client.renderer.RenderStateShard.ITEM_ENTITY_TARGET;
import com.godofthings.mept.MeptItems;
import static net.minecraft.client.renderer.RenderStateShard.NO_CULL;
import com.godofthings.mept.MeptItems;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_LINES_SHADER;
import com.godofthings.mept.MeptItems;
import static net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TRANSPARENCY;
import com.godofthings.mept.MeptItems;
import static net.minecraft.client.renderer.RenderStateShard.VIEW_OFFSET_Z_LAYERING;

import com.godofthings.mept.MeptItems;
import java.util.ArrayList;
import com.godofthings.mept.MeptItems;
import java.util.List;
import com.godofthings.mept.MeptItems;
import java.util.OptionalDouble;

import com.godofthings.mept.MeptItems;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.godofthings.mept.MeptItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.godofthings.mept.MeptItems;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.godofthings.mept.MeptItems;
import org.lwjgl.opengl.GL11;

import com.godofthings.mept.MeptItems;
import net.minecraft.client.Camera;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.Minecraft;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.multiplayer.ClientLevel;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.renderer.LevelRenderer;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.renderer.MultiBufferSource;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.renderer.RenderStateShard;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.renderer.RenderType;
import com.godofthings.mept.MeptItems;
import net.minecraft.core.BlockPos;
import com.godofthings.mept.MeptItems;
import net.minecraft.core.Direction;
import com.godofthings.mept.MeptItems;
import net.minecraft.nbt.CompoundTag;
import com.godofthings.mept.MeptItems;
import net.minecraft.network.chat.Component;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.entity.player.Player;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.item.ItemStack;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.phys.AABB;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.phys.BlockHitResult;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.phys.HitResult;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.phys.shapes.Shapes;
import com.godofthings.mept.MeptItems;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import com.godofthings.mept.MeptItems;
import net.neoforged.neoforge.common.NeoForge;
import com.godofthings.mept.MeptItems;
import net.neoforged.bus.api.EventPriority;
import com.godofthings.mept.MeptItems;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.godofthings.mept.MeptItems;
import appeng.api.parts.IPart;
import com.godofthings.mept.MeptItems;
import appeng.api.parts.IPartItem;
import com.godofthings.mept.MeptItems;
import appeng.parts.BusCollisionHelper;
import com.godofthings.mept.MeptItems;
import appeng.parts.PartPlacement;

import com.godofthings.mept.MeptItems;
import com.godofthings.mept.WandMenu;
import com.godofthings.mept.MeptItems;
import com.godofthings.mept.MeptDataComponents;

/**
 * Renders placement preview for ME Placement Tool when holding cables, panels, quartz fiber, etc.
 * Completely follows AE2's RenderBlockOutlineHook implementation.
 */
public class MeptPartPreviewRenderer {
    private MeptPartPreviewRenderer() {
    }

    /**
     * Similar to {@link RenderType#LINES}, but with inverted depth test.
     * Copied from AE2's RenderBlockOutlineHook.
     */
    public static final RenderType LINES_BEHIND_BLOCK = RenderType.create(
            "godofthings_mept_lines_behind_block",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard(">", GL11.GL_GREATER))
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    // Preview box color - same as MeptMultiblockPreviewRenderer (cyan/blue)
    private static final float PREVIEW_RED = 0.0f;
    private static final float PREVIEW_GREEN = 0.75f;
    private static final float PREVIEW_BLUE = 1.0f;

    public static void install() {
        // Use HIGH priority to run before AE2's handler (which uses default priority)
        // AE2's handler checks if the item is IPartItem, but our tool is not IPartItem
        // So we need higher priority to render preview for our tool
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, MeptPartPreviewRenderer::handleEvent);
    }

    private static void handleEvent(RenderHighlightEvent.Block evt) {
        var level = Minecraft.getInstance().level;
        var poseStack = evt.getPoseStack();
        var buffers = evt.getMultiBufferSource();
        var camera = evt.getCamera();
        if (level == null || buffers == null) {
            return;
        }

        var blockHitResult = evt.getTarget();
        if (blockHitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        if (replaceBlockOutline(level, poseStack, buffers, camera, blockHitResult)) {
            evt.setCanceled(true);
        }
    }

    /**
     * Renders placement preview for ME Placement Tool.
     * Only renders for IPartItem (cables, panels, quartz fiber, etc.)
     */
    private static boolean replaceBlockOutline(ClientLevel level,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            BlockHitResult hitResult) {

        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        // Check if player is holding ME Placement Tool
        ItemStack wand = player.getMainHandItem();
        if (wand.isEmpty() || wand.getItem() != MeptItems.ME_PLACEMENT_TOOL.get()) {
            return false;
        }

        // Get the configured item from the wand
        ItemStack targetItem = getConfiguredItem(wand);
        if (targetItem == null || targetItem.isEmpty()) {
            return false;
        }

        // Only render preview for IPartItem (cables, panels, quartz fiber, etc.) or IFacadeItem
        if (!(targetItem.getItem() instanceof IPartItem<?>) && !(targetItem.getItem() instanceof appeng.api.implementations.items.IFacadeItem)) {
            return false;
        }

        // Render without depth test to also have a preview for parts inside blocks.
        if (targetItem.getItem() instanceof IPartItem<?>) {
            showPartPlacementPreview(player, poseStack, buffers, camera, hitResult, targetItem, true);
            showPartPlacementPreview(player, poseStack, buffers, camera, hitResult, targetItem, false);
        } else if (targetItem.getItem() instanceof appeng.api.implementations.items.IFacadeItem) {
            showFacadePlacementPreview(player, poseStack, buffers, camera, hitResult, targetItem);
        }

        return true;
    }

    // Cache the parsed configured item keyed by the config component reference:
    // deserializing an 18-slot ItemStackHandler from NBT every frame is wasteful.
    private static CompoundTag lastConfigTag;
    private static ItemStack cachedConfiguredItem = ItemStack.EMPTY;

    /**
     * Get the currently configured item from the wand's Data Component
     */
    private static ItemStack getConfiguredItem(ItemStack wand) {
        // Use DataComponent instead of NBT
        CompoundTag cfg = wand.get(MeptDataComponents.PLACEMENT_CONFIG.get());

        if (cfg == null) {
            lastConfigTag = null;
            cachedConfiguredItem = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }

        // Data components are immutable in practice; reference equality is a cheap cache key
        if (cfg == lastConfigTag) {
            return cachedConfiguredItem;
        }
        
        // Get selected slot index (default 0)
        int selected = 0;
        if (cfg.contains("SelectedSlot")) {
            selected = cfg.getInt("SelectedSlot");
            if (selected < 0 || selected >= 18) selected = 0;
        }

        // Build handler from NBT
        var handler = new ItemStackHandler(18);
        var registryAccess = Minecraft.getInstance().level.registryAccess();
        if (cfg.contains("items")) {
            handler.deserializeNBT(registryAccess, cfg.getCompound("items"));
        } else {
            handler.deserializeNBT(registryAccess, cfg);
        }

        ItemStack target = handler.getStackInSlot(selected);
        
        // Unwrap AE wrapped stacks
        if (target != null && !target.isEmpty()) {
            try {
                var unwrapped = appeng.api.stacks.GenericStack.unwrapItemStack(target);
                if (unwrapped != null && unwrapped.what() instanceof appeng.api.stacks.AEItemKey itemKey) {
                    target = itemKey.toStack();
                }
            } catch (Throwable ignored) {}
        }
        
        lastConfigTag = cfg;
        cachedConfiguredItem = target;
        return target;
    }

    /**
     * Show placement preview for AE2 parts (cables, panels, etc.)
     * Follows AE2's showPartPlacementPreview implementation exactly.
     */
    private static void showPartPlacementPreview(
            Player player,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            BlockHitResult blockHitResult,
            ItemStack itemInHand,
            boolean insideBlock) {
        if (itemInHand.getItem() instanceof IPartItem<?> partItem) {
            var placement = PartPlacement.getPartPlacement(player,
                    player.level(),
                    itemInHand,
                    blockHitResult.getBlockPos(),
                    blockHitResult.getDirection(),
                    blockHitResult.getLocation());

            if (placement != null) {
                var part = partItem.createPart();
                renderPart(poseStack, buffers, camera, placement.pos(), part, placement.side(), true, insideBlock);
            }
        }
    }

    /**
     * Show placement preview for AE2 facades
     */
    private static void showFacadePlacementPreview(
            Player player,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            BlockHitResult blockHitResult,
            ItemStack itemInHand) {
        if (itemInHand.getItem() instanceof appeng.api.implementations.items.IFacadeItem facadeItem) {
            var facade = facadeItem.createPartFromItemStack(itemInHand, blockHitResult.getDirection());
            if (facade != null) {
                renderFacade(poseStack, buffers, camera, blockHitResult.getBlockPos(), facade);
            }
        }
    }

    /**
     * Render the facade preview boxes.
     */
    private static void renderFacade(PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            BlockPos pos,
            appeng.api.parts.IFacadePart facade) {
        var boxes = new ArrayList<AABB>();
        var helper = new BusCollisionHelper(boxes, facade.getSide(), true);
        facade.getBoxes(helper, false);
        renderBoxes(poseStack, buffers, camera, pos, boxes, true, false);
    }

    /**
     * Render the part preview boxes.
     * Follows AE2's renderPart implementation exactly.
     */
    private static void renderPart(PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            BlockPos pos,
            IPart part,
            Direction side,
            boolean preview,
            boolean insideBlock) {
        var boxes = new ArrayList<AABB>();
        var helper = new BusCollisionHelper(boxes, side, true);
        part.getBoxes(helper);
        renderBoxes(poseStack, buffers, camera, pos, boxes, preview, insideBlock);
    }

    /**
     * Render the preview boxes.
     * Follows AE2's renderBoxes implementation exactly, but with custom color (same as MeptMultiblockPreviewRenderer).
     */
    private static void renderBoxes(PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            BlockPos pos,
            List<AABB> boxes,
            boolean preview,
            boolean insideBlock) {
        RenderType renderType = insideBlock ? LINES_BEHIND_BLOCK : RenderType.lines();
        var buffer = buffers.getBuffer(renderType);
        float alpha = insideBlock ? 0.2f : preview ? 0.6f : 0.4f;

        for (var box : boxes) {
            var shape = Shapes.create(box);
            renderShape(
                    poseStack,
                    buffer,
                    shape,
                    pos.getX() - camera.getPosition().x,
                    pos.getY() - camera.getPosition().y,
                    pos.getZ() - camera.getPosition().z,
                    PREVIEW_RED,
                    PREVIEW_GREEN,
                    PREVIEW_BLUE,
                    alpha);
        }
    }

    // Copied from LevelRenderer.renderShape since it is private
    private static void renderShape(
            PoseStack poseStack,
            com.mojang.blaze3d.vertex.VertexConsumer consumer,
            net.minecraft.world.phys.shapes.VoxelShape shape,
            double x,
            double y,
            double z,
            float red,
            float green,
            float blue,
            float alpha) {
        PoseStack.Pose last = poseStack.last();
        shape.forAllEdges(
                (k, l, m, n, o, p) -> {
                    float q = (float)(n - k);
                    float r = (float)(o - l);
                    float s = (float)(p - m);
                    float t = net.minecraft.util.Mth.sqrt(q * q + r * r + s * s);
                    q /= t;
                    r /= t;
                    s /= t;
                    consumer.addVertex(last, (float)(k + x), (float)(l + y), (float)(m + z))
                            .setColor(red, green, blue, alpha)
                            .setNormal(last, q, r, s);
                    consumer.addVertex(last, (float)(n + x), (float)(o + y), (float)(p + z))
                            .setColor(red, green, blue, alpha)
                            .setNormal(last, q, r, s);
                });
    }
}
