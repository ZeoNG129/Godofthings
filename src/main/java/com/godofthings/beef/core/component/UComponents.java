package com.godofthings.beef.core.component;

import com.mojang.serialization.Codec;
import com.godofthings.beef.UselessMod;
import com.godofthings.beef.api.enums.tool.ToolTypeMode;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

/**
 * 本模组（beef 子系统）的数据组件注册表。
 *
 * <p><b>本次死代码清理</b>：牛排工具框架已整套删除，原先只为它服务的那一批组件
 * （附魔模式 / 扳手标签 / 建筑手杖 / 耕地模式 / 顺手收菜 / 剪刀 / 打火石 / 自动熔炼 /
 * 增强连锁 / 强制挖掘 / AE 存储优先 / 匠心仪式挎包 / 强制击杀 / 时间加速 / 生物捕捉 /
 * 精魂 / 觉醒粉 / 斩首 / 传送 / 范围伤害 / 范围磁力 / 催熟 / 连点 / 强制生长）连同
 * 它们的注册项一并移除：清理后它们在 {@code src/main/java} 里已无任何引用。</p>
 *
 * <p>保留的 5 项都还有活引用：{@link #CurrentToolTypeComponent} 与两项玩家保护开关由
 * {@code com.godofthings.beef.utils.UselessItemUtils}（omnitools 扳手目标工具判定 / 玩家保护）
 * 使用；{@link #AeNetworkConnectComponent} 与 {@link #WIRELESS_LINK_TARGET} 由 AE 连接
 * 子系统（{@code EventHandler} / {@code AeDeviceLinker} / {@code AE 连接预览包}）使用。</p>
 */
public final class UComponents {
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, UselessMod.MODID);

    /**
     * 当前工具类型组件（CurrentToolType）
     * 用于在物品上存储当前选中的工具类型（枚举类型 ToolTypeMode）
     * 例如多功能工具在不同模式（镐、斧、铲等）之间切换时使用
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolTypeMode>> CurrentToolTypeComponent =
            register("tool_type", builder ->
                    builder.persistent(Codec.STRING.xmap(ToolTypeMode::valueOf, Enum::name))
                            .networkSynchronized(StreamCodec.of(
                                    FriendlyByteBuf::writeEnum,
                                    buf -> buf.readEnum(ToolTypeMode.class)
                            ))
            );

    /**
     * AE 连接模式组件（AeNetworkConnect）
     * 开启后右键一台「拥有 AE 网格节点」的机器，把它的节点接入链接目标（无线访问点）所在的那张网。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> AeNetworkConnectComponent =
            register("ae_network_connect", builder ->
                    builder.persistent(Codec.BOOL)
                           .networkSynchronized(StreamCodec.of(
                                   FriendlyByteBuf::writeBoolean,
                                   FriendlyByteBuf::readBoolean
                           ))
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BeefInvulnerabilityEnabledComponent =
            register("beef_invulnerability_enabled", builder ->
                    builder.persistent(Codec.BOOL)
                           .networkSynchronized(StreamCodec.of(
                                   FriendlyByteBuf::writeBoolean,
                                   FriendlyByteBuf::readBoolean
                           ))
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BeefAdvancedStealthEnabledComponent =
            register("beef_advanced_stealth_enabled", builder ->
                    builder.persistent(Codec.BOOL)
                           .networkSynchronized(StreamCodec.of(
                                   FriendlyByteBuf::writeBoolean,
                                   FriendlyByteBuf::readBoolean
                           ))
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> WIRELESS_LINK_TARGET = register(
            "wireless_link_target",
            builder ->
                    builder.persistent(GlobalPos.CODEC)
                           .networkSynchronized(GlobalPos.STREAM_CODEC)
    );

    // 私有构造器，防止外部实例化（该类仅用于注册静态组件）
    private UComponents() {}

    /**
     * 通用注册方法
     *
     * @param name            组件的注册名称（资源位置路径部分）
     * @param builderOperator 对 DataComponentType.Builder 的操作函数
     * @param <T>             组件存储的数据类型
     * @return DeferredHolder，用于延迟获取已注册的 DataComponentType
     */
    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        // 通过 DeferredRegister 注册，实际构建在模组加载时进行
        return DATA_COMPONENTS.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    /**
     * 初始化方法：在模组事件总线上注册 DeferredRegister
     * 通常在模组主类的构造函数中调用：UComponents.init(modEventBus);
     * @param modEventBus 模组的事件总线（通常是 MOD 事件总线）
     */
    public static void init(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
