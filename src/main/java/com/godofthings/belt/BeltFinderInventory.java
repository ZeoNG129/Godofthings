package com.godofthings.belt;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 保底查找器：皮带不在皮带槽里时，直接在玩家物品栏（主背包 / 快捷栏 / 副手）里找一条皮带。
 *
 * <p>[本地改动] 上游这里对应的是 {@code BeltFinderCurios}（Curios 饰品栏查找器）。Curios 在本项目里
 * 不是编译期依赖，按约定「可选依赖不硬绑」改用物品栏保底：<b>不装 Curios 也能用轮盘</b>，
 * 装了也只是多一条查找路径（本类与 Curios 无关）。注册顺序保证皮带槽优先。</p>
 */
public class BeltFinderInventory extends BeltFinder
{
    public static final String FINDER_ID = "inventory";

    public static void initInventory()
    {
        BeltFinder.addFinder(new BeltFinderInventory());
    }

    @Override
    public String getName()
    {
        return FINDER_ID;
    }

    @Override
    public Optional<? extends BeltGetter> findStack(LivingEntity entity, boolean allowCosmetic)
    {
        if (!(entity instanceof Player player))
            return Optional.empty();

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            if (inventory.getItem(i).getItem() instanceof ToolBeltItem)
            {
                return Optional.of(new InventoryBeltGetter(player, i));
            }
        }
        return Optional.empty();
    }

    private static class InventoryBeltGetter implements BeltGetter
    {
        private final Player player;
        private final int slot;

        private InventoryBeltGetter(Player player, int slot)
        {
            this.player = player;
            this.slot = slot;
        }

        @Override
        public ItemStack getBelt()
        {
            return player.getInventory().getItem(slot);
        }

        @Override
        public void setBelt(ItemStack stack)
        {
            player.getInventory().setItem(slot, stack);
        }

        @Override
        public void syncToClients()
        {
            // 无需额外同步：皮带本身就在玩家物品栏里，原版物品栏同步会带上它的数据组件
        }
    }
}
