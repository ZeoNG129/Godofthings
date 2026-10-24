package com.godofthings.superfusion;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 超级融合卡的数据组件（移植自 Mekanism-Card 的 ModDataComponents）。
 * <p>
 * 只保留超级融合卡自身用得到的组件：融合模式、模块清空模式、模糊目标模式。
 * 上游的「能量」组件已随电量需求一并删除（按主人要求融合卡不再耗电），
 * 「区域升级模式」属于等级安装器（本模组不移植该物品），也一并去掉。
 * <p>
 * 注意：{@code DeferredRegister.register} 只能在 {@code Godofthings} 自己的静态初始化里调用，
 * 但为稳妥起见，实际的 {@code register(modEventBus)} 调用仍由 {@code Godofthings} 构造函数发起
 * （仅在 Mekanism 存在时才挂载，见 {@code SuperFusionRegistry}）。
 */
public final class SuperFusionComponents
{
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, com.godofthings.Godofthings.MODID);

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
