package com.godofthings.doll.compat;

import com.godofthings.doll.event.TwoICarriedCompanionship;
import com.godofthings.doll.init.DollComponents;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.item.TwoIDollItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Curios（饰品栏）兼容 —— <b>纯反射，不产生编译期依赖</b>。
 *
 * <p>这条约束来自本仓库的既有规则：Curios 是可选依赖，一旦在代码里 {@code import}
 * 它的类，没装 Curios 的整合包会在<b>类加载阶段</b>就 {@code NoClassDefFoundError}。
 * 所以这里只在 {@link #isLoaded()} 为真时才 {@code Class.forName}，方法句柄解析一次后
 * 缓存进静态字段，之后每次调用都是直接 {@code invoke}。</p>
 *
 * <p><b>反射链路</b>（Curios 9.5.1 / NeoForge 1.21.1）：</p>
 * <ol>
 *   <li>{@code CuriosApi#getCuriosInventory(LivingEntity)} → {@code Optional<ICuriosItemHandler>}</li>
 *   <li>{@code ICuriosItemHandler#getCurios()} → {@code Map<String, ICurioStacksHandler>}</li>
 *   <li>{@code ICurioStacksHandler#getStacks()} → {@code IDynamicStackHandler}</li>
 *   <li>{@code IDynamicStackHandler#getSlots()} / {@code getStackInSlot(int)}</li>
 *   <li>{@code ICuriosItemHandler#isSlotActive(String, int)}</li>
 * </ol>
 *
 * <p><b>降级</b>：没装 Curios、或任何一步反射失败，一律静默返回 —— 玩偶仍然可以
 * 手持 / 头顶 / 放出，只是少了饰品栏那一条路径。</p>
 */
public final class TwoICuriosCompat
{
    private static final Logger LOGGER = LoggerFactory.getLogger("godofthings-doll-curios");
    private static final String MOD_ID = "curios";

    /** 反射句柄（解析一次后缓存；null = 还没解析或解析失败） */
    private static Method getCuriosInventory;
    private static Method handlerGetCurios;
    private static Method handlerIsSlotActive;
    private static Method stacksHandlerGetStacks;
    private static Method dynamicGetSlots;
    private static Method dynamicGetStackInSlot;
    private static boolean resolved;
    private static boolean available;

    private TwoICuriosCompat()
    {
    }

    /** Curios 是否装了（只查 ModList，不碰它的类） */
    public static boolean isLoaded()
    {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    /**
     * 注册玩偶为饰品。
     *
     * <p>原实现调 {@code CuriosApi.registerCurio(item, ICurioItem)} 并挂一个
     * {@code CurioCanEquipEvent} 监听器。改成反射后只做「能装进任意饰品槽」这一件事：
     * 玩偶物品本身不实现 {@code ICurioItem}，靠 Curios 的默认行为 + 我们的
     * {@code mayWear} 归属校验拦截。归属校验在 {@code onEquip} 路径里由
     * {@link TwoIDollItem#claimOrCheckOwner} 兜底。</p>
     */
    public static void register()
    {
        if (!isLoaded())
        {
            LOGGER.debug("Curios 未安装，玩偶饰品栏集成跳过");
            return;
        }
        LOGGER.debug("Curios 已安装，玩偶可使用饰品栏（反射模式）");
    }

    /** 这只玩偶是否允许由该玩家佩戴（主人校验） */
    public static boolean mayWear(Player player, ItemStack stack)
    {
        if (player == null || stack == null || stack.isEmpty())
        {
            return false;
        }
        UUID owner = stack.get(DollComponents.TWO_I_OWNER.get());
        return owner == null || owner.equals(player.getUUID());
    }

    /**
     * 扫描玩家饰品栏，把找到的玩偶交给好感度采样。
     *
     * <p>这是「放在饰品栏里的玩偶也参与陪伴度累计」的唯一入口，所以必须能拿到
     * 每一格的物品。反射失败时直接返回，不影响主循环。</p>
     */
    public static void sampleEquipped(ServerPlayer player, long time)
    {
        if (!resolve())
        {
            return;
        }
        try
        {
            Object optional = getCuriosInventory.invoke(null, player);
            if (!(optional instanceof Optional<?> opt) || opt.isEmpty())
            {
                return;
            }
            Object handler = opt.get();
            Object curios = handlerGetCurios.invoke(handler);
            if (!(curios instanceof Map<?, ?> map))
            {
                return;
            }
            for (Map.Entry<?, ?> entry : map.entrySet())
            {
                if (!(entry.getKey() instanceof String slotId))
                {
                    continue;
                }
                Object stacksHandler = entry.getValue();
                if (stacksHandler == null)
                {
                    continue;
                }
                Object stacks = stacksHandlerGetStacks.invoke(stacksHandler);
                if (stacks == null)
                {
                    continue;
                }
                int slots = (int) dynamicGetSlots.invoke(stacks);
                for (int i = 0; i < slots; i++)
                {
                    Object active = handlerIsSlotActive.invoke(handler, slotId, i);
                    if (!(active instanceof Boolean on) || !on)
                    {
                        continue;
                    }
                    Object stack = dynamicGetStackInSlot.invoke(stacks, i);
                    if (stack instanceof ItemStack itemStack)
                    {
                        TwoICarriedCompanionship.sampleCarried(player, itemStack, time);
                    }
                }
            }
        }
        catch (Throwable t)
        {
            // 反射链任何一环失败都只记一次，之后不再尝试，避免每 tick 刷日志
            available = false;
            LOGGER.debug("Curios 反射链路失败，玩偶饰品栏集成停用: {}", t.toString());
        }
    }

    /** 解析并缓存全部反射句柄；返回是否可用 */
    private static boolean resolve()
    {
        if (resolved)
        {
            return available;
        }
        resolved = true;
        if (!isLoaded())
        {
            available = false;
            return false;
        }
        try
        {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Class<?> itemHandler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            Class<?> stacksHandler = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            Class<?> dynamic = Class.forName("top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler");

            getCuriosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
            handlerGetCurios = itemHandler.getMethod("getCurios");
            handlerIsSlotActive = itemHandler.getMethod("isSlotActive", String.class, int.class);
            stacksHandlerGetStacks = stacksHandler.getMethod("getStacks");
            dynamicGetSlots = dynamic.getMethod("getSlots");
            dynamicGetStackInSlot = dynamic.getMethod("getStackInSlot", int.class);
            available = true;
        }
        catch (Throwable t)
        {
            available = false;
            LOGGER.debug("Curios 反射解析失败，玩偶饰品栏集成停用: {}", t.toString());
        }
        return available;
    }
}
