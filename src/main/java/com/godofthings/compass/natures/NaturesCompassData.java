package com.godofthings.compass.natures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.godofthings.Godofthings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;

/**
 * 自然罗盘的注册内容与同步状态（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>上游这里是它自己的 mod 主类（{@code @Mod NaturesCompass}）。本项目<b>不建第二个 {@code @Mod}</b>，
 * 所以这里只保留「数据组件 + 权限节点 + 客户端同步状态」这三块，物品本体在
 * {@link Godofthings#NATURES_COMPASS} 里注册，网络包在 {@code network.NaturesCompassMessages} 里注册，
 * 客户端注册在 {@code client.NaturesCompassClient} 里注册，配置在 {@code Godofthings} 构造器里注册。</p>
 *
 * <p>物品身上记的东西全部走数据组件（{@code persistent} 存盘 + {@code networkSynchronized} 同步给客户端）：
 * 目标群系 id / 状态机 / 找到的坐标 / 搜索半径 / 采样数 / 是否显示坐标 / 已找到的坐标列表 / 已用次数。</p>
 */
public final class NaturesCompassData
{
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 传送权限节点（OP / 单人开作弊 / 权限插件三者任一即可，见 PlayerUtils） */
    public static final PermissionNode<Boolean> TELEPORT_PERMISSION = new PermissionNode<>(
            Godofthings.MODID, "natures.teleport", PermissionTypes.BOOLEAN, (player, playerUUID, context) -> false);

    // ---- 数据组件 ----
    // 真正的注册写在 Godofthings 自己的静态初始化里（原因见那里的注释：DeferredRegister 不能被
    // 延迟到 RegisterEvent 之后才首次加载）。这里只是别名，方便本包代码继续用短名字访问。
    /** 目标群系的资源位置字符串（如 {@code minecraft:plains}） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BIOME_ID =
            Godofthings.NATURES_COMPASS_BIOME_ID;
    /** 状态机（0 空闲 / 1 搜索中 / 2 已找到 / 3 未找到，见 CompassState） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COMPASS_STATE =
            Godofthings.NATURES_COMPASS_STATE;
    /** 找到的群系的 X 坐标 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FOUND_X =
            Godofthings.NATURES_COMPASS_FOUND_X;
    /** 找到的群系的 Z 坐标 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FOUND_Z =
            Godofthings.NATURES_COMPASS_FOUND_Z;
    /** 搜索半径（搜索过程中不断刷新，用于 HUD 显示） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SEARCH_RADIUS =
            Godofthings.NATURES_COMPASS_SEARCH_RADIUS;
    /** 已采样次数 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SAMPLES =
            Godofthings.NATURES_COMPASS_SAMPLES;
    /** HUD 上是否显示精确坐标与距离（服务端按配置写，客户端只读） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DISPLAY_COORDS =
            Godofthings.NATURES_COMPASS_DISPLAY_COORDS;
    /** 已经找到过的群系坐标（「搜索下一个」用来跳过已找到的、以及判断是否连通） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<BlockPos>>> PREV_POS =
            Godofthings.NATURES_COMPASS_PREV_POS;
    /** 已用次数（配合配置项 compassDurability 做「用几次就坏，要拿指南针修」） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DAMAGE =
            Godofthings.NATURES_COMPASS_DAMAGE;

    // ---- 客户端同步状态（SyncPacket 收到后写入，界面 / HUD 读） ----
    /** 收到过服务端同步包（界面用它决定什么时候重建列表） */
    public static boolean synced;
    /** 当前玩家能不能传送（创造 / OP / 作弊 / 权限节点） */
    public static boolean canTeleport;
    /** 「搜索下一个」最多还能点几次 */
    public static int maxNextSearches;
    /** 当前玩家是不是无限经验（创造模式） */
    public static boolean infiniteXp;
    /** 允许搜索的群系列表 */
    public static List<ResourceLocation> allowedBiomes = new ArrayList<>();
    /** 群系 → 搜索一次要扣的经验等级 */
    public static Map<ResourceLocation, Integer> xpLevelsForAllowedBiomes = new HashMap<>();
    /** 群系 → 会生成它的维度列表 */
    public static ListMultimap<ResourceLocation, ResourceLocation> dimensionKeysForAllowedBiomeKeys = ArrayListMultimap.create();

    private NaturesCompassData()
    {
    }

    /**
     * 把传送权限节点挂到 NeoForge 的权限系统上（由 {@code Godofthings#commonSetup} 挂到
     * {@code NeoForge.EVENT_BUS}；上游是在自己的 {@code @Mod} 类里用 {@code @SubscribeEvent} 收这个事件的）。
     */
    public static void registerPermissionNodes(PermissionGatherEvent.Nodes event)
    {
        event.addNodes(TELEPORT_PERMISSION);
    }

    /** 手上（或物品栏里）的这个物品栈是不是自然罗盘 */
    public static boolean isCompass(ItemStack stack)
    {
        return stack != null && !stack.isEmpty() && stack.getItem() == Godofthings.NATURES_COMPASS.get();
    }
}
