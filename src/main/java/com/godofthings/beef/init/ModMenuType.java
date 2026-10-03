package com.godofthings.beef.init;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.content.menus.DimensionConfigMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 无用维度配置界面的菜单。
 *
 * <p>照抄上游 {@code com.sorrowmist.useless.init.ModMenuType} 中与本模组已移植子系统相关的项；
 * 上游的合金炉 / 发电机等机器菜单属于机器子系统，未随本次照抄带入。</p>
 *
 * <p><b>本次死代码清理</b>：牛排工具框架已整套删除，原先随工具带进来的
 * {@code chain_group_menu}（连锁等价组界面，只为让界面继承 {@code AbstractContainerScreen}
 * 以便 JEI / EMI 在侧栏画原料）已移除。</p>
 */
public final class ModMenuType {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, UselessMod.MODID);

    /** 无用维度配置界面（潜行右键传送方块打开）。照抄上游 ModMenuType 的同名项。 */
    public static final Supplier<MenuType<DimensionConfigMenu>> DIMENSION_CONFIG_MENU =
            MENU_TYPES.register("dimension_config_menu",
                    () -> IMenuTypeExtension.create(DimensionConfigMenu::new));

    private ModMenuType() {}

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
