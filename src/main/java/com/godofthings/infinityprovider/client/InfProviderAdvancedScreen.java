package com.godofthings.infinityprovider.client;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;

import com.godofthings.infinityprovider.InfProviderMenu;

/**
 * 高级设置子屏（移植自 AE2LT 的 OverloadedPatternProviderAdvancedScreen）：
 * 无线派发策略 / 无线速度 / 机器并行数。样式 {@code /screens/inf_provider_advanced.json}。
 */
public class InfProviderAdvancedScreen<M extends InfProviderMenu.EnhancedActions>
        extends AESubScreen<M, InfProviderEnhancedScreen<M>> {

    private static final int LABEL_X = 38;
    private static final int STRATEGY_Y = 33;
    private static final int SPEED_Y = 57;
    private static final int FILTER_Y = 81;

    private final InfToggleButton wirelessStrategyButton;
    private final InfToggleButton wirelessSpeedButton;
    private final InfToggleButton filteredImportButton;
    private final AETextField machineParallelism;
    private final Button applyParallelism;
    private int lastSyncedParallelism;

    public InfProviderAdvancedScreen(InfProviderEnhancedScreen<M> parent) {
        super(parent, "/screens/inf_provider_advanced.json");

        var menu = parent.getMenu();
        var backLabel = parent.getTitle();
        this.widgets.add("return", new TabButton(Icon.BACK, backLabel, btn -> this.returnToParent()));

        this.wirelessStrategyButton = new InfToggleButton(
                InfToggleButton.ButtonType.WIRELESS_STRATEGY,
                btn -> menu.clientToggleDispatch());
        this.wirelessStrategyButton.setTooltipOn(
                List.of(Component.translatable("gui.godofthings.infprovider.strategy.even")));
        this.wirelessStrategyButton.setTooltipOff(
                List.of(Component.translatable("gui.godofthings.infprovider.strategy.single")));
        this.widgets.add("wirelessStrategy", this.wirelessStrategyButton);

        this.wirelessSpeedButton = new InfToggleButton(
                InfToggleButton.ButtonType.SPEED,
                btn -> menu.clientToggleSpeed());
        this.wirelessSpeedButton.setTooltipOn(
                List.of(Component.translatable("gui.godofthings.infprovider.speed.fast")));
        this.wirelessSpeedButton.setTooltipOff(
                List.of(Component.translatable("gui.godofthings.infprovider.speed.normal")));
        this.widgets.add("wirelessSpeed", this.wirelessSpeedButton);

        this.filteredImportButton = new InfToggleButton(
                InfToggleButton.ButtonType.FILTERED_IMPORT,
                btn -> menu.clientToggleFiltered());
        this.filteredImportButton.setTooltipOn(
                List.of(Component.translatable("gui.godofthings.infprovider.filtered.on")));
        this.filteredImportButton.setTooltipOff(
                List.of(Component.translatable("gui.godofthings.infprovider.filtered.off")));
        this.widgets.add("filteredImport", this.filteredImportButton);

        this.machineParallelism = this.widgets.addTextField("machineParallelism");
        this.machineParallelism.setMaxLength(10);
        this.machineParallelism.setFilter(InfProviderAdvancedScreen::isParallelismDraft);
        this.lastSyncedParallelism = menu.getMachineParallelism();
        this.machineParallelism.setValue(Integer.toString(this.lastSyncedParallelism));
        this.machineParallelism.setTooltip(Tooltip.create(Component.translatable(
                "gui.godofthings.infprovider.parallelism_tooltip")));
        this.applyParallelism = this.widgets.addButton("applyParallelism",
                Component.translatable("gui.godofthings.infprovider.apply"), this::applyParallelism);
    }

    private static boolean isParallelismDraft(String text) {
        if (text.isEmpty()) {
            return true;
        }
        try {
            return text.chars().allMatch(c -> c >= '0' && c <= '9')
                    && Integer.parseInt(text) >= 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private int draftedParallelism() {
        try {
            return Integer.parseInt(this.machineParallelism.getValue());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void applyParallelism() {
        int value = this.draftedParallelism();
        if (value > 0) {
            this.menu.clientSetParallelism(value);
        }
    }

    @Override
    protected boolean shouldAddToolbar() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        boolean wirelessActive = this.menu.isWirelessMode();

        this.wirelessStrategyButton.setStateIndex(this.menu.isEvenDistribution() ? 1 : 0);
        this.wirelessStrategyButton.setVisibility(true);
        this.wirelessStrategyButton.active = wirelessActive;

        this.wirelessSpeedButton.setStateIndex(this.menu.isFastSpeed() ? 1 : 0);
        this.wirelessSpeedButton.setVisibility(true);
        this.wirelessSpeedButton.active = wirelessActive;

        this.filteredImportButton.setState(this.menu.isFilteredImport());
        this.filteredImportButton.setVisibility(true);

        if (this.lastSyncedParallelism != this.menu.getMachineParallelism()) {
            if (!this.machineParallelism.isFocused()
                    || this.draftedParallelism() == this.lastSyncedParallelism) {
                this.machineParallelism.setValue(Integer.toString(this.menu.getMachineParallelism()));
            }
            this.lastSyncedParallelism = this.menu.getMachineParallelism();
        }
        this.machineParallelism.setEditable(wirelessActive);
        this.applyParallelism.visible = true;
        this.applyParallelism.active = wirelessActive
                && this.draftedParallelism() > 0
                && this.draftedParallelism() != this.menu.getMachineParallelism();
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        int wirelessColor = this.menu.isWirelessMode() ? 0x404040 : 0x909090;
        guiGraphics.drawString(this.font,
                Component.translatable("gui.godofthings.infprovider.advanced.distribution"),
                LABEL_X, STRATEGY_Y, wirelessColor, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.godofthings.infprovider.advanced.probe"),
                LABEL_X, SPEED_Y, wirelessColor, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.godofthings.infprovider.advanced.input_filter"),
                LABEL_X, FILTER_Y, 0x404040, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.godofthings.infprovider.advanced.machine_parallelism"),
                14, 105, wirelessColor, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.godofthings.infprovider.advanced.parallelism_hint"),
                14, 147, 0x707070, false);
        if (!this.menu.isWirelessMode()) {
            guiGraphics.drawString(this.font,
                    Component.translatable("gui.godofthings.infprovider.advanced.wireless_hint"),
                    14, 161, 0x707070, false);
        }
    }

    @Override
    public void onClose() {
        this.returnToParent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.machineParallelism.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            if (this.applyParallelism.active) {
                this.applyParallelism();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                || !this.machineParallelism.isFocused()
                        && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.returnToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
