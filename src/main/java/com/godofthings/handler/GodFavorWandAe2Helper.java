package com.godofthings.handler;

import appeng.api.config.Actionable;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import com.godofthings.item.GodFavorWandItem;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import com.godofthings.item.WandModes;

/**
 * 神之工具的 AE2 集成（AE2 19.x API）。
 * - 通过 IGridLinkableHandler 支持 AE2 记忆卡的连接/断开
 * - "AE 存储优先" 模式下掉落物直接存入 ME 网络
 * 本类仅在 ModList 检测到 ae2 时才会被加载。
 */
public final class GodFavorWandAe2Helper
{
    public static final IGridLinkableHandler LINKABLE_HANDLER = new GodFavorWandAe2Helper.LinkableHandler();

    private GodFavorWandAe2Helper()
    {
    }

    @Nullable
    public static IGrid getLinkedGrid(ItemStack stack, Level level, @Nullable Player player)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            GlobalPos linkedPos = GodFavorWandItem.getLinkedPosition(stack);
            if (linkedPos == null)
            {
                return null;
            }
            ServerLevel linkedLevel = serverLevel.getServer().getLevel(linkedPos.dimension());
            if (linkedLevel == null)
            {
                return null;
            }
            return linkedLevel.getBlockEntity(linkedPos.pos()) instanceof IWirelessAccessPoint accessPoint
                    ? accessPoint.getGrid()
                    : null;
        }
        return null;
    }

    public static boolean storeItemInAENetwork(ItemStack stack, Player player, ItemStack toolStack)
    {
        if (player == null || stack.isEmpty())
        {
            return false;
        }

        ItemStack toolItem = toolStack;
        if (toolStack == null || toolStack.isEmpty())
        {
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();
            if (mainHandItem.getItem() instanceof GodFavorWandItem)
            {
                toolItem = mainHandItem;
            }
            else if (offHandItem.getItem() instanceof GodFavorWandItem)
            {
                toolItem = offHandItem;
            }
            else
            {
                return false;
            }
        }

        if (toolItem.getItem() instanceof GodFavorWandItem tool && tool.isAEStoragePriorityMode(toolItem))
        {
            try
            {
                IGrid grid = getLinkedGrid(toolItem, player.level(), player);
                if (grid == null)
                {
                    return false;
                }

                MEStorage storage = grid.getStorageService().getInventory();
                if (storage == null)
                {
                    return false;
                }

                AEItemKey aeKey = AEItemKey.of(stack);
                if (aeKey == null)
                {
                    return false;
                }

                // 1.21.1 AE2 19.x：动作源用 IActionSource.ofPlayer（原 appeng.me.helpers.PlayerSource 内部类）
                long inserted = storage.insert(aeKey, stack.getCount(), Actionable.MODULATE, IActionSource.ofPlayer(player));
                if (inserted == stack.getCount())
                {
                    return true;
                }

                if (inserted > 0L)
                {
                    stack.setCount((int) (stack.getCount() - inserted));
                    return stack.isEmpty();
                }

                return false;
            }
            catch (Exception e)
            {
                return false;
            }
        }

        return false;
    }

    private static class LinkableHandler implements IGridLinkableHandler
    {
        @Override
        public boolean canLink(ItemStack stack)
        {
            return stack.getItem() instanceof GodFavorWandItem;
        }

        @Override
        public void link(ItemStack itemStack, GlobalPos pos)
        {
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(tag ->
                    WandModes.update(itemStack, root -> root.put("accessPoint", (CompoundTag) tag)));
        }

        @Override
        public void unlink(ItemStack itemStack)
        {
            WandModes.update(itemStack, root -> root.remove("accessPoint"));
        }
    }
}
