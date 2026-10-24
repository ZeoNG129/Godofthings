package com.godofthings.mept.client;

import com.godofthings.mept.MeptItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.godofthings.mept.MeptItems;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.Camera;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.Minecraft;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.renderer.LevelRenderer;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.renderer.MultiBufferSource;
import com.godofthings.mept.MeptItems;
import net.minecraft.client.renderer.RenderType;
import com.godofthings.mept.MeptItems;
import net.minecraft.core.BlockPos;
import com.godofthings.mept.MeptItems;
import net.minecraft.core.Direction;
import com.godofthings.mept.MeptItems;
import net.minecraft.nbt.CompoundTag;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.InteractionHand;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.entity.Entity;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.entity.player.Player;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.item.ItemStack;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.item.context.BlockPlaceContext;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.item.context.UseOnContext;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.level.Level;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.phys.AABB;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.phys.BlockHitResult;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.phys.HitResult;
import com.godofthings.mept.MeptItems;
import net.minecraft.world.phys.Vec3;
import com.godofthings.mept.MeptItems;
import net.neoforged.bus.api.SubscribeEvent;
import com.godofthings.mept.MeptItems;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import com.godofthings.mept.MeptItems;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.godofthings.mept.MeptItems;
import com.godofthings.mept.ItemMultiblockPlacementTool;
import com.godofthings.mept.MeptItems;
import com.godofthings.mept.ItemMultiblockPlacementTool.DirectionMode;
import com.godofthings.mept.MeptItems;
import com.godofthings.mept.MeptDataComponents;

import com.godofthings.mept.MeptItems;
import appeng.api.parts.IPart;
import com.godofthings.mept.MeptItems;
import appeng.api.parts.IPartItem;
import com.godofthings.mept.MeptItems;
import appeng.api.parts.PartHelper;
import com.godofthings.mept.MeptItems;
import appeng.api.stacks.GenericStack;
import com.godofthings.mept.MeptItems;
import appeng.parts.BusCollisionHelper;
import com.godofthings.mept.MeptItems;
import appeng.parts.PartPlacement;

import com.godofthings.mept.MeptItems;
import java.util.*;

/**
 * Renders preview of multiblock placement positions.
 * For AE2 parts, previews attach to cable faces (same rules as server placement).
 */
public class MeptMultiblockPreviewRenderer {
    private BlockHitResult lastRayTraceResult;
    private ItemStack lastWand;
    private Set<BlockPos> cachedPositions;
    private BlockPos lastPartPlacementPos;
    private Direction lastPartSide;
    private int lastPlacementCount;
    private DirectionMode lastDirectionMode;
    // Cache the parsed target stack keyed by the config component reference:
    // deserializing an 18-slot ItemStackHandler from NBT every frame is wasteful.
    private CompoundTag lastTargetCfg;
    private ItemStack cachedTargetStack = ItemStack.EMPTY;

    @SubscribeEvent(receiveCanceled = true)
    public void renderBlockHighlight(RenderHighlightEvent.Block event) {
        if (event.getTarget().getType() != HitResult.Type.BLOCK) return;

        BlockHitResult rtr = event.getTarget();
        Entity entity = event.getCamera().getEntity();
        if (!(entity instanceof Player player)) return;

        ItemStack wand = player.getMainHandItem();
        if (wand.isEmpty() || wand.getItem() != MeptItems.MULTIBLOCK_PLACEMENT_TOOL.get()) return;

        int placementCount = ItemMultiblockPlacementTool.getPlacementCount(wand);
        DirectionMode directionMode = ItemMultiblockPlacementTool.getDirectionMode(wand);
        ItemStack target = getSelectedTargetStack(wand);

        boolean targetIsPart = target.getItem() instanceof IPartItem<?>;
        PartPlacement.Placement currentPartPlacement = targetIsPart
                ? getPartPlacementWithCableFallback(player, player.level(), target,
                        rtr.getBlockPos(), rtr.getDirection(), rtr.getLocation())
                : null;
        BlockPos currentPartPlacementPos = currentPartPlacement != null ? currentPartPlacement.pos() : null;
        Direction currentPartSide = currentPartPlacement != null ? currentPartPlacement.side() : null;
        boolean partPlacementChanged = targetIsPart
                && (!Objects.equals(lastPartPlacementPos, currentPartPlacementPos)
                        || lastPartSide != currentPartSide);

        Set<BlockPos> blocks;
        Direction partSide = null;
        if (cachedPositions == null || !compareRTR(lastRayTraceResult, rtr) ||
                !ItemStack.matches(lastWand, wand) || lastPlacementCount != placementCount ||
                lastDirectionMode != directionMode || partPlacementChanged) {
            if (targetIsPart) {
                if (currentPartPlacement == null) {
                    blocks = Collections.emptySet();
                    partSide = null;
                } else {
                    partSide = currentPartSide;
                    blocks = calculatePartPlacementPositions(player, rtr, target, currentPartPlacement,
                            placementCount, directionMode);
                }
            } else {
                blocks = calculatePlacementPositions(player, rtr, wand, placementCount, directionMode);
            }
            cachedPositions = blocks;
            lastRayTraceResult = rtr;
            lastWand = wand.copy();
            lastPlacementCount = placementCount;
            lastDirectionMode = directionMode;
            lastPartPlacementPos = currentPartPlacementPos;
            lastPartSide = partSide;
        } else {
            blocks = cachedPositions;
            partSide = lastPartSide;
        }

        if (blocks == null || blocks.isEmpty()) return;

        PoseStack ms = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        Camera camera = event.getCamera();

        if (target.getItem() instanceof IPartItem<?> partItem && partSide != null) {
            IPart part = partItem.createPart();
            for (BlockPos block : blocks) {
                renderPart(ms, buffer, camera, block, part, partSide, false);
                renderPart(ms, buffer, camera, block, part, partSide, true);
            }
        } else {
            VertexConsumer lineBuilder = buffer.getBuffer(RenderType.LINES);
            double camX = camera.getPosition().x;
            double camY = camera.getPosition().y;
            double camZ = camera.getPosition().z;

            for (BlockPos block : blocks) {
                AABB aabb = new AABB(block).move(-camX, -camY, -camZ);
                LevelRenderer.renderLineBox(ms, lineBuilder, aabb, 0.0F, 0.75F, 1.0F, 0.4F);
            }
        }

        // AE2's own outline hook highlights the exact cable/part hit by the crosshair.
        // Keep the event alive for part hosts so that handler can render the selected
        // sub-part after our bulk-placement preview. For ordinary blocks, continue
        // replacing the vanilla full-block outline as before.
        if (PartHelper.getPartHost(player.level(), rtr.getBlockPos()) == null) {
            event.setCanceled(true);
        }
    }

    private Set<BlockPos> calculatePlacementPositions(Player player, BlockHitResult rtr, ItemStack wand, int placementCount, DirectionMode directionMode) {
        Set<BlockPos> placePositions = new HashSet<>();
        if (placementCount <= 0) return placePositions;

        Level level = player.level();
        BlockPos clickedPos = rtr.getBlockPos();
        Direction clickedFace = rtr.getDirection();
        var clickedState = level.getBlockState(clickedPos);

        LinkedList<BlockPos> candidates = new LinkedList<>();
        Set<BlockPos> allCandidates = new HashSet<>();
        ArrayList<BlockPos> positions = new ArrayList<>();

        // MAX_CANDIDATES limit to prevent infinite loops when placeable positions are fewer than requested
        final int MAX_CANDIDATES = placementCount * 10;

        BlockPos startingPoint = clickedPos.relative(clickedFace);
        candidates.add(startingPoint);

        while (!candidates.isEmpty() && positions.size() < placementCount && allCandidates.size() < MAX_CANDIDATES) {
            BlockPos currentCandidate = candidates.removeFirst();
            if (!allCandidates.add(currentCandidate)) {
                continue;
            }

            // Match ConstructionWand: even when direction is locked, the supporting block
            // (opposite the clicked face) must equal the clicked block.
            BlockPos supportingPoint = currentCandidate.relative(clickedFace.getOpposite());
            var supportingState = level.getBlockState(supportingPoint);

            if (supportingState.getBlock() == clickedState.getBlock()) {
                var currentState = level.getBlockState(currentCandidate);
                boolean canPlace = level.isEmptyBlock(currentCandidate);
                if (!canPlace) {
                    try {
                        var checkContext = new BlockPlaceContext(new UseOnContext(
                                player, InteractionHand.MAIN_HAND, new BlockHitResult(
                                        rtr.getLocation(), rtr.getDirection(), currentCandidate, rtr.isInside()
                                )
                        ));
                        canPlace = currentState.canBeReplaced(checkContext);
                    } catch (Throwable t) {}
                }
                if (canPlace) {
                    positions.add(currentCandidate);
                    // Only expand candidates after successful placement (prevents cross-pit overflow)
                    addAdjacentPositions(candidates, currentCandidate, clickedFace, directionMode);
                }
            }
        }

        placePositions.addAll(positions);
        return placePositions;
    }

    private Set<BlockPos> calculatePartPlacementPositions(Player player, BlockHitResult rtr, ItemStack target,
            PartPlacement.Placement placement, int placementCount, DirectionMode directionMode) {
        Set<BlockPos> placePositions = new HashSet<>();
        if (placementCount <= 0 || placement == null) return placePositions;

        Level level = player.level();
        BlockPos clickedPos = rtr.getBlockPos();
        Direction clickedFace = rtr.getDirection();
        var clickedState = level.getBlockState(clickedPos);
        Direction partSide = placement.side();
        boolean placingOnClickedHost = placement.pos().equals(clickedPos);

        LinkedList<BlockPos> candidates = new LinkedList<>();
        Set<BlockPos> allCandidates = new HashSet<>();
        ArrayList<BlockPos> positions = new ArrayList<>();
        final int MAX_CANDIDATES = placementCount * 10;

        candidates.add(placement.pos());

        while (!candidates.isEmpty() && positions.size() < placementCount && allCandidates.size() < MAX_CANDIDATES) {
            BlockPos currentCandidate = candidates.removeFirst();
            if (!allCandidates.add(currentCandidate)) {
                continue;
            }

            boolean supportMatches;
            if (placingOnClickedHost) {
                supportMatches = level.getBlockState(currentCandidate).getBlock() == clickedState.getBlock();
            } else {
                supportMatches = level.getBlockState(currentCandidate.relative(partSide)).getBlock() == clickedState.getBlock();
            }

            if (supportMatches && canPlaceConfiguredPart(player, level, target, currentCandidate, partSide)) {
                positions.add(currentCandidate);
                addAdjacentPositions(candidates, currentCandidate, clickedFace, directionMode);
            }
        }

        placePositions.addAll(positions);
        return placePositions;
    }

    private boolean canPlaceConfiguredPart(Player player, Level level, ItemStack partStack, BlockPos pos,
            Direction side) {
        return PartPlacement.canPlacePartOnBlock(player, level, partStack, pos, side);
    }

    private PartPlacement.Placement getPartPlacementWithCableFallback(Player player, Level level,
            ItemStack partStack, BlockPos clickedPos, Direction clickedFace, Vec3 clickLocation) {
        var placement = PartPlacement.getPartPlacement(player, level, partStack, clickedPos, clickedFace, clickLocation);
        if (placement != null) {
            return placement;
        }

        var host = PartHelper.getPartHost(level, clickedPos);
        if (host != null && host.getPart(null) != null && host.canAddPart(partStack, clickedFace)) {
            return new PartPlacement.Placement(clickedPos, clickedFace);
        }

        return null;
    }

    private ItemStack getSelectedTargetStack(ItemStack wand) {
        CompoundTag cfg = wand.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg == null) {
            lastTargetCfg = null;
            cachedTargetStack = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }

        // Data components are immutable in practice; reference equality is a cheap cache key
        if (cfg == lastTargetCfg) {
            return cachedTargetStack;
        }

        int selected = 0;
        if (cfg.contains("SelectedSlot")) {
            selected = cfg.getInt("SelectedSlot");
            if (selected < 0 || selected >= 18) selected = 0;
        }

        var handler = new ItemStackHandler(18);
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var registryAccess = level.registryAccess();
            if (cfg.contains("items")) {
                handler.deserializeNBT(registryAccess, cfg.getCompound("items"));
            } else {
                handler.deserializeNBT(registryAccess, cfg);
            }
        }

        ItemStack target = handler.getStackInSlot(selected);
        if (!target.isEmpty()) {
            try {
                var genericStack = GenericStack.unwrapItemStack(target);
                if (genericStack != null && genericStack.what() instanceof appeng.api.stacks.AEItemKey itemKey) {
                    target = itemKey.toStack();
                }
            } catch (Exception ignored) {}
        }
        lastTargetCfg = cfg;
        cachedTargetStack = target;
        return target;
    }

    private void renderPart(PoseStack poseStack, MultiBufferSource buffers, Camera camera, BlockPos pos,
            IPart part, Direction side, boolean insideBlock) {
        var boxes = new ArrayList<AABB>();
        var helper = new BusCollisionHelper(boxes, side, true);
        part.getBoxes(helper);
        var buffer = buffers.getBuffer(insideBlock ? MeptPartPreviewRenderer.LINES_BEHIND_BLOCK : RenderType.lines());
        MeptRainbowRenderHelper.renderRainbowBoxes(poseStack, buffer, pos, boxes,
                camera.getPosition().x, camera.getPosition().y, camera.getPosition().z, insideBlock ? 0.2f : 0.6f);
    }

    private void addAdjacentPositions(LinkedList<BlockPos> candidates, BlockPos pos, Direction face, DirectionMode directionMode) {
        switch (directionMode) {
            case NORTH_SOUTH -> {
                candidates.add(pos.north());
                candidates.add(pos.south());
            }
            case EAST_WEST -> {
                candidates.add(pos.east());
                candidates.add(pos.west());
            }
            case VERTICAL -> {
                candidates.add(pos.above());
                candidates.add(pos.below());
            }
            case AUTO -> addAutoAdjacentPositions(candidates, pos, face);
        }
    }

    private void addAutoAdjacentPositions(LinkedList<BlockPos> candidates, BlockPos pos, Direction face) {
        switch (face) {
            case DOWN, UP -> {
                candidates.add(pos.north());
                candidates.add(pos.south());
                candidates.add(pos.east());
                candidates.add(pos.west());
                candidates.add(pos.north().east());
                candidates.add(pos.north().west());
                candidates.add(pos.south().east());
                candidates.add(pos.south().west());
            }
            case NORTH, SOUTH -> {
                candidates.add(pos.above());
                candidates.add(pos.below());
                candidates.add(pos.east());
                candidates.add(pos.west());
                candidates.add(pos.above().east());
                candidates.add(pos.above().west());
                candidates.add(pos.below().east());
                candidates.add(pos.below().west());
            }
            case EAST, WEST -> {
                candidates.add(pos.above());
                candidates.add(pos.below());
                candidates.add(pos.north());
                candidates.add(pos.south());
                candidates.add(pos.above().north());
                candidates.add(pos.above().south());
                candidates.add(pos.below().north());
                candidates.add(pos.below().south());
            }
        }
    }

    private static boolean compareRTR(BlockHitResult rtr1, BlockHitResult rtr2) {
        if (rtr1 == null || rtr2 == null) return false;
        return rtr1.getBlockPos().equals(rtr2.getBlockPos()) && rtr1.getDirection().equals(rtr2.getDirection());
    }

    public void reset() {
        cachedPositions = null;
        lastRayTraceResult = null;
        lastWand = null;
        lastPlacementCount = 0;
        lastDirectionMode = null;
        lastPartPlacementPos = null;
        lastPartSide = null;
    }
}
