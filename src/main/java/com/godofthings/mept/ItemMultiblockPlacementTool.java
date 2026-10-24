package com.godofthings.mept;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.parts.PartPlacement;

import org.jetbrains.annotations.Nullable;

/**
 * ME 多方块放置工具（照抄上游 ItemMultiblockPlacementTool）：BFS 批量放置。
 * <p>
 * [本地改动] 能量检查与扣电调用已随「不消耗能源」要求删除。
 */
public class ItemMultiblockPlacementTool extends BasePlacementToolItem
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int[] PLACEMENT_COUNTS = {1, 8, 64, 256, 1024};

    /** 批量放置的方向模式。照抄上游。 */
    public enum DirectionMode
    {
        AUTO,
        NORTH_SOUTH,
        EAST_WEST,
        VERTICAL;

        public static DirectionMode fromId(int id)
        {
            DirectionMode[] values = values();
            if (id < 0 || id >= values.length)
            {
                return AUTO;
            }
            return values[id];
        }

        public DirectionMode next()
        {
            DirectionMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public String translationKey()
        {
            return "godofthings.mept.direction." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public ItemMultiblockPlacementTool(Item.Properties props)
    {
        super(props);
    }

    /** 打开合成请求界面。照抄上游。 */
    private void openCraftingMenu(ServerPlayer player, ItemStack wand, AEKey whatToCraft, int amount)
    {
        int wandSlot = findInventorySlot(player, wand);
        if (wandSlot >= 0)
        {
            CraftAmountMenu.open(player, MenuLocators.forInventorySlot(wandSlot), whatToCraft, amount);
        }
        else if (player.getMainHandItem() == wand)
        {
            CraftAmountMenu.open(player, MenuLocators.forHand(player, InteractionHand.MAIN_HAND), whatToCraft, amount);
        }
        else if (player.getOffhandItem() == wand)
        {
            CraftAmountMenu.open(player, MenuLocators.forHand(player, InteractionHand.OFF_HAND), whatToCraft, amount);
        }
    }

    private int findInventorySlot(Player player, ItemStack itemStack)
    {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            if (inv.getItem(i) == itemStack)
            {
                return i;
            }
        }
        return -1;
    }

    public static int getPlacementCount(ItemStack stack)
    {
        CompoundTag cfg = stack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg != null && cfg.contains("PlacementCount"))
        {
            int count = cfg.getInt("PlacementCount");
            for (int pc : PLACEMENT_COUNTS)
            {
                if (pc == count)
                {
                    return count;
                }
            }
        }
        return PLACEMENT_COUNTS[0];
    }

    public static int getNextPlacementCount(ItemStack stack, boolean forward)
    {
        int current = getPlacementCount(stack);
        for (int i = 0; i < PLACEMENT_COUNTS.length; i++)
        {
            if (PLACEMENT_COUNTS[i] == current)
            {
                int nextIndex = forward
                        ? (i + 1) % PLACEMENT_COUNTS.length
                        : (i - 1 + PLACEMENT_COUNTS.length) % PLACEMENT_COUNTS.length;
                return PLACEMENT_COUNTS[nextIndex];
            }
        }
        return PLACEMENT_COUNTS[0];
    }

    public static DirectionMode getDirectionMode(ItemStack stack)
    {
        CompoundTag cfg = stack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg != null && cfg.contains("DirectionMode"))
        {
            return DirectionMode.fromId(cfg.getInt("DirectionMode"));
        }
        return DirectionMode.AUTO;
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        if (level.isClientSide)
        {
            return InteractionResult.sidedSuccess(true);
        }

        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return InteractionResult.PASS;
        }

        ItemStack wand = player.getItemInHand(context.getHand());
        int placementCount = getPlacementCount(wand);

        // [本地改动] 上游此处有电量检查，不消耗能源版本直接跳过

        var grid = this.getLinkedGrid(wand, level, player);
        if (grid == null)
        {
            return InteractionResult.FAIL;
        }

        CompoundTag cfg = wand.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg == null)
        {
            cfg = new CompoundTag();
        }

        int selected = cfg.getInt("SelectedSlot");
        if (selected < 0 || selected >= 18)
        {
            selected = 0;
        }

        ItemStack target = getItemFromConfig(cfg, selected, level.registryAccess());
        if (target == null || target.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.no_configured_item"), true);
            return InteractionResult.FAIL;
        }

        DirectionMode directionMode = DirectionMode.fromId(cfg.getInt("DirectionMode"));

        var storage = grid.getStorageService().getInventory();
        var src = new PlayerSource(player);

        // 流体放置
        try
        {
            var unwrapped = GenericStack.unwrapItemStack(target);
            if (unwrapped != null && AEFluidKey.is(unwrapped.what()))
            {
                return handleFluidMultiPlacement(context, player, wand, storage, src,
                        (AEFluidKey) unwrapped.what(), placementCount, directionMode);
            }
        }
        catch (Throwable ignored)
        {
        }

        String fluidId = getFluidFromConfig(cfg, selected);
        if (fluidId != null)
        {
            return handleFluidIdMultiPlacement(context, player, wand, storage, src, fluidId, placementCount,
                    directionMode);
        }

        // 方块放置——找出全部匹配 key（遵循 NBT 白名单）
        var matchingKeys = MeptConfig.findAllMatchingKeys(storage, target);
        if (matchingKeys.isEmpty())
        {
            var craftKey = AEItemKey.of(target);
            var craftingService = grid.getCraftingService();
            if (craftingService != null && craftKey != null && craftingService.isCraftable(craftKey))
            {
                openCraftingMenu(serverPlayer, wand, craftKey, placementCount);
                return InteractionResult.sidedSuccess(false);
            }
            player.displayClientMessage(Component.translatable("message.godofthings.mept.network_missing",
                    target.getHoverName()), true);
            return InteractionResult.FAIL;
        }

        long totalAvailable = matchingKeys.stream().mapToLong(Map.Entry::getValue).sum();

        // AE2 部件挂在缆面，不是相邻空位
        if (target.getItem() instanceof IPartItem<?>)
        {
            return handlePartMultiPlacement(context, player, wand, grid, storage, src, target,
                    matchingKeys, totalAvailable, placementCount, directionMode);
        }

        if (!(target.getItem() instanceof BlockItem blockItem))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.unsupported_target"), true);
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        var clickedFace = context.getClickedFace();
        var clickedState = level.getBlockState(clickedPos);

        List<BlockPos> placePositions = calculatePlacementPositions(player, level, clickedPos, clickedFace,
                clickedState, target, placementCount, directionMode);

        if (placePositions.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
            return InteractionResult.sidedSuccess(false);
        }

        if (totalAvailable < placePositions.size())
        {
            var craftKey = AEItemKey.of(target);
            var craftingService = grid.getCraftingService();
            if (craftingService != null && craftKey != null && craftingService.isCraftable(craftKey))
            {
                int missingAmount = (int) (placePositions.size() - totalAvailable);
                openCraftingMenu(serverPlayer, wand, craftKey, missingAmount);
                return InteractionResult.sidedSuccess(false);
            }
            player.displayClientMessage(Component.translatable("message.godofthings.mept.network_missing",
                    target.getHoverName()), true);
            return InteractionResult.FAIL;
        }

        // 内存卡资源检查
        if (MeptMemoryCardHelper.hasConfiguredMemoryCard(player))
        {
            var resourceCheck = MeptMemoryCardHelper.checkResourcesForMultipleBlocks(player, grid,
                    placePositions.size());
            if (!resourceCheck.sufficient)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.missing_resources",
                        resourceCheck.getMissingItemsMessage()), false);
                return InteractionResult.sidedSuccess(false);
            }
        }

        boolean hasMekConfigCard = MeptModCompat.isMekanismLoaded()
                && MeptMekanismConfigCardHelper.hasConfiguredConfigCard(player);

        ItemStack savedOffHand = player.getOffhandItem().copy();

        List<Map.Entry<AEItemKey, Long>> availableKeys = new ArrayList<>();
        for (var entry : matchingKeys)
        {
            availableKeys.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }

        int placedCount = 0;
        List<MeptUndoHistory.PlacementSnapshot> placedSnapshots = new ArrayList<>();

        for (BlockPos placePos : placePositions)
        {
            if (!level.mayInteract(player, placePos)
                    || !player.mayUseItemAt(placePos, clickedFace.getOpposite(), target))
            {
                continue;
            }

            AEItemKey currentKey = null;
            for (var entry : availableKeys)
            {
                if (entry.getValue() > 0)
                {
                    currentKey = entry.getKey();
                    entry.setValue(entry.getValue() - 1);
                    break;
                }
            }

            if (currentKey == null)
            {
                break;
            }

            var placeStack = currentKey.toStack(1);
            // 先扣再放——避免 SIMULATE 与 MODULATE 之间网络内容变化时留下免费方块
            boolean extracted = !player.isCreative();
            if (extracted && storage.extract(currentKey, 1, Actionable.MODULATE, src) < 1)
            {
                break;
            }

            ItemStack origMain = player.getMainHandItem();
            ItemStack origOff = player.getOffhandItem();
            boolean placedThis = false;

            try
            {
                player.setItemInHand(InteractionHand.MAIN_HAND, placeStack);
                // 在目标位置合成点击坐标，方向敏感方块（台阶/楼梯/原木）不用远处的原点击点
                var hitVec = Vec3.atCenterOf(placePos.relative(context.getClickedFace().getOpposite()))
                        .add(Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(0.5));
                BlockPlaceContext placeContext = new BlockPlaceContext(
                        level, player, InteractionHand.MAIN_HAND, placeStack,
                        new BlockHitResult(hitVec, context.getClickedFace(), placePos, context.isInside()));
                var result = blockItem.place(placeContext);
                if (result.consumesAction())
                {
                    placedThis = true;
                    placedCount++;
                    placedSnapshots.add(new MeptUndoHistory.PlacementSnapshot(
                            level.getBlockState(placePos), placePos, placeStack, currentKey, 1));
                }
            }
            catch (Throwable t)
            {
                LOGGER.warn("Exception during placement at {}", placePos, t);
            }
            finally
            {
                player.setItemInHand(InteractionHand.MAIN_HAND, origMain);
                player.setItemInHand(InteractionHand.OFF_HAND, origOff);
            }

            if (!placedThis && extracted)
            {
                storage.insert(currentKey, 1, Actionable.MODULATE, src);
            }
        }

        if (placedCount == 0)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
            return InteractionResult.sidedSuccess(false);
        }

        player.setItemInHand(InteractionHand.OFF_HAND, savedOffHand);

        // 应用内存卡 / 配置卡到全部已放置方块
        boolean configApplied = false;

        if (MeptMemoryCardHelper.hasConfiguredMemoryCard(player))
        {
            boolean firstBlock = true;
            for (MeptUndoHistory.PlacementSnapshot snapshot : placedSnapshots)
            {
                if (MeptMemoryCardHelper.applyMemoryCardToBlock(player, level, snapshot.pos, firstBlock, grid))
                {
                    configApplied = true;
                }
                firstBlock = false;
            }
        }
        else if (hasMekConfigCard)
        {
            boolean firstBlock = true;
            for (MeptUndoHistory.PlacementSnapshot snapshot : placedSnapshots)
            {
                if (MeptMekanismConfigCardHelper.applyConfigCardToBlock(player, level, snapshot.pos, firstBlock))
                {
                    configApplied = true;
                }
                firstBlock = false;
            }
        }

        // AE2LT 过载频率卡自动连接
        for (MeptUndoHistory.PlacementSnapshot snapshot : placedSnapshots)
        {
            MeptAe2ltHelper.queueAutoConnect(player, level, snapshot.pos, null);
        }

        MeptRegistry.undoHistory().add(player, level, placedSnapshots, configApplied);

        // [本地改动] 上游此处按放置数量扣电，不消耗能源版本跳过

        if (!placedSnapshots.isEmpty())
        {
            BlockPos soundPos = placedSnapshots.get(0).pos;
            var placedState = level.getBlockState(soundPos);
            var soundType = placedState.getSoundType(level, soundPos, player);
            level.playSound(null, soundPos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        }

        return InteractionResult.sidedSuccess(false);
    }

    /** BFS/轴向锁定生成放置位置。照抄上游。 */
    private List<BlockPos> calculatePlacementPositions(Player player, Level level, BlockPos clickedPos,
            net.minecraft.core.Direction clickedFace, net.minecraft.world.level.block.state.BlockState clickedState,
            ItemStack placementStack, int maxCount, DirectionMode directionMode)
    {
        LinkedList<BlockPos> candidates = new LinkedList<>();
        HashSet<BlockPos> allCandidates = new HashSet<>();
        ArrayList<BlockPos> placePositions = new ArrayList<>();

        final int MAX_CANDIDATES = maxCount * 10;

        BlockPos startingPoint = clickedPos.relative(clickedFace);
        candidates.add(startingPoint);

        while (!candidates.isEmpty() && placePositions.size() < maxCount && allCandidates.size() < MAX_CANDIDATES)
        {
            BlockPos currentCandidate = candidates.removeFirst();
            if (!allCandidates.add(currentCandidate))
            {
                continue;
            }

            // 与 ConstructionWand 一致：即使方向锁了，「后面」的支撑方块也必须与点击方块相同
            BlockPos supportingPoint = currentCandidate.relative(clickedFace.getOpposite());
            var supportingState = level.getBlockState(supportingPoint);

            if (supportingState.getBlock() == clickedState.getBlock())
            {
                var currentState = level.getBlockState(currentCandidate);
                boolean canPlace = level.isEmptyBlock(currentCandidate);
                if (!canPlace)
                {
                    try
                    {
                        BlockPlaceContext checkContext = new BlockPlaceContext(new UseOnContext(
                                player, InteractionHand.MAIN_HAND, new BlockHitResult(
                                        player.getEyePosition(), clickedFace, currentCandidate, false)));
                        canPlace = currentState.canBeReplaced(checkContext);
                    }
                    catch (Throwable t)
                    {
                    }
                }

                if (canPlace && (!level.mayInteract(player, currentCandidate)
                        || !player.mayUseItemAt(currentCandidate, clickedFace.getOpposite(), placementStack)))
                {
                    canPlace = false;
                }

                if (canPlace)
                {
                    placePositions.add(currentCandidate);
                    // 只有放置成功才扩展候选（防止跨坑溢出）
                    addAdjacentPositions(candidates, currentCandidate, clickedFace, directionMode);
                }
            }
        }

        return placePositions;
    }

    private void addAdjacentPositions(LinkedList<BlockPos> candidates, BlockPos pos, net.minecraft.core.Direction face,
            DirectionMode directionMode)
    {
        switch (directionMode)
        {
            case NORTH_SOUTH ->
            {
                candidates.add(pos.north());
                candidates.add(pos.south());
            }
            case EAST_WEST ->
            {
                candidates.add(pos.east());
                candidates.add(pos.west());
            }
            case VERTICAL ->
            {
                candidates.add(pos.above());
                candidates.add(pos.below());
            }
            case AUTO -> addAutoAdjacentPositions(candidates, pos, face);
        }
    }

    private void addAutoAdjacentPositions(LinkedList<BlockPos> candidates, BlockPos pos, net.minecraft.core.Direction face)
    {
        switch (face)
        {
            case DOWN, UP ->
            {
                candidates.add(pos.north());
                candidates.add(pos.south());
                candidates.add(pos.east());
                candidates.add(pos.west());
                candidates.add(pos.north().east());
                candidates.add(pos.north().west());
                candidates.add(pos.south().east());
                candidates.add(pos.south().west());
            }
            case NORTH, SOUTH ->
            {
                candidates.add(pos.east());
                candidates.add(pos.west());
                candidates.add(pos.above());
                candidates.add(pos.below());
                candidates.add(pos.above().east());
                candidates.add(pos.above().west());
                candidates.add(pos.below().east());
                candidates.add(pos.below().west());
            }
            case EAST, WEST ->
            {
                candidates.add(pos.north());
                candidates.add(pos.south());
                candidates.add(pos.above());
                candidates.add(pos.below());
                candidates.add(pos.above().north());
                candidates.add(pos.above().south());
                candidates.add(pos.below().north());
                candidates.add(pos.below().south());
            }
        }
    }

    private InteractionResult handleFluidMultiPlacement(UseOnContext context, Player player, ItemStack wand,
            MEStorage storage, PlayerSource src, AEFluidKey aeFluidKey,
            int placementCount, DirectionMode directionMode)
    {
        Level level = context.getLevel();
        var fluid = aeFluidKey.getFluid();

        if (!(fluid instanceof FlowingFluid))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.unsupported_target"), true);
            return InteractionResult.FAIL;
        }

        var legacyBlock = fluid.defaultFluidState().createLegacyBlock();
        BlockPos clickedPos = context.getClickedPos();
        var clickedFace = context.getClickedFace();
        var clickedState = level.getBlockState(clickedPos);

        List<BlockPos> placePositions = calculateFluidPlacementPositions(level, clickedPos, clickedFace,
                clickedState, fluid, legacyBlock, placementCount, directionMode);

        if (placePositions.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
            return InteractionResult.sidedSuccess(false);
        }

        long totalFluidNeeded = (long) placePositions.size() * AEFluidKey.AMOUNT_BLOCK;
        long simAvail = storage.extract(aeFluidKey, totalFluidNeeded, Actionable.SIMULATE, src);
        if (simAvail < totalFluidNeeded)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.network_missing",
                    aeFluidKey.getDisplayName()), true);
            return InteractionResult.FAIL;
        }

        int placedCount = 0;
        for (BlockPos placePos : placePositions)
        {
            if (!level.mayInteract(player, placePos)
                    || !player.mayUseItemAt(placePos, clickedFace.getOpposite(), wand))
            {
                continue;
            }
            // 先扣再放；创造模式不动网络
            boolean extracted = !player.isCreative();
            if (extracted)
            {
                long extractedAmount = storage.extract(aeFluidKey, AEFluidKey.AMOUNT_BLOCK, Actionable.MODULATE, src);
                if (extractedAmount < AEFluidKey.AMOUNT_BLOCK)
                {
                    if (extractedAmount > 0)
                    {
                        storage.insert(aeFluidKey, extractedAmount, Actionable.MODULATE, src);
                    }
                    break;
                }
            }
            boolean success = false;
            try
            {
                var stateAtPos = level.getBlockState(placePos);
                boolean isLiquidContainer = stateAtPos.getBlock() instanceof LiquidBlockContainer;

                if (level.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER))
                {
                    success = true;
                }
                else if (isLiquidContainer && fluid == Fluids.WATER)
                {
                    ((LiquidBlockContainer) stateAtPos.getBlock())
                            .placeLiquid(level, placePos, stateAtPos, ((FlowingFluid) fluid).getSource(false));
                    success = true;
                }
                else
                {
                    boolean canBeReplaced = false;
                    try
                    {
                        canBeReplaced = stateAtPos.canBeReplaced(fluid);
                    }
                    catch (Throwable ignored)
                    {
                    }
                    if (canBeReplaced && !stateAtPos.liquid())
                    {
                        level.destroyBlock(placePos, true);
                    }
                    success = level.setBlock(placePos, legacyBlock, Block.UPDATE_ALL_IMMEDIATE);
                }
            }
            catch (Throwable t)
            {
                LOGGER.warn("Exception during fluid placement at {}", placePos, t);
            }
            if (success)
            {
                placedCount++;
            }
            else if (extracted)
            {
                storage.insert(aeFluidKey, AEFluidKey.AMOUNT_BLOCK, Actionable.MODULATE, src);
            }
        }

        if (placedCount > 0)
        {
            level.playSound(null, clickedPos.relative(clickedFace), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS,
                    1.0F, 1.0F);
            return InteractionResult.sidedSuccess(false);
        }

        player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
        return InteractionResult.sidedSuccess(false);
    }

    private List<BlockPos> calculateFluidPlacementPositions(Level level, BlockPos clickedPos,
            net.minecraft.core.Direction clickedFace, net.minecraft.world.level.block.state.BlockState clickedState,
            net.minecraft.world.level.material.Fluid fluid, net.minecraft.world.level.block.state.BlockState legacyBlock,
            int maxCount, DirectionMode directionMode)
    {
        LinkedList<BlockPos> candidates = new LinkedList<>();
        HashSet<BlockPos> allCandidates = new HashSet<>();
        ArrayList<BlockPos> placePositions = new ArrayList<>();

        final int MAX_CANDIDATES = maxCount * 10;

        BlockPos startingPoint = clickedPos.relative(clickedFace);
        candidates.add(startingPoint);

        while (!candidates.isEmpty() && placePositions.size() < maxCount && allCandidates.size() < MAX_CANDIDATES)
        {
            BlockPos currentCandidate = candidates.removeFirst();
            if (!allCandidates.add(currentCandidate))
            {
                continue;
            }

            BlockPos supportingPoint = currentCandidate.relative(clickedFace.getOpposite());
            var supportingState = level.getBlockState(supportingPoint);

            if (supportingState.getBlock() == clickedState.getBlock())
            {
                var stateAtPos = level.getBlockState(currentCandidate);
                boolean stateIsLegacy = stateAtPos == legacyBlock;
                boolean stateIsAir = stateAtPos.isAir();
                boolean canBeReplaced = false;
                try
                {
                    canBeReplaced = stateAtPos.canBeReplaced(fluid);
                }
                catch (Throwable ignored)
                {
                }
                boolean isLiquidContainer = stateAtPos.getBlock() instanceof LiquidBlockContainer;
                boolean containerCanPlace = false;
                if (isLiquidContainer)
                {
                    try
                    {
                        containerCanPlace = ((LiquidBlockContainer) stateAtPos.getBlock())
                                .canPlaceLiquid(null, level, currentCandidate, stateAtPos, fluid);
                    }
                    catch (Throwable ignored)
                    {
                    }
                }

                boolean canPlace = !stateIsLegacy
                        && (stateIsAir || canBeReplaced || (isLiquidContainer && containerCanPlace));
                if (canPlace)
                {
                    placePositions.add(currentCandidate);
                    addAdjacentPositions(candidates, currentCandidate, clickedFace, directionMode);
                }
            }
        }

        return placePositions;
    }

    private InteractionResult handleFluidIdMultiPlacement(UseOnContext context, Player player, ItemStack wand,
            MEStorage storage, PlayerSource src, String fluidId,
            int placementCount, DirectionMode directionMode)
    {
        try
        {
            var fid = ResourceLocation.tryParse(fluidId);
            if (fid == null)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.unsupported_target"), true);
                return InteractionResult.FAIL;
            }

            var fluid = BuiltInRegistries.FLUID.get(fid);
            if (fluid == null || fluid == Fluids.EMPTY)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.unsupported_target"), true);
                return InteractionResult.FAIL;
            }

            var aeFluidKey = AEFluidKey.of(fluid);
            return handleFluidMultiPlacement(context, player, wand, storage, src, aeFluidKey, placementCount,
                    directionMode);
        }
        catch (Exception e)
        {
            LOGGER.warn("Error resolving fluid {}", fluidId, e);
            player.displayClientMessage(Component.translatable("message.godofthings.mept.unsupported_target"), true);
            return InteractionResult.FAIL;
        }
    }

    /** 批量放置 AE2 部件到缆面（对齐普通右键放置）。照抄上游。 */
    private InteractionResult handlePartMultiPlacement(UseOnContext context, Player player, ItemStack wand,
            IGrid grid, MEStorage storage, PlayerSource src, ItemStack target,
            List<Map.Entry<AEItemKey, Long>> matchingKeys, long totalAvailable,
            int placementCount, DirectionMode directionMode)
    {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        var clickedState = level.getBlockState(clickedPos);

        var firstPlacement = getPartPlacementWithCableFallback(player, level, target, clickedPos,
                clickedFace, context.getClickLocation());
        if (firstPlacement == null)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
            return InteractionResult.sidedSuccess(false);
        }

        Direction partSide = firstPlacement.side();
        boolean placingOnClickedHost = firstPlacement.pos().equals(clickedPos);

        List<BlockPos> placePositions = calculatePartPlacementPositions(
                player, level, firstPlacement.pos(), partSide, clickedFace, clickedState,
                target, placementCount, directionMode, placingOnClickedHost);

        if (placePositions.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
            return InteractionResult.sidedSuccess(false);
        }

        if (totalAvailable < placePositions.size())
        {
            var craftKey = AEItemKey.of(target);
            var craftingService = grid.getCraftingService();
            if (craftingService != null && craftKey != null && craftingService.isCraftable(craftKey)
                    && player instanceof ServerPlayer serverPlayer)
            {
                int missingAmount = (int) (placePositions.size() - totalAvailable);
                openCraftingMenu(serverPlayer, wand, craftKey, missingAmount);
                return InteractionResult.sidedSuccess(false);
            }
            player.displayClientMessage(Component.translatable("message.godofthings.mept.network_missing",
                    target.getHoverName()), true);
            return InteractionResult.FAIL;
        }

        List<Map.Entry<AEItemKey, Long>> availableKeys = new ArrayList<>();
        for (var entry : matchingKeys)
        {
            availableKeys.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }

        int placedCount = 0;
        List<MeptUndoHistory.PlacementSnapshot> placedSnapshots = new ArrayList<>();

        for (BlockPos placePos : placePositions)
        {
            if (!level.mayInteract(player, placePos)
                    || !player.mayUseItemAt(placePos, partSide, target))
            {
                continue;
            }

            AEItemKey currentKey = null;
            for (var entry : availableKeys)
            {
                if (entry.getValue() > 0)
                {
                    currentKey = entry.getKey();
                    entry.setValue(entry.getValue() - 1);
                    break;
                }
            }

            if (currentKey == null)
            {
                break;
            }

            var placeStack = currentKey.toStack(1);
            if (!(placeStack.getItem() instanceof IPartItem<?> partItem))
            {
                continue;
            }

            boolean extracted = !player.isCreative();
            if (extracted && storage.extract(currentKey, 1, Actionable.MODULATE, src) < 1)
            {
                break;
            }

            if (placePart(player, level, placePos, partSide, partItem, placeStack))
            {
                placedCount++;
                placedSnapshots.add(new MeptUndoHistory.PartPlacementSnapshot(placePos, partSide, currentKey));
            }
            else if (extracted)
            {
                storage.insert(currentKey, 1, Actionable.MODULATE, src);
            }
        }

        if (placedCount == 0)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
            return InteractionResult.sidedSuccess(false);
        }

        boolean configApplied = false;
        if (MeptMemoryCardHelper.hasConfiguredMemoryCard(player))
        {
            boolean firstPart = true;
            for (MeptUndoHistory.PlacementSnapshot snapshot : placedSnapshots)
            {
                var host = PartHelper.getPartHost(level, snapshot.pos);
                if (host != null
                        && MeptMemoryCardHelper.applyMemoryCardToPart(player, host.getPart(partSide), firstPart, grid))
                {
                    configApplied = true;
                }
                firstPart = false;
            }
        }

        for (MeptUndoHistory.PlacementSnapshot snapshot : placedSnapshots)
        {
            MeptAe2ltHelper.queueAutoConnect(player, level, snapshot.pos, partSide);
        }

        MeptRegistry.undoHistory().add(player, level, placedSnapshots, configApplied);

        // [本地改动] 上游此处按放置数量扣电，不消耗能源版本跳过

        if (!placedSnapshots.isEmpty())
        {
            BlockPos soundPos = placedSnapshots.get(0).pos;
            var placedState = level.getBlockState(soundPos);
            var soundType = placedState.getSoundType(level, soundPos, player);
            level.playSound(null, soundPos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        }

        return InteractionResult.sidedSuccess(false);
    }

    private List<BlockPos> calculatePartPlacementPositions(Player player, Level level, BlockPos startPos,
            Direction partSide, Direction clickedFace, net.minecraft.world.level.block.state.BlockState clickedState,
            ItemStack partStack, int maxCount, DirectionMode directionMode, boolean placingOnClickedHost)
    {
        LinkedList<BlockPos> candidates = new LinkedList<>();
        HashSet<BlockPos> allCandidates = new HashSet<>();
        ArrayList<BlockPos> placePositions = new ArrayList<>();

        final int MAX_CANDIDATES = maxCount * 10;
        candidates.add(startPos);

        while (!candidates.isEmpty() && placePositions.size() < maxCount && allCandidates.size() < MAX_CANDIDATES)
        {
            BlockPos currentCandidate = candidates.removeFirst();
            if (!allCandidates.add(currentCandidate))
            {
                continue;
            }

            boolean supportMatches;
            if (placingOnClickedHost)
            {
                supportMatches = level.getBlockState(currentCandidate).getBlock() == clickedState.getBlock();
            }
            else
            {
                supportMatches = level.getBlockState(currentCandidate.relative(partSide)).getBlock()
                        == clickedState.getBlock();
            }

            if (supportMatches
                    && level.mayInteract(player, currentCandidate)
                    && player.mayUseItemAt(currentCandidate, partSide, partStack)
                    && canPlaceConfiguredPart(player, level, partStack, currentCandidate, partSide))
            {
                placePositions.add(currentCandidate);
                addAdjacentPositions(candidates, currentCandidate, clickedFace, directionMode);
            }
        }

        return placePositions;
    }

    private boolean canPlaceConfiguredPart(Player player, Level level, ItemStack partStack, BlockPos pos, Direction side)
    {
        return PartPlacement.canPlacePartOnBlock(player, level, partStack, pos, side);
    }

    private PartPlacement.Placement getPartPlacementWithCableFallback(Player player, Level level, ItemStack partStack,
            BlockPos clickedPos, Direction clickedFace, Vec3 clickLocation)
    {
        var placement = PartPlacement.getPartPlacement(player, level, partStack, clickedPos, clickedFace, clickLocation);
        if (placement != null)
        {
            return placement;
        }

        var host = PartHelper.getPartHost(level, clickedPos);
        if (host != null && host.getPart(null) != null && host.canAddPart(partStack, clickedFace))
        {
            return new PartPlacement.Placement(clickedPos, clickedFace);
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean placePart(Player player, Level level, BlockPos pos, Direction side, IPartItem<?> partItem,
            ItemStack partStack)
    {
        try
        {
            return PartPlacement.placePart(player, level, (IPartItem) partItem, partStack.getComponents(), pos, side)
                    != null;
        }
        catch (Exception t)
        {
            LOGGER.warn("Exception during part placement attempt for player {} at {}",
                    player.getName().getString(), pos, t);
            return false;
        }
    }

    private ItemStack getItemFromConfig(CompoundTag cfg, int slot, net.minecraft.core.HolderLookup.Provider registries)
    {
        if (cfg == null)
        {
            return ItemStack.EMPTY;
        }

        CompoundTag itemsTag = cfg.contains("items") ? cfg.getCompound("items") : cfg;
        if (itemsTag.contains("Items"))
        {
            ListTag list = itemsTag.getList("Items", 10);
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag itemTag = list.getCompound(i);
                if (itemTag.getInt("Slot") == slot)
                {
                    return ItemStack.parseOptional(registries, itemTag);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private String getFluidFromConfig(CompoundTag cfg, int slot)
    {
        if (cfg == null || !cfg.contains("fluids"))
        {
            return null;
        }
        var ftag = cfg.getCompound("fluids");
        String key = Integer.toString(slot);
        if (ftag.contains(key))
        {
            String fluidId = ftag.getString(key);
            return fluidId.isEmpty() ? null : fluidId;
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        var hr = player.pick(5.0D, 0.0F, false);
        if (hr != null && hr.getType() != net.minecraft.world.phys.HitResult.Type.MISS)
        {
            return new InteractionResultHolder<>(InteractionResult.PASS, stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer)
        {
            player.openMenu(new net.minecraft.world.MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.empty();
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
                        net.minecraft.world.entity.player.Inventory inv, Player p)
                {
                    return new WandMenu(id, inv, stack);
                }
            }, buf -> {
                CompoundTag cfg = stack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
                buf.writeNbt(cfg != null ? cfg : new CompoundTag());
            });
        }

        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()), stack);
    }
}
