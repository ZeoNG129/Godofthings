package com.godofthings.belt.belt;

import com.godofthings.Godofthings;
import com.godofthings.belt.common.Screens;
import com.godofthings.belt.slot.BeltAttachment;
import com.godofthings.belt.slot.IBeltSlotItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ComponentItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * 神之皮带（移植自 ToolBelt 2.2.10 的 {@code dev.gigaherz.toolbelt.belt.ToolBeltItem}，BSD-3-Clause）。
 *
 * <p>[本地改动] <b>默认满级</b>：上游皮带从 2 格起步，靠「皮带包 + 缝纫 / 铁砧升级」逐级扩容
 * （{@code belt_upgrade_*_via_sewing} 共 7 级，2 → 9 格）。本模组保留<b>上限</b>：
 * {@link #getSlotsCount(ItemStack)} 恒返回 {@link #MAX_SLOTS}（9 格），
 * 皮带包与缝纫升级整套不再移植，因此也不再有 {@code god_belt_size} 数据组件。</p>
 */
public class ToolBeltItem extends Item implements IBeltSlotItem
{
    /** 皮带槽位数（上游 2..9 的上限，本模组恒取上限）。 */
    public static final int MAX_SLOTS = 16;

    public static void register(RegisterCapabilitiesEvent event)
    {
        event.registerItem(
                Capabilities.ItemHandler.ITEM,
                (stack, context) -> getComponentItemHandler(stack),
                Godofthings.GOD_BELT
        );
        event.registerItem(
                IBeltSlotItem.CAPABILITY,
                (stack, context) -> (IBeltSlotItem) stack.getItem(),
                Godofthings.GOD_BELT
        );
    }

    public static @NotNull ComponentItemHandler getComponentItemHandler(ItemStack stack)
    {
        return new ComponentItemHandler(stack, DataComponents.CONTAINER, MAX_SLOTS);
    }

    private static int getSlotFor(Inventory inv, ItemStack stack)
    {
        if (inv.getSelected() == stack)
            return inv.selected;

        for (int i = 0; i < inv.items.size(); ++i)
        {
            ItemStack invStack = inv.items.get(i);
            if (invStack == stack)
            {
                return i;
            }
        }

        // Couldn't find the exact instance, can not ensure we have the right slot.
        return -1;
    }

    /** [本地改动] 满级改造：读等级恒返回上限，写入等级的地方不再存在（没有可升级状态）。 */
    public static int getSlotsCount(ItemStack stack)
    {
        return MAX_SLOTS;
    }

    // ----------- Begin implementation ----------

    public ToolBeltItem(Properties properties)
    {
        super(properties);
    }

    private InteractionResult openBeltScreen(@Nullable Player player, ItemStack stack, Level world)
    {
        int slot = player != null ? getSlotFor(player.getInventory(), stack) : -1;
        if (slot == -1)
            return InteractionResult.FAIL;

        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            Screens.openBeltScreen(serverPlayer, slot);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        if (context.getHand() != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;

        return openBeltScreen(context.getPlayer(), context.getItemInHand(), context.getLevel());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND)
            return new InteractionResultHolder<>(InteractionResult.PASS, stack);
        InteractionResult result = openBeltScreen(player, stack, world);
        return new InteractionResultHolder<>(result, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn)
    {
        // [本地改动] 上游是「升级进度 %s/%s」，满级后直接报槽位数
        tooltip.add(Component.translatable("tooltip.godofthings.god_belt", getSlotsCount(stack)));
    }

    @Override
    public void onWornTick(ItemStack itemstack, BeltAttachment slot)
    {
        tickAllSlots(itemstack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected)
    {
        if (entityIn instanceof LivingEntity)
        {
            tickAllSlots(stack);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return !ItemStack.isSameItem(oldStack, newStack); // super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    private void tickAllSlots(ItemStack source)
    {
        var inventory = Objects.requireNonNull(source.getCapability(Capabilities.ItemHandler.ITEM), "No inventory!");

        for (int i = 0; i < inventory.getSlots(); i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty())
                return;
            var extItem = stack.getCapability(IItemInBelt.CAPABILITY);
            if (extItem != null)
            {
                extItem.onWornTick(stack, source);
            }
        }
    }
}
