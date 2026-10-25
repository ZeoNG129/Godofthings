package com.godofthings.belt;

import com.godofthings.backpack.CuriosCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Curios 饰品栏查找器：皮带戴在饰品栏（腰带槽）里时也能被轮盘/快捷键找到。
 *
 * <p>Curios 不是本项目的编译期依赖，全部走 {@link CuriosCompat} 的反射路径；
 * 未装 Curios、或反射失败，本查找器直接返回空，由 {@link BeltFinderInventory} 保底。</p>
 *
 * <p><b>只读</b>：皮带内容存在皮带物品自己的数据组件里，不需要替换整个 ItemStack，
 * 因此不实现 {@code setBelt}（默认空实现）。</p>
 */
public class BeltFinderCurios extends BeltFinder
{
    public static final String FINDER_ID = "curios";

    private static final Predicate<ItemStack> IS_BELT = stack -> stack.getItem() instanceof ToolBeltItem;

    public static void initCurios()
    {
        BeltFinder.addFinder(new BeltFinderCurios());
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
        {
            return Optional.empty();
        }
        if (CuriosCompat.findFirstInCurios(player, IS_BELT).isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(new CuriosBeltGetter(player));
    }

    private static final class CuriosBeltGetter implements BeltGetter
    {
        private final Player player;

        private CuriosBeltGetter(Player player)
        {
            this.player = player;
        }

        @Override
        public ItemStack getBelt()
        {
            // 每次实时向 Curios 取：玩家换槽位/换皮带时立即反映，不需要缓存失效
            return CuriosCompat.findFirstInCurios(player, IS_BELT);
        }

        @Override
        public void syncToClients()
        {
            // Curios 自己会同步饰品栏内容，这里不需要额外发包
        }
    }
}
