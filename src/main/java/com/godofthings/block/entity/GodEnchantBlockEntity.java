package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodEnchantMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 神之附魔台：单个物品槽，任何物品都能被任意附魔（无需条件、自选等级）。
 */
public class GodEnchantBlockEntity extends BlockEntity implements MenuProvider
{
    private final ItemStackHandler itemHandler = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    public GodEnchantBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_ENCHANT_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler()
    {
        return itemHandler;
    }

    // ---- capability 注册 ----
    // 1.21.1：BE 不再覆盖 getCapability/LazyOptional，能力经 RegisterCapabilitiesEvent（MOD 总线）集中注册

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistrar
    {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_ENCHANT_BE.get(),
                    (be, side) -> be.getItemHandler());
        }
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_enchant");
    }

    // ---- NBT：保存物品槽（1.20.5+ 需 HolderLookup.Provider） ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        // 天神附魔方块 → 选择附魔默认最高等级
        boolean heavenly = this.getBlockState().is(com.godofthings.Godofthings.GOD_HEAVEN_ENCHANT.get());
        return new GodEnchantMenu(containerId, inventory, this, heavenly);
    }
}
