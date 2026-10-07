package com.godofthings.backpack;

import com.godofthings.Godofthings;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
 *
 * <p>这里还负责<b>登录时把 Curios「背部」槽开出来</b>（{@link #ensureBackSlot(ServerPlayer)}）：
 * Curios 的槽位定义 {@code data/curios/curios/slots/back.json} 只有 order / icon / validators、
 * <b>没有 size 字段</b>，所以新玩家默认 0 格、界面里根本不显示背部槽（原版要用
 * {@code /curios <玩家> add back 1} 手动开）。监听器写在类里（与 {@code ToolBeltCompat} 同一套做法：
 * {@code @EventBusSubscriber} + 静态 {@code @SubscribeEvent}），只加不改、不动别的文件。</p>
 */
// 游戏总线为默认值（Bus.GAME），省略 bus 属性；登录事件本身就是服务端事件
@EventBusSubscriber(modid = Godofthings.MODID)
public final class CuriosCompat
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Curios 的模组 id */
    private static final String CURIOS_MOD_ID = "curios";
    private static final String CURIOS_API_CLASS = "top.theillusivec4.curios.api.CuriosApi";
    private static final String CURIO_HANDLER_CLASS = "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler";
    private static final String SLOT_RESULT_CLASS = "top.theillusivec4.curios.api.SlotResult";
    private static final String SLOT_HELPER_CLASS = "top.theillusivec4.curios.api.type.util.ISlotHelper";

    /** Curios 的「背部」槽 id（对应槽位定义 {@code data/curios/curios/slots/back.json}） */
    public static final String BACK_SLOT_ID = "back";
    /** 至少给玩家开 1 格背部槽（效果等于原版指令 {@code /curios <玩家> add back 1}） */
    private static final int MIN_BACK_SLOTS = 1;

    /** 反射缓存（静态方法用 null 作接收者；解析失败就一直是 null） */
    private static Method getCuriosInventory;
    private static Method findFirstCurio;
    private static Method slotResultStack;
    /** 是否已经尝试过解析（避免每次调用都重复 Class.forName） */
    private static boolean resolved;
    /** 解析是否成功（没装 Curios / API 变了都是 false） */
    private static boolean usable;

    /** 开槽用的三个方法，单独一组缓存（与「查找背包」那组互不影响） */
    private static Method getSlotHelper;
    private static Method slotsForType;
    private static Method growSlotType;
    private static boolean slotResolved;
    private static boolean slotUsable;

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

    /**
     * 确保玩家的 Curios「背部」槽至少开 1 格（<b>幂等</b>：已经有格子的玩家原样不动）。
     *
     * <p>Curios 的 {@code data/curios/curios/slots/back.json} 没有 size 字段，新玩家默认 0 格、
     * 界面里看不到背部槽；这里走的是和原版指令 {@code /curios <玩家> add back 1} 完全相同的 API。</p>
     *
     * <p>只应在服务端调用（槽位容量是服务端权威数据，改完由 Curios 自己同步给客户端），
     * 所以参数收的是 {@link ServerPlayer}。</p>
     */
    public static void ensureBackSlot(ServerPlayer player)
    {
        if (player == null || !isLoaded())
        {
            return; // 没装 Curios：连反射都不碰
        }
        try
        {
            if (!resolveSlotApi())
            {
                return;
            }
            Object helper = getSlotHelper.invoke(null);
            if (helper == null)
            {
                return;
            }
            Object current = slotsForType.invoke(helper, player, BACK_SLOT_ID);
            int slots = current instanceof Integer value ? value : 0;
            if (slots >= MIN_BACK_SLOTS)
            {
                return; // 已经有格子了：不动（幂等，不重复加）
            }
            growSlotType.invoke(helper, BACK_SLOT_ID, MIN_BACK_SLOTS - slots, player);
        }
        catch (Throwable throwable)
        {
            // 与 findBackpackInBackSlot 同一套安全策略：任何异常都只记 debug，绝不影响登录流程
            LOGGER.debug("[godofthings] 自动开启 Curios 背部槽失败（按未安装处理）", throwable);
        }
    }

    /** 解析并缓存开槽用的三个反射方法；失败一次就记住失败，不再重试 */
    private static boolean resolveSlotApi()
    {
        if (slotResolved)
        {
            return slotUsable;
        }
        slotResolved = true;
        try
        {
            Class<?> api = Class.forName(CURIOS_API_CLASS);
            Class<?> slotHelper = Class.forName(SLOT_HELPER_CLASS);
            getSlotHelper = api.getMethod("getSlotHelper");
            slotsForType = slotHelper.getMethod("getSlotsForType", LivingEntity.class, String.class);
            growSlotType = slotHelper.getMethod("growSlotType", String.class, int.class, LivingEntity.class);
            slotUsable = true;
        }
        catch (Throwable throwable)
        {
            LOGGER.debug("[godofthings] Curios 槽位 API 反射解析失败（按未安装处理）", throwable);
            getSlotHelper = null;
            slotsForType = null;
            growSlotType = null;
            slotUsable = false;
        }
        return slotUsable;
    }

    /**
     * 玩家登录（服务端）：把背部槽开出来。
     *
     * <p>开两次是刻意的：登录事件发生在 {@code placeNewPlayer} 过程中，万一 Curios 的初始同步包
     * 已经先发完，玩家这一趟可能看不到新格子 —— 所以再延迟 1 tick 补一次。
     * {@link #ensureBackSlot(ServerPlayer)} 本身是幂等的（已经有格子就什么都不做），重复调用无副作用。</p>
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            ensureBackSlot(serverPlayer);
            MinecraftServer server = serverPlayer.getServer();
            if (server != null)
            {
                // 延迟 1 tick 再补一次（幂等）：确保 Curios 的槽位同步发生在开槽之后
                server.tell(new TickTask(server.getTickCount() + 1, () -> ensureBackSlot(serverPlayer)));
            }
        }
    }
}
