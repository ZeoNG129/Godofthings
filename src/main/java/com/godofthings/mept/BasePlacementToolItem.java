package com.godofthings.mept;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.core.localization.Tooltips;
import appeng.items.AEBaseItem;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.util.Platform;

/**
 * 放置工具基类（照抄上游 BasePlacementToolItem）。
 * <p>
 * [本地改动] 不继承 AEBasePoweredItem（按主人要求做成不消耗能源版本），
 * 改为继承普通 AEBaseItem：保留 ME 无线访问点绑定与网络查询能力，
 * 去掉电量存储/电量检查/扣电（usePower / hasPower 恒真）。
 * 其余（链接状态提示、getLinkedGrid 查询、LinkableHandler）与上游一致。
 */
public abstract class BasePlacementToolItem extends AEBaseItem
{
    public static final IGridLinkableHandler LINKABLE_HANDLER = new LinkableHandler();

    public BasePlacementToolItem(Item.Properties props)
    {
        super(props);
    }

    /**
     * 在玩家手上找指定类型的工具（主手优先，副手兜底）。照抄上游。
     */
    public static ItemStack findHeldTool(Player player, Class<?> toolClass)
    {
        if (player == null)
        {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getMainHandItem();
        if (toolClass.isInstance(main.getItem()))
        {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (toolClass.isInstance(off.getItem()))
        {
            return off;
        }
        return ItemStack.EMPTY;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
            TooltipFlag advancedTooltips)
    {
        super.appendHoverText(stack, context, lines, advancedTooltips);
        if (getLinkedPosition(stack) == null)
        {
            lines.add(Tooltips.of(GuiText.Unlinked, Tooltips.RED));
        }
        else
        {
            lines.add(Tooltips.of(GuiText.Linked, Tooltips.GREEN));
        }
    }

    /** 工具绑定的无线访问点位置。照抄上游。 */
    @Nullable
    public GlobalPos getLinkedPosition(ItemStack item)
    {
        return item.get(MeptDataComponents.LINKED_POSITION.get());
    }

    /** 工具绑定的 AE 网络；未绑定或不可用时返回 null 并向玩家提示。照抄上游。 */
    @Nullable
    public IGrid getLinkedGrid(ItemStack item, net.minecraft.world.level.Level level, @Nullable Player sendMessagesTo)
    {
        if (!(level instanceof ServerLevel serverLevel))
        {
            return null;
        }

        var pos = getLinkedPosition(item);
        if (pos == null)
        {
            if (sendMessagesTo != null)
            {
                sendMessagesTo.displayClientMessage(PlayerMessages.DeviceNotLinked.text(), true);
            }
            return null;
        }

        var linkedLevel = serverLevel.getServer().getLevel(pos.dimension());
        if (linkedLevel == null)
        {
            if (sendMessagesTo != null)
            {
                sendMessagesTo.displayClientMessage(PlayerMessages.LinkedNetworkNotFound.text(), true);
            }
            return null;
        }

        var be = Platform.getTickingBlockEntity(linkedLevel, pos.pos());
        if (!(be instanceof IWirelessAccessPoint accessPoint))
        {
            if (sendMessagesTo != null)
            {
                sendMessagesTo.displayClientMessage(PlayerMessages.LinkedNetworkNotFound.text(), true);
            }
            return null;
        }

        var grid = accessPoint.getGrid();
        if (grid == null)
        {
            if (sendMessagesTo != null)
            {
                sendMessagesTo.displayClientMessage(PlayerMessages.LinkedNetworkNotFound.text(), true);
            }
        }
        return grid;
    }

    /** [本地改动] 不消耗能源：恒真（上游会扣 AE 电量）。 */
    public boolean usePower(Player player, double amount, ItemStack is)
    {
        return true;
    }

    /** [本地改动] 不消耗能源：恒真（上游会检查 AE 电量）。 */
    public boolean hasPower(Player player, double amt, ItemStack is)
    {
        return true;
    }

    /** 无线访问点绑定处理器。照抄上游。 */
    private static class LinkableHandler implements IGridLinkableHandler
    {
        @Override
        public boolean canLink(ItemStack stack)
        {
            return stack.getItem() instanceof BasePlacementToolItem;
        }

        @Override
        public void link(ItemStack itemStack, GlobalPos pos)
        {
            itemStack.set(MeptDataComponents.LINKED_POSITION.get(), pos);
        }

        @Override
        public void unlink(ItemStack itemStack)
        {
            itemStack.remove(MeptDataComponents.LINKED_POSITION.get());
        }
    }
}
