package com.godofthings.infinityprovider.client;

import com.godofthings.infinityprovider.InfProviderMenu;

import appeng.api.parts.PartModels;
import appeng.client.gui.style.ScreenStyle;
import appeng.init.client.InitScreens;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 无限样板供应器的客户端注册（移植自上游 ClientScreenRegister + ClientPartModelRegister）：
 * 四个菜单屏幕 + part 底板模型。
 *
 * <p>AE2 的屏幕样式加载器（StyleManager / Blitter）把 JSON 和贴图都硬编码解析到
 * {@code ae2:} 命名空间，因此 screen json 必须放在 {@code assets/ae2/screens/}、
 * 背景贴图放在 {@code assets/ae2/textures/gui/}（上游亦如此）。</p>
 */
@EventBusSubscriber(modid = "godofthings", value = Dist.CLIENT)
public final class InfProviderClientSetup {

    private static boolean partModelsRegistered = false;

    private InfProviderClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, InfProviderMenu.Provider1Menu.TYPE,
                InfProviderClientSetup::provider1Screen,
                "/screens/_1slots_pattern_provider.json");
        InitScreens.register(event, InfProviderMenu.Provider32Menu.TYPE,
                InfProviderScreens.S32::new, "/screens/_32slots_pattern_provider.json");
        InitScreens.register(event, InfProviderMenu.Provider1024Menu.TYPE,
                InfProviderScreens.S1024::new, "/screens/_1024slots_pattern_provider.json");
        InitScreens.register(event, InfProviderMenu.ProviderInfMenu.TYPE,
                InfProviderScreens.SInf::new, "/screens/infinity_pattern_provider.json");
    }

    /** 1 槽版直接复用 AE2 样板供应器屏幕；具名工厂避免 register 重载的 lambda 推断歧义。 */
    private static appeng.client.gui.implementations.PatternProviderScreen<InfProviderMenu.Provider1Menu> provider1Screen(
            InfProviderMenu.Provider1Menu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        return new appeng.client.gui.implementations.PatternProviderScreen<>(menu, playerInventory, title, style);
    }

    @SubscribeEvent
    public static void registerPartModels(RegisterEvent event) {
        if (!partModelsRegistered) {
            PartModels.registerModels(new net.minecraft.resources.ResourceLocation[] {
                    com.godofthings.infinityprovider.InfProviderPart.Provider1Part.BASE,
                    com.godofthings.infinityprovider.InfProviderPart.Provider32Part.BASE,
                    com.godofthings.infinityprovider.InfProviderPart.Provider1024Part.BASE,
                    com.godofthings.infinityprovider.InfProviderPart.ProviderInfPart.BASE,
            });
            partModelsRegistered = true;
        }
    }
}
