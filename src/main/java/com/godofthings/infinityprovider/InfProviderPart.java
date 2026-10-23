package com.godofthings.infinityprovider;

import appeng.api.networking.GridFlags;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModels;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.parts.PartModel;
import appeng.parts.crafting.PatternProviderPart;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

/**
 * 四档供应器的线缆部件（part）形态（移植自 InfinityPatternProvider）。
 * 模型 = 自家底板 + AE2 接口的开关/通道状态贴图；槽位数同样只改 {@code createLogic}。
 */
public abstract class InfProviderPart extends PatternProviderPart {

    private static final ResourceLocation AE2_INTERFACE_OFF =
            ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_off");
    private static final ResourceLocation AE2_INTERFACE_ON =
            ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_on");
    private static final ResourceLocation AE2_INTERFACE_HAS_CHANNEL =
            ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_has_channel");

    /** 每档自己的 base 模型 id。 */
    protected abstract ResourceLocation baseModel();

    @Override
    public IPartModel getStaticModels() {
        ResourceLocation base = baseModel();
        if (this.isPowered()) {
            return this.isActive()
                    ? new PartModel(new ResourceLocation[] { base, AE2_INTERFACE_HAS_CHANNEL })
                    : new PartModel(new ResourceLocation[] { base, AE2_INTERFACE_ON });
        }
        return new PartModel(new ResourceLocation[] { base, AE2_INTERFACE_OFF });
    }

    protected InfProviderPart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    // 每档 part 的具体实现 =====================================================

    public static class Provider1PartItem extends PartItem<Provider1Part> {
        public Provider1PartItem(Properties props) {
            super(props, Provider1Part.class, Provider1Part::new);
        }
    }

    public static class Provider1Part extends InfProviderPart {
        public static final ResourceLocation BASE =
                ResourceLocation.fromNamespaceAndPath(GodofthingsModId.MODID, "part/_1slots_pattern_provider_base");

        public Provider1Part(IPartItem<?> partItem) {
            super(partItem);
        }

        @Override
        protected ResourceLocation baseModel() {
            return BASE;
        }

        @Override
        protected PatternProviderLogic createLogic() {
            return new PatternProviderLogic(this.getMainNode(), this, 1);
        }

        @Override
        public void openMenu(Player player, MenuHostLocator locator) {
            MenuOpener.open(InfProviderMenu.Provider1Menu.TYPE, player, locator);
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            MenuOpener.returnTo(InfProviderMenu.Provider1Menu.TYPE, player, subMenu.getLocator());
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack(InfProviderRegistry.P1_PART_ITEM.get());
        }

        @Override
        public AEItemKey getTerminalIcon() {
            return AEItemKey.of(this.getPartItem());
        }
    }

    public static class Provider32PartItem extends PartItem<Provider32Part> {
        public Provider32PartItem(Properties props) {
            super(props, Provider32Part.class, Provider32Part::new);
        }
    }

    public static class Provider32Part extends InfProviderPart {
        public static final ResourceLocation BASE =
                ResourceLocation.fromNamespaceAndPath(GodofthingsModId.MODID, "part/_32slots_pattern_provider_base");

        public Provider32Part(IPartItem<?> partItem) {
            super(partItem);
        }

        @Override
        protected ResourceLocation baseModel() {
            return BASE;
        }

        @Override
        protected PatternProviderLogic createLogic() {
            return new PatternProviderLogic(this.getMainNode(), this, 32);
        }

        @Override
        public void openMenu(Player player, MenuHostLocator locator) {
            MenuOpener.open(InfProviderMenu.Provider32Menu.TYPE, player, locator);
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            MenuOpener.returnTo(InfProviderMenu.Provider32Menu.TYPE, player, subMenu.getLocator());
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack(InfProviderRegistry.P32_PART_ITEM.get());
        }

        @Override
        public AEItemKey getTerminalIcon() {
            return AEItemKey.of(this.getPartItem());
        }
    }

    public static class Provider1024PartItem extends PartItem<Provider1024Part> {
        public Provider1024PartItem(Properties props) {
            super(props, Provider1024Part.class, Provider1024Part::new);
        }
    }

    public static class Provider1024Part extends InfProviderPart {
        public static final ResourceLocation BASE =
                ResourceLocation.fromNamespaceAndPath(GodofthingsModId.MODID, "part/_1024slots_pattern_provider_base");

        public Provider1024Part(IPartItem<?> partItem) {
            super(partItem);
        }

        @Override
        protected ResourceLocation baseModel() {
            return BASE;
        }

        @Override
        protected PatternProviderLogic createLogic() {
            return new PatternProviderLogic(this.getMainNode(), this, 1024);
        }

        @Override
        public void openMenu(Player player, MenuHostLocator locator) {
            MenuOpener.open(InfProviderMenu.Provider1024Menu.TYPE, player, locator);
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            MenuOpener.returnTo(InfProviderMenu.Provider1024Menu.TYPE, player, subMenu.getLocator());
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack(InfProviderRegistry.P1024_PART_ITEM.get());
        }

        @Override
        public AEItemKey getTerminalIcon() {
            return AEItemKey.of(this.getPartItem());
        }
    }

    public static class ProviderInfPartItem extends PartItem<ProviderInfPart> {
        public ProviderInfPartItem(Properties props) {
            super(props, ProviderInfPart.class, ProviderInfPart::new);
        }
    }

    public static class ProviderInfPart extends InfProviderPart {
        public static final ResourceLocation BASE =
                ResourceLocation.fromNamespaceAndPath(GodofthingsModId.MODID, "part/infinity_pattern_provider_base");

        public ProviderInfPart(IPartItem<?> partItem) {
            super(partItem);
        }

        @Override
        protected ResourceLocation baseModel() {
            return BASE;
        }

        @Override
        protected PatternProviderLogic createLogic() {
            return new PatternProviderLogic(this.getMainNode(), this, InfProviderBlockEntity.INFINITY_SLOTS);
        }

        @Override
        public void openMenu(Player player, MenuHostLocator locator) {
            MenuOpener.open(InfProviderMenu.ProviderInfMenu.TYPE, player, locator);
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            MenuOpener.returnTo(InfProviderMenu.ProviderInfMenu.TYPE, player, subMenu.getLocator());
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack(InfProviderRegistry.PINF_PART_ITEM.get());
        }

        @Override
        public AEItemKey getTerminalIcon() {
            return AEItemKey.of(this.getPartItem());
        }
    }
}
