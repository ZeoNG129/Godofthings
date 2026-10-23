package com.godofthings.infinityprovider.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.style.ScreenStyle;

/** 三档大容量供应器的具体屏幕（1024 / 无限共用翻页基类）。 */
public final class InfProviderScreens {

    private InfProviderScreens() {
    }

    public static class S32 extends InfProviderPagedScreen<com.godofthings.infinityprovider.InfProviderMenu.Provider32Menu> {
        public S32(com.godofthings.infinityprovider.InfProviderMenu.Provider32Menu menu,
                Inventory playerInventory, Component title, ScreenStyle style) {
            super(menu, playerInventory, title, style, 32);
        }
    }

    public static class S1024 extends InfProviderPagedScreen<com.godofthings.infinityprovider.InfProviderMenu.Provider1024Menu> {
        public S1024(com.godofthings.infinityprovider.InfProviderMenu.Provider1024Menu menu,
                Inventory playerInventory, Component title, ScreenStyle style) {
            super(menu, playerInventory, title, style, 1024);
        }
    }

    public static class SInf extends InfProviderPagedScreen<com.godofthings.infinityprovider.InfProviderMenu.ProviderInfMenu> {
        public SInf(com.godofthings.infinityprovider.InfProviderMenu.ProviderInfMenu menu,
                Inventory playerInventory, Component title, ScreenStyle style) {
            super(menu, playerInventory, title, style,
                    com.godofthings.infinityprovider.InfProviderBlockEntity.INFINITY_SLOTS);
        }
    }
}
