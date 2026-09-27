package com.godofthings.beef.init;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.content.menus.ChainGroupMenu;
import com.godofthings.beef.content.menus.DimensionConfigMenu;
import com.godofthings.beef.content.menus.StaffLinkMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 造化杖自带的两个界面菜单。
 * <p>照抄上游 {@code com.sorrowmist.useless.init.ModMenuType} 中与工具相关的两项，
 * 上游的合金炉/发电机等机器菜单属于机器子系统，未随本次照抄带入。</p>
 */
public final class ModMenuType {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, UselessMod.MODID);

    public static final Supplier<MenuType<StaffLinkMenu>> STAFF_LINK_MENU =
            MENU_TYPES.register("staff_link_menu",
                    () -> IMenuTypeExtension.create(StaffLinkMenu::new));

    /**
     * 连锁等价组界面用的空菜单：只为让界面继承 {@code AbstractContainerScreen}
     * （JEI / EMI 的原料侧栏只画在容器界面旁边），不承载任何数据、也不下发。
     */
    public static final Supplier<MenuType<ChainGroupMenu>> CHAIN_GROUP_MENU =
            MENU_TYPES.register("chain_group_menu",
                    () -> IMenuTypeExtension.create(ChainGroupMenu::new));

    /** 无用维度配置界面（潜行右键传送方块打开）。照抄上游 ModMenuType 的同名项。 */
    public static final Supplier<MenuType<DimensionConfigMenu>> DIMENSION_CONFIG_MENU =
            MENU_TYPES.register("dimension_config_menu",
                    () -> IMenuTypeExtension.create(DimensionConfigMenu::new));

    private ModMenuType() {}

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
