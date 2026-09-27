package com.godofthings.beef.stretcher.init;

import com.godofthings.beef.stretcher.UselessStretcherMod;
import com.godofthings.beef.stretcher.network.Network;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * 荒辰移晷之杖子系统的注册入口。
 *
 * <p>对应上游 {@code UselessStretcherMod} 主类构造器里那几行接线，只保留杖相关的部分
 * （上游还注册万象模具方块/方块实体/菜单与维度方块，那些属于未移植的子系统）。
 * 由 {@link com.godofthings.Godofthings} 的构造器调用。</p>
 */
public final class StretcherRegistration {

    private StretcherRegistration() {
    }

    public static void register(IEventBus modBus, ModContainer container) {
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        StretcherComponents.init(modBus);
        modBus.addListener(Network::register);
        modBus.addListener(ModItemDefaults::modifyDefaultComponents);
        // 上游配置文件名默认 useless_stretcher-common.toml，这里显式命名避免与既有配置重名。
        container.registerConfig(ModConfig.Type.COMMON, com.godofthings.beef.stretcher.config.StretcherConfig.COMMON_SPEC,
                "godofthings-staff-common.toml");
    }

    /** 上游 {@code UselessStretcherMod.MODID} 的等价引用点，便于其它类统一取用。 */
    public static String modId() {
        return UselessStretcherMod.MODID;
    }
}
