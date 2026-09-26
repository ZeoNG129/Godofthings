package com.godofthings.ae2;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
import com.godofthings.Godofthings;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * AE2 能力注册集中入口。
 * <p>
 * <b>本类硬引用 appeng 类型，只允许在 AE2 已安装时经 {@link AeSoftDepend#invokeStatic} 反射加载。</b>
 * 原先这些注册散落在 7 个方块实体的 CapabilityRegistration 内部类里，导致那些内部类的
 * 宿主类（也就是 BE 本身）一旦加载就要求 appeng 在 classpath 上。
 * <p>
 * 注意 {@code AECapabilities.IN_WORLD_GRID_NODE_HOST} 的上下文类型是 {@code Void}（不是 Direction）。
 */
public final class AeRegistration
{
    private AeRegistration() {}

    /** 反射入口（签名必须是对象数组可描述的类型，故此处只用 RegisterCapabilitiesEvent）。 */
    public static void register(RegisterCapabilitiesEvent event)
    {
        register(event, Godofthings.GOD_FURNACE_BE.get());
        register(event, Godofthings.GOD_MINER_BE.get());
        register(event, Godofthings.GOD_RESOURCE_BE.get());
        register(event, Godofthings.GOD_DROP_BE.get());
        register(event, Godofthings.GOD_CRAFT_BE.get());
        register(event, Godofthings.GOD_SLAUGHTER_BE.get());
        register(event, Godofthings.GOD_ABSORBER_BE.get());
    }

    private static <BE extends BlockEntity> void register(RegisterCapabilitiesEvent event, BlockEntityType<BE> type)
    {
        // AE 版实例才是 IInWorldGridNodeHost（基础版在 AE2 加载失败回退时不是），故用 instanceof 兜底
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type,
                (BE be, Void side) -> be instanceof IInWorldGridNodeHost host ? host : null);
    }
}
