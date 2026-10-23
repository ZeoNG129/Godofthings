package com.godofthings.infinityprovider;

import java.util.ArrayList;
import java.util.List;

import com.godofthings.Godofthings;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.slot.AppEngSlot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

/**
 * 四档供应器的菜单（移植自 InfinityPatternProvider）。
 * 1 槽版直接复用 AE2 菜单布局；大容量版额外缓存样板槽列表供屏幕翻页。
 */
public final class InfProviderMenu {

    private InfProviderMenu() {
    }

    /** 带样板槽缓存的菜单（大容量三档共用），供翻页屏幕取槽位列表。 */
    public interface PagedMenu {
        List<AppEngSlot> getPatternSlots();
    }

    public static class Provider1Menu extends PatternProviderMenu {
        public static MenuType<Provider1Menu> TYPE;

        public static void init() {
            TYPE = MenuTypeBuilder.create(Provider1Menu::new, PatternProviderLogicHost.class)
                    .buildUnregistered(menuId("_1slots_pattern_provider"));
        }

        public Provider1Menu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
            super(TYPE, id, playerInventory, host);
        }
    }

    public static class Provider32Menu extends PatternProviderMenu implements PagedMenu {
        public static MenuType<Provider32Menu> TYPE;
        private List<AppEngSlot> patternSlots;

        public static void init() {
            TYPE = MenuTypeBuilder.create(Provider32Menu::new, PatternProviderLogicHost.class)
                    .buildUnregistered(menuId("_32slots_pattern_provider"));
        }

        public Provider32Menu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
            super(TYPE, id, playerInventory, host);
            this.initializePatternSlots();
        }

        private void initializePatternSlots() {
            this.patternSlots = new ArrayList<>();
            for (Slot slot : this.getSlots(SlotSemantics.ENCODED_PATTERN)) {
                if (slot instanceof AppEngSlot appEngSlot) {
                    this.patternSlots.add(appEngSlot);
                }
            }
        }

        public List<AppEngSlot> getPatternSlots() {
            return this.patternSlots;
        }
    }

    public static class Provider1024Menu extends PatternProviderMenu implements PagedMenu {
        public static MenuType<Provider1024Menu> TYPE;
        private List<AppEngSlot> patternSlots;

        public static void init() {
            TYPE = MenuTypeBuilder.create(Provider1024Menu::new, PatternProviderLogicHost.class)
                    .buildUnregistered(menuId("_1024slots_pattern_provider"));
        }

        public Provider1024Menu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
            super(TYPE, id, playerInventory, host);
            this.initializePatternSlots();
        }

        private void initializePatternSlots() {
            this.patternSlots = new ArrayList<>();
            for (Slot slot : this.getSlots(SlotSemantics.ENCODED_PATTERN)) {
                if (slot instanceof AppEngSlot appEngSlot) {
                    this.patternSlots.add(appEngSlot);
                }
            }
        }

        public List<AppEngSlot> getPatternSlots() {
            return this.patternSlots;
        }
    }

    public static class ProviderInfMenu extends PatternProviderMenu implements PagedMenu {
        public static MenuType<ProviderInfMenu> TYPE;
        private List<AppEngSlot> patternSlots;

        public static void init() {
            TYPE = MenuTypeBuilder.create(ProviderInfMenu::new, PatternProviderLogicHost.class)
                    .buildUnregistered(menuId("infinity_pattern_provider"));
        }

        public ProviderInfMenu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
            super(TYPE, id, playerInventory, host);
            this.initializePatternSlots();
        }

        private void initializePatternSlots() {
            this.patternSlots = new ArrayList<>();
            for (Slot slot : this.getSlots(SlotSemantics.ENCODED_PATTERN)) {
                if (slot instanceof AppEngSlot appEngSlot) {
                    this.patternSlots.add(appEngSlot);
                }
            }
        }

        public List<AppEngSlot> getPatternSlots() {
            return this.patternSlots;
        }
    }

    /**
     * 菜单 id 用本模组命名空间：AE2 的 {@code MenuTypeBuilder#build(String)} 会自行以
     * {@code ae2:} 前缀注册菜单，与我们自己的 DeferredRegister 撞成「重复值」，
     * 所以这里一律走 {@code buildUnregistered} + 自家注册器。
     */
    private static ResourceLocation menuId(String path) {
        return ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, path);
    }
}
