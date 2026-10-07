package com.godofthings.backpack;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Curios（饰品栏）兼容：查「背部槽里有没有神之背包」。
 *
 * <p><b>为什么用反射</b>：Curios 是可选依赖，本模组不能对它产生编译期依赖（否则没装 Curios 的整合包
 * 连类都加载不了）。这里只在 {@link #isLoaded()} 为真时才去 {@code Class.forName}，
 * 三个 {@link Method} 解析一次后缓存在静态字段里，之后每次调用都是直接 invoke。</p>
 *
 * <p><b>反射链路</b>（Curios 9.5.1 / NeoForge 1.21.1 实测签名）：</p>
 * <ol>
 *   <li>{@code top.theillusivec4.curios.api.CuriosApi#getCuriosInventory(LivingEntity)}
 *       → {@code Optional<ICuriosItemHandler>}（静态方法）</li>
 *   <li>{@code top.theillusivec4.curios.api.type.capability.ICuriosItemHandler#findFirstCurio(Predicate<ItemStack>)}
 *       → {@code Optional<SlotResult>}</li>
 *   <li>{@code top.theillusivec4.curios.api.SlotResult#stack()} → {@code ItemStack}</li>
 * </ol>
 *
 * <p><b>降级路径</b>：没装 Curios（{@link ModList} 直接判否，连类都不加载）、或上面任何一步失败
 * （类 / 方法改名、初始化报错、调用抛异常），一律吞掉并返回 {@link ItemStack#EMPTY}，
 * 绝不影响「主物品栏 / 副手」那两条既有路径。</p>
 */
public final class CuriosCompat
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Curios 的模组 id */
    private static final String CURIOS_MOD_ID = "curios";
    private static final String CURIOS_API_CLASS = "top.theillusivec4.curios.api.CuriosApi";
    private static final String CURIO_HANDLER_CLASS = "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler";
    private static final String SLOT_RESULT_CLASS = "top.theillusivec4.curios.api.SlotResult";

    /** 反射缓存（静态方法用 null 作接收者；解析失败就一直是 null） */
    private static Method getCuriosInventory;
    private static Method findFirstCurio;
    private static Method slotResultStack;
    /** 是否已经尝试过解析（避免每次调用都重复 Class.forName） */
    private static boolean resolved;
    /** 解析是否成功（没装 Curios / API 变了都是 false） */
    private static boolean usable;

    private CuriosCompat() {}

    /** 装没装 Curios（纯 {@link ModList} 查询，不触发任何 Curios 类加载） */
    public static boolean isLoaded()
    {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(CURIOS_MOD_ID);
    }

    /**
     * 找玩家饰品栏里的神之背包；没有就返回空堆叠。
     *
     * <p>Curios 的背部槽靠物品标签 {@code curios:back} 判定（槽位定义
     * {@code data/curios/curios/slots/back.json} 的 validator 是 {@code curios:tag}），
     * 本模组给神之背包挂了这个标签，所以「找得到」等于「装备在背部槽」。</p>
     */
    public static ItemStack findBackpackInBackSlot(Player player)
    {
        if (player == null || !isLoaded())
        {
            return ItemStack.EMPTY;
        }
        try
        {
            if (!resolve())
            {
                return ItemStack.EMPTY;
            }
            Object inventory = getCuriosInventory.invoke(null, player);
            if (!(inventory instanceof Optional<?> optional) || optional.isEmpty())
            {
                return ItemStack.EMPTY;
            }
            Predicate<ItemStack> isBackpack = stack -> stack.getItem() instanceof GodBackpackItem;
            Object found = findFirstCurio.invoke(optional.get(), isBackpack);
            if (!(found instanceof Optional<?> slotResult) || slotResult.isEmpty())
            {
                return ItemStack.EMPTY;
            }
            Object stack = slotResultStack.invoke(slotResult.get());
            return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        }
        catch (Throwable throwable)
        {
            // 故意连 Error 一起吞：Curios 若因自身依赖缺失在类加载阶段炸（NoClassDefFoundError /
            // ExceptionInInitializerError），也不能把「按 B 打开背包」这条链路带崩。
            LOGGER.debug("[godofthings] 查询 Curios 背部槽失败，按「没有背包」处理", throwable);
            return ItemStack.EMPTY;
        }
    }

    /** 解析并缓存三个反射方法；失败一次就记住失败，不再重试 */
    private static boolean resolve()
    {
        if (resolved)
        {
            return usable;
        }
        resolved = true;
        try
        {
            Class<?> api = Class.forName(CURIOS_API_CLASS);
            Class<?> handler = Class.forName(CURIO_HANDLER_CLASS);
            Class<?> slotResult = Class.forName(SLOT_RESULT_CLASS);
            getCuriosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
            findFirstCurio = handler.getMethod("findFirstCurio", Predicate.class);
            slotResultStack = slotResult.getMethod("stack");
            usable = true;
        }
        catch (Throwable throwable)
        {
            LOGGER.debug("[godofthings] Curios API 反射解析失败（按未安装处理）", throwable);
            getCuriosInventory = null;
            findFirstCurio = null;
            slotResultStack = null;
            usable = false;
        }
        return usable;
    }
}
