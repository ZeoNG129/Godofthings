package com.godofthings.superfusion;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 超级融合卡的数据组件（移植自 Mekanism-Card 的 ModDataComponents）。
 * <p>
 * 只保留超级融合卡自身用得到的组件：能量、融合模式、模块清空模式、模糊目标模式。
 * 上游的「区域升级模式」属于等级安装器（本模组不移植该物品），一并去掉。
 * <p>
 * 注意：{@code DeferredRegister.register} 只能在 {@code Godofthings} 的静态初始化里调用，
 * 这里用 {@code DeferredHolder} 字段挂在本类的静态初始化上——本类只在
 * {@code Godofthings.SUPER_FUSION_ITEM} 的注册 lambda 被求值时才会加载，
 * 那时 {@code RegisterEvent} 还没触发（注册表在构造 mod 阶段就开始接收），因此安全。
 * 但为稳妥起见，实际的 {@code register(modEventBus)} 调用仍由 {@code Godofthings} 构造函数发起。
 */
public final class SuperFusionComponents
{
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, com.godofthings.Godofthings.MODID);

    /** 卡内储能（FE），上限 200000 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            DATA_COMPONENT_TYPES.register("super_fusion_energy", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .build());

    /** 融合模式序号（对应 SuperFusionItem.FusionMode） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FUSION_MODE =
            DATA_COMPONENT_TYPES.register("super_fusion_mode", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .build());

    /** 模块操作模式：true = 清空全部升级，false = 按存档配置安装 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MODULE_CLEAR_MODE =
            DATA_COMPONENT_TYPES.register("super_fusion_module_clear", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .build());

    /** 目标匹配模式：true = 模糊（不限机器种类），false = 精确 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FUZZY_TARGET_MODE =
            DATA_COMPONENT_TYPES.register("super_fusion_fuzzy_target", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .build());

    private SuperFusionComponents()
    {
    }
}
