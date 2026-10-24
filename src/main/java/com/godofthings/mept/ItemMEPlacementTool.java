package com.godofthings.mept;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import appeng.api.config.Actionable;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.me.helpers.PlayerSource;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartPlacement;

/**
 * ME 放置工具（照抄上游 ItemMEPlacementTool）：从 AE 网络直接放置物品。
 * <p>
 * [本地改动] 能量检查与扣电调用已随「不消耗能源」要求删除（基类 hasPower/usePower 恒真，
 * 因此这里的调用保留也不产生任何效果，与上游代码结构保持一致便于日后对照）。
 */
public class ItemMEPlacementTool extends BasePlacementToolItem
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public ItemMEPlacementTool(Item.Properties props)
    {
        super(props);
    }

    /** 为可合成物品打开合成请求界面。照抄上游。 */
    private void openCraftingMenu(ServerPlayer player, ItemStack wand, AEKey whatToCraft)
    {
        int wandSlot = findInventorySlot(player, wand);
        if (wandSlot >= 0)
        {
            CraftAmountMenu.open(player, MenuLocators.forInventorySlot(wandSlot), whatToCraft, 1);
        }
        else if (player.getMainHandItem() == wand)
        {
            CraftAmountMenu.open(player, MenuLocators.forHand(player, InteractionHand.MAIN_HAND), whatToCraft, 1);
        }
        else if (player.getOffhandItem() == wand)
        {
            CraftAmountMenu.open(player, MenuLocators.forHand(player, InteractionHand.OFF_HAND), whatToCraft, 1);
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

        var storage = grid.getStorageService().getInventory();
        var src = new PlayerSource(player);

        BlockPos lastPlacementPos = null;
        boolean lastPlacementWasBlock = false;
        IPart lastPlacedPart = null;
        Direction lastPlacedPartSide = null;

        // 包装流体（GenericStack）
        try
        {
            var unwrapped = GenericStack.unwrapItemStack(target);
            if (unwrapped != null && AEFluidKey.is(unwrapped.what()))
            {
                return handleFluidPlacement(context, player, wand, storage, src, (AEFluidKey) unwrapped.what());
            }
        }
        catch (Throwable ignored)
        {
        }

        // 流体配置
        String fluidId = getFluidFromConfig(cfg, selected);
        if (fluidId != null)
        {
            return handleFluidIdPlacement(context, player, wand, storage, src, fluidId);
        }

        // 在网络中找匹配物品
        var aeKey = MeptConfig.findMatchingKey(storage, target);
        if (aeKey == null)
        {
            var craftKey = AEItemKey.of(target);
            var craftingService = grid.getCraftingService();
            if (craftingService != null && craftKey != null && craftingService.isCraftable(craftKey))
            {
                openCraftingMenu(serverPlayer, wand, craftKey);
                return InteractionResult.sidedSuccess(false);
            }
            player.displayClientMessage(Component.translatable("message.godofthings.mept.network_missing",
                    target.getHoverName()), true);
            return InteractionResult.FAIL;
        }

        // 模拟抽取
        long avail = storage.extract(aeKey, 1L, Actionable.SIMULATE, src);
        if (avail <= 0)
        {
            var craftingService = grid.getCraftingService();
            if (craftingService != null && craftingService.isCraftable(aeKey))
            {
                openCraftingMenu(serverPlayer, wand, aeKey);
                return InteractionResult.sidedSuccess(false);
            }
            player.displayClientMessage(Component.translatable("message.godofthings.mept.network_missing",
                    target.getHoverName()), true);
            return InteractionResult.FAIL;
        }

        // 内存卡资源检查
        if (MeptMemoryCardHelper.hasConfiguredMemoryCard(player))
        {
            var resourceCheck = MeptMemoryCardHelper.checkResourcesForMultipleBlocks(player, grid, 1);
            if (!resourceCheck.sufficient)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.missing_resources",
                        resourceCheck.getMissingItemsMessage()), false);
                return InteractionResult.sidedSuccess(false);
            }
        }

        // 通用机械配置卡
        boolean hasMekConfigCard = MeptModCompat.isMekanismLoaded()
                && MeptMekanismConfigCardHelper.hasConfiguredConfigCard(player);

        ItemStack placeStack = aeKey.toStack(1);
        BlockPlaceContext blockPlaceContext = createBlockPlaceContext(context, player, placeStack);
        BlockPos blockPlacePos = blockPlaceContext.getClickedPos();

        if (placeStack.getItem() instanceof net.minecraft.world.item.BlockItem
                && !canUseAt(player, level, blockPlacePos, context.getClickedFace().getOpposite(), placeStack))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
            return InteractionResult.FAIL;
        }

        // 先扣再放（创造模式不动网络）。照抄上游。
        boolean debited = player.isCreative()
                || storage.extract(aeKey, 1L, Actionable.MODULATE, src) >= 1;
        if (!debited)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.network_missing",
                    target.getHoverName()), true);
            return InteractionResult.FAIL;
        }

        boolean placed = false;

        try
        {
            if (placeStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)
            {
                placed = tryPlaceBlock(player, placeStack, blockItem, blockPlaceContext);
                if (placed)
                {
                    lastPlacementPos = blockPlacePos;
                    lastPlacementWasBlock = true;
                }
            }
            else if (placeStack.getItem() instanceof IPartItem<?>)
            {
                var result = tryPlacePart(context, player, level, placeStack);
                if (result != null)
                {
                    placed = true;
                    lastPlacementPos = result.pos();
                    lastPlacementWasBlock = false;
                    lastPlacedPart = result.part();
                    lastPlacedPartSide = result.side();
                }
            }
            else if (placeStack.getItem() instanceof appeng.api.implementations.items.IFacadeItem)
            {
                placed = tryPlaceFacade(context, player, level, placeStack);
                if (placed)
                {
                    lastPlacementPos = context.getClickedPos();
                    lastPlacementWasBlock = false;
                }
            }
        }
        catch (Throwable t)
        {
            LOGGER.warn("Exception during placement for player {} at {}",
                    player.getName().getString(), blockPlacePos, t);
        }

        if (placed)
        {
            // [本地改动] 上游此处扣电，不消耗能源版本跳过

            // 播放方块自己的放置音效
            BlockPos soundPos = lastPlacementPos != null ? lastPlacementPos : blockPlacePos;
            var placedState = level.getBlockState(soundPos);
            var soundType = placedState.getSoundType(level, soundPos, player);
            level.playSound(null, soundPos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

            // 应用内存卡 / 配置卡
            if (MeptMemoryCardHelper.hasConfiguredMemoryCard(player))
            {
                if (lastPlacementWasBlock)
                {
                    MeptMemoryCardHelper.applyMemoryCardToBlock(player, level, soundPos, true, grid);
                }
                else if (lastPlacedPart != null)
                {
                    MeptMemoryCardHelper.applyMemoryCardToPart(player, lastPlacedPart, true, grid);
                }
            }
            else if (hasMekConfigCard && lastPlacementWasBlock)
            {
                MeptMekanismConfigCardHelper.applyConfigCardToBlock(player, level, soundPos, true);
            }

            // AE2LT 过载频率卡自动连接（软依赖，未装则跳过）
            if (lastPlacementWasBlock)
            {
                MeptAe2ltHelper.queueAutoConnect(player, level, soundPos, null);
            }
            else if (lastPlacedPartSide != null)
            {
                MeptAe2ltHelper.queueAutoConnect(player, level, soundPos, lastPlacedPartSide);
            }
        }
        else
        {
            if (!player.isCreative())
            {
                returnToStorageOrPlayer(player, storage, src, aeKey, 1);
            }
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
        }

        return InteractionResult.sidedSuccess(false);
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

    private InteractionResult handleFluidPlacement(UseOnContext context, Player player, ItemStack wand,
            appeng.api.storage.MEStorage storage, PlayerSource src, AEFluidKey aeFluidKey)
    {
        Level level = context.getLevel();
        BlockPos fluidPlacePos = context.getClickedPos().relative(context.getClickedFace());
        var fluid = aeFluidKey.getFluid();

        // 世界/圈地保护（原版桶行为）
        if (!level.mayInteract(player, fluidPlacePos)
                || !player.mayUseItemAt(fluidPlacePos, context.getClickedFace().getOpposite(), wand))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
            return InteractionResult.FAIL;
        }

        long simAvail = storage.extract(aeFluidKey, AEFluidKey.AMOUNT_BLOCK, Actionable.SIMULATE, src);
        if (simAvail < AEFluidKey.AMOUNT_BLOCK)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.network_missing",
                    aeFluidKey.getDisplayName()), true);
            return InteractionResult.FAIL;
        }

        boolean debited = player.isCreative();
        if (!debited)
        {
            long extracted = storage.extract(aeFluidKey, AEFluidKey.AMOUNT_BLOCK, Actionable.MODULATE, src);
            if (extracted < AEFluidKey.AMOUNT_BLOCK)
            {
                if (extracted > 0)
                {
                    storage.insert(aeFluidKey, extracted, Actionable.MODULATE, src);
                }
                player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
                return InteractionResult.sidedSuccess(false);
            }
            debited = true;
        }

        boolean placedFluid = tryPlaceFluid(level, fluidPlacePos, fluid);

        if (placedFluid)
        {
            level.playSound(null, fluidPlacePos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(false);
        }

        if (debited && !player.isCreative())
        {
            storage.insert(aeFluidKey, AEFluidKey.AMOUNT_BLOCK, Actionable.MODULATE, src);
        }
        player.displayClientMessage(Component.translatable("message.godofthings.mept.cannot_place"), true);
        return InteractionResult.sidedSuccess(false);
    }

    private InteractionResult handleFluidIdPlacement(UseOnContext context, Player player, ItemStack wand,
            appeng.api.storage.MEStorage storage, PlayerSource src, String fluidId)
    {
        try
        {
            var fid = ResourceLocation.tryParse(fluidId);
            if (fid == null)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.unsupported_target"), true);
                return InteractionResult.FAIL;
            }

            var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(fid);
            if (fluid == null || fluid == Fluids.EMPTY)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.unsupported_target"), true);
                return InteractionResult.FAIL;
            }

            var aeFluidKey = AEFluidKey.of(fluid);
            return handleFluidPlacement(context, player, wand, storage, src, aeFluidKey);
        }
        catch (Exception e)
        {
            LOGGER.warn("Error resolving fluid {}", fluidId, e);
            player.displayClientMessage(Component.translatable("message.godofthings.mept.unsupported_target"), true);
            return InteractionResult.FAIL;
        }
    }

    private boolean tryPlaceFluid(Level level, BlockPos pos, net.minecraft.world.level.material.Fluid fluid)
    {
        var stateAtPos = level.getBlockState(pos);
        boolean isFlowing = fluid instanceof FlowingFluid;
        var legacyBlock = fluid.defaultFluidState().createLegacyBlock();

        if (!isFlowing || stateAtPos == legacyBlock)
        {
            return false;
        }

        boolean canPlace = stateAtPos.isAir() || stateAtPos.canBeReplaced(fluid)
                || (stateAtPos.getBlock() instanceof LiquidBlockContainer lbc
                        && lbc.canPlaceLiquid(null, level, pos, stateAtPos, fluid));

        if (!canPlace)
        {
            return false;
        }

        if (level.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER))
        {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
            return true;
        }

        if (stateAtPos.getBlock() instanceof LiquidBlockContainer lbc && fluid == Fluids.WATER)
        {
            return lbc.placeLiquid(level, pos, stateAtPos, ((FlowingFluid) fluid).getSource(false));
        }

        if (stateAtPos.canBeReplaced(fluid) && !stateAtPos.liquid())
        {
            level.destroyBlock(pos, true);
        }
        return level.setBlock(pos, legacyBlock, Block.UPDATE_ALL_IMMEDIATE);
    }

    private BlockPlaceContext createBlockPlaceContext(UseOnContext context, Player player, ItemStack placeStack)
    {
        return new BlockPlaceContext(
                context.getLevel(), player, InteractionHand.MAIN_HAND, placeStack,
                new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                        context.getClickedPos(), context.isInside()));
    }

    private boolean tryPlaceBlock(Player player, ItemStack placeStack,
            net.minecraft.world.item.BlockItem blockItem, BlockPlaceContext placeContext)
    {
        ItemStack origMain = player.getMainHandItem().copy();
        ItemStack origOff = player.getOffhandItem().copy();

        try
        {
            player.setItemInHand(InteractionHand.MAIN_HAND, placeStack);
            var result = blockItem.place(placeContext);
            return result.consumesAction();
        }
        finally
        {
            player.setItemInHand(InteractionHand.MAIN_HAND, origMain);
            player.setItemInHand(InteractionHand.OFF_HAND, origOff);
        }
    }

    private record PartPlacementResult(BlockPos pos, Direction side, IPart part)
    {
    }

    private PartPlacementResult tryPlacePart(UseOnContext context, Player player, Level level, ItemStack placeStack)
    {
        ItemStack origMain = player.getMainHandItem().copy();
        ItemStack origOff = player.getOffhandItem().copy();

        try
        {
            player.setItemInHand(InteractionHand.MAIN_HAND, placeStack);
            var placement = PartPlacement.getPartPlacement(player, level, placeStack,
                    context.getClickedPos(), context.getClickedFace(), context.getClickLocation());

            if (placement != null && level instanceof net.minecraft.server.level.ServerLevel)
            {
                if (!canUseAt(player, level, placement.pos(), placement.side(), placeStack))
                {
                    return null;
                }
                @SuppressWarnings("unchecked")
                var partItem = (IPartItem<IPart>) placeStack.getItem();
                var part = PartPlacement.placePart(player, level, partItem,
                        placeStack.getComponents(), placement.pos(), placement.side());
                if (part != null)
                {
                    return new PartPlacementResult(placement.pos(), placement.side(), part);
                }
            }
        }
        catch (Throwable t)
        {
            LOGGER.warn("Exception during part placement", t);
        }
        finally
        {
            player.setItemInHand(InteractionHand.MAIN_HAND, origMain);
            player.setItemInHand(InteractionHand.OFF_HAND, origOff);
        }
        return null;
    }

    private boolean tryPlaceFacade(UseOnContext context, Player player, Level level, ItemStack placeStack)
    {
        try
        {
            if (!canUseAt(player, level, context.getClickedPos(), context.getClickedFace(), placeStack))
            {
                return false;
            }
            var facadeItem = (appeng.api.implementations.items.IFacadeItem) placeStack.getItem();
            var facade = facadeItem.createPartFromItemStack(placeStack, context.getClickedFace());
            if (facade == null)
            {
                return false;
            }

            var host = PartHelper.getPartHost(level, context.getClickedPos());
            if (host == null)
            {
                return false;
            }

            if (host.getPart(null) != null && host.getFacadeContainer().canAddFacade(facade))
            {
                boolean added = host.getFacadeContainer().addFacade(facade);
                if (added)
                {
                    var blockState = facade.getBlockState();
                    var soundType = blockState.getSoundType();
                    level.playSound(null, context.getClickedPos(), soundType.getPlaceSound(),
                            SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
                    host.markForSave();
                    host.markForUpdate();
                    return true;
                }
            }
        }
        catch (Throwable t)
        {
            LOGGER.error("Exception during facade placement", t);
        }
        return false;
    }

    private boolean canUseAt(Player player, Level level, BlockPos pos, Direction side, ItemStack stack)
    {
        return level.mayInteract(player, pos) && player.mayUseItemAt(pos, side, stack);
    }

    private void returnToStorageOrPlayer(Player player, appeng.api.storage.MEStorage storage,
            PlayerSource src, AEKey key, long amount)
    {
        long inserted = storage.insert(key, amount, Actionable.MODULATE, src);
        long leftover = amount - inserted;
        if (leftover <= 0 || !(key instanceof AEItemKey itemKey))
        {
            return;
        }
        while (leftover > 0)
        {
            int count = (int) Math.min(leftover, itemKey.getItem().getDefaultMaxStackSize());
            ItemStack returned = itemKey.toStack(count);
            if (!player.getInventory().add(returned))
            {
                player.drop(returned, false);
            }
            leftover -= count;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        // 只在没瞄准方块时打开配置界面
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
