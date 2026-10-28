package com.godofthings.infinityprovider;

import com.godofthings.Godofthings;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.PatternProviderMenu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/**
 * 四档供应器的菜单（移植自 InfinityPatternProvider + AE2LT 的增强动作）。
 * 1 槽版直接复用 AE2 菜单布局；大容量版额外缓存样板槽列表供屏幕翻页。
 *
 * <p>增强设置（无线/回收/自适应倍增/高级设置）通过宿主 {@link InfEnhancedHost}
 * 读写，GuiSync 字段把状态同步给屏幕按钮。</p>
 */
public final class InfProviderMenu {

    private InfProviderMenu() {
    }

    /** 带样板槽缓存的菜单（大容量三档共用），供翻页屏幕取槽位列表。 */
    public interface PagedMenu {
        java.util.List<appeng.menu.slot.AppEngSlot> getPatternSlots();
    }

    /** 增强设置的动作与同步（四档共用，混入各自菜单基类）。 */
    public static abstract class EnhancedActions extends PatternProviderMenu {
        // GuiSync 取高位段，避开 AE2 及常见扩展占用的低位号
        @GuiSync(22000)
        public int providerMode;
        @GuiSync(22001)
        public int returnMode;
        @GuiSync(22003)
        public int wirelessDispatchMode;
        @GuiSync(22006)
        public int wirelessSpeedMode;
        @GuiSync(22010)
        public int adaptiveBatchEnabled;
        @GuiSync(22013)
        public int filteredImport;
        @GuiSync(22011)
        public int machineParallelism = 1;
        @GuiSync(22012)
        public int connectionCount;

        protected EnhancedActions(MenuType<? extends PatternProviderMenu> menuType, int id,
                Inventory playerInventory, PatternProviderLogicHost host) {
            super(menuType, id, playerInventory, host);
            this.registerClientAction("infToggleMode", this::infToggleMode);
            this.registerClientAction("infToggleReturn", this::infToggleReturn);
            this.registerClientAction("infToggleDispatch", this::infToggleDispatch);
            this.registerClientAction("infToggleSpeed", this::infToggleSpeed);
            this.registerClientAction("infToggleAdaptive", this::infToggleAdaptive);
            this.registerClientAction("infToggleFiltered", this::infToggleFiltered);
            this.registerClientAction("infSetParallelism", Integer.class, this::infSetParallelism);
        }

        private InfEnhancedHost enhancedHost() {
            return this.getTarget() instanceof InfEnhancedHost host ? host : null;
        }

        @Override
        public void broadcastChanges() {
            var host = this.enhancedHost();
            if (host != null && this.isServerSide()) {
                var settings = host.infSettings();
                this.providerMode = settings.getProviderMode().ordinal();
                this.returnMode = settings.getReturnMode().ordinal();
                this.wirelessDispatchMode = settings.getWirelessDispatchMode().ordinal();
                this.wirelessSpeedMode = settings.getWirelessSpeedMode().ordinal();
                this.adaptiveBatchEnabled = settings.isAdaptiveBatchEnabled() ? 1 : 0;
                this.filteredImport = settings.isFilteredImport() ? 1 : 0;
                this.machineParallelism = settings.getMachineParallelism();
                this.connectionCount = host.infConnections().size();
            }
            super.broadcastChanges();
        }

        // ---- 服务端动作 ----

        private void infToggleMode() {
            var host = this.enhancedHost();
            if (host == null || !this.isServerSide()) {
                return;
            }
            var current = host.infSettings().getProviderMode();
            host.infSettings().setProviderMode(
                    current == InfProviderSettings.ProviderMode.NORMAL
                            ? InfProviderSettings.ProviderMode.WIRELESS
                            : InfProviderSettings.ProviderMode.NORMAL);
        }

        private void infToggleReturn() {
            var host = this.enhancedHost();
            if (host == null || !this.isServerSide()) {
                return;
            }
            var values = InfProviderSettings.ReturnMode.values();
            var current = host.infSettings().getReturnMode();
            host.infSettings().setReturnMode(values[(current.ordinal() + 1) % values.length]);
        }

        private void infToggleDispatch() {
            var host = this.enhancedHost();
            if (host == null || !this.isServerSide()) {
                return;
            }
            var values = InfProviderSettings.WirelessDispatchMode.values();
            var current = host.infSettings().getWirelessDispatchMode();
            host.infSettings().setWirelessDispatchMode(values[(current.ordinal() + 1) % values.length]);
        }

        private void infToggleSpeed() {
            var host = this.enhancedHost();
            if (host == null || !this.isServerSide()) {
                return;
            }
            var values = InfProviderSettings.WirelessSpeedMode.values();
            var current = host.infSettings().getWirelessSpeedMode();
            host.infSettings().setWirelessSpeedMode(values[(current.ordinal() + 1) % values.length]);
        }

        private void infToggleAdaptive() {
            var host = this.enhancedHost();
            if (host == null || !this.isServerSide()) {
                return;
            }
            host.infSettings().setAdaptiveBatchEnabled(!host.infSettings().isAdaptiveBatchEnabled());
        }

        private void infToggleFiltered() {
            var host = this.enhancedHost();
            if (host == null || !this.isServerSide()) {
                return;
            }
            host.infSettings().setFilteredImport(!host.infSettings().isFilteredImport());
        }

        private void infSetParallelism(int value) {
            var host = this.enhancedHost();
            if (host == null || !this.isServerSide()) {
                return;
            }
            host.infSettings().setMachineParallelism(Math.max(1, value));
        }

        // ---- 客户端入口 ----

        public void clientToggleMode() {
            this.sendClientAction("infToggleMode");
        }

        public void clientToggleReturn() {
            this.sendClientAction("infToggleReturn");
        }

        public void clientToggleDispatch() {
            this.sendClientAction("infToggleDispatch");
        }

        public void clientToggleSpeed() {
            this.sendClientAction("infToggleSpeed");
        }

        public void clientToggleAdaptive() {
            this.sendClientAction("infToggleAdaptive");
        }

        public void clientToggleFiltered() {
            this.sendClientAction("infToggleFiltered");
        }

        public void clientSetParallelism(int value) {
            this.sendClientAction("infSetParallelism", Math.max(1, value));
        }

        // ---- 状态查询（屏幕按钮刷新用）----

        public boolean isWirelessMode() {
            return this.providerMode == InfProviderSettings.ProviderMode.WIRELESS.ordinal();
        }

        public int getReturnModeOrdinal() {
            return this.returnMode;
        }

        public boolean isEvenDistribution() {
            return this.wirelessDispatchMode == InfProviderSettings.WirelessDispatchMode.EVEN_DISTRIBUTION.ordinal();
        }

        public boolean isFastSpeed() {
            return this.wirelessSpeedMode == InfProviderSettings.WirelessSpeedMode.FAST.ordinal();
        }

        public boolean isAdaptiveBatch() {
            return this.adaptiveBatchEnabled != 0;
        }

        public boolean isFilteredImport() {
            return this.filteredImport != 0;
        }

        public int getMachineParallelism() {
            return this.machineParallelism;
        }

        public int getConnectionCount() {
            return this.connectionCount;
        }
    }

    // ==================== 四档具体菜单 ====================

    public static class Provider1Menu extends EnhancedActions {
        public static MenuType<Provider1Menu> TYPE;

        public static void init() {
            TYPE = MenuTypeBuilder.create(Provider1Menu::new, PatternProviderLogicHost.class)
                    .buildUnregistered(menuId("_pattern_provider_1"));
        }

        public Provider1Menu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
            super(TYPE, id, playerInventory, host);
        }
    }

    public static class Provider32Menu extends EnhancedActions implements PagedMenu {
        public static MenuType<Provider32Menu> TYPE;
        private java.util.List<appeng.menu.slot.AppEngSlot> patternSlots;

        public static void init() {
            TYPE = MenuTypeBuilder.create(Provider32Menu::new, PatternProviderLogicHost.class)
                    .buildUnregistered(menuId("pattern_provider_32"));
        }

        public Provider32Menu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
            super(TYPE, id, playerInventory, host);
            this.initializePatternSlots();
        }

        private void initializePatternSlots() {
            this.patternSlots = new java.util.ArrayList<>();
            for (var slot : this.getSlots(SlotSemantics.ENCODED_PATTERN)) {
                if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                    this.patternSlots.add(appEngSlot);
                }
            }
        }

        public java.util.List<appeng.menu.slot.AppEngSlot> getPatternSlots() {
            return this.patternSlots;
        }
    }

    public static class Provider1024Menu extends EnhancedActions implements PagedMenu {
        public static MenuType<Provider1024Menu> TYPE;
        private java.util.List<appeng.menu.slot.AppEngSlot> patternSlots;

        public static void init() {
            TYPE = MenuTypeBuilder.create(Provider1024Menu::new, PatternProviderLogicHost.class)
                    .buildUnregistered(menuId("pattern_provider_1024"));
        }

        public Provider1024Menu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
            super(TYPE, id, playerInventory, host);
            this.initializePatternSlots();
        }

        private void initializePatternSlots() {
            this.patternSlots = new java.util.ArrayList<>();
            for (var slot : this.getSlots(SlotSemantics.ENCODED_PATTERN)) {
                if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                    this.patternSlots.add(appEngSlot);
                }
            }
        }

        public java.util.List<appeng.menu.slot.AppEngSlot> getPatternSlots() {
            return this.patternSlots;
        }
    }

    public static class ProviderInfMenu extends EnhancedActions implements PagedMenu {
        public static MenuType<ProviderInfMenu> TYPE;
        private java.util.List<appeng.menu.slot.AppEngSlot> patternSlots;

        public static void init() {
            TYPE = MenuTypeBuilder.create(ProviderInfMenu::new, PatternProviderLogicHost.class)
                    .buildUnregistered(menuId("pattern_provider_infinity"));
        }

        public ProviderInfMenu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
            super(TYPE, id, playerInventory, host);
            this.initializePatternSlots();
        }

        private void initializePatternSlots() {
            this.patternSlots = new java.util.ArrayList<>();
            for (var slot : this.getSlots(SlotSemantics.ENCODED_PATTERN)) {
                if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                    this.patternSlots.add(appEngSlot);
                }
            }
        }

        public java.util.List<appeng.menu.slot.AppEngSlot> getPatternSlots() {
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
