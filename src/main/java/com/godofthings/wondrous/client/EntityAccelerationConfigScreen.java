package com.godofthings.wondrous.client;
import com.godofthings.wondrous.init.WondrousItems;

import com.godofthings.wondrous.client.gui.SelectableAE2Button;
import com.godofthings.wondrous.client.gui.FloatingScreen;
import com.godofthings.wondrous.client.gui.StretcherScreenStyle;
import com.godofthings.wondrous.config.WondrousConfig;
import com.godofthings.wondrous.content.entity.WondrousStaffAcceleration;
import com.godofthings.wondrous.init.ModItems;
import com.godofthings.wondrous.init.StaffComponents;
import com.godofthings.wondrous.network.WondrousNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Separate entity controls never change machine or range multipliers. */
public final class EntityAccelerationConfigScreen extends StaffScreen {
    private final Screen parent;
    private final InteractionHand hand;
    private boolean timers;
    private int speed;
    private boolean settingsLoaded;
    private int hintTop;

    public EntityAccelerationConfigScreen(Screen parent, InteractionHand hand) {
        super(Component.translatable("gui.godofthings.entity_acceleration.title"), Tab.ENTITY, parent, hand);
        this.parent = parent;
        this.hand = hand;
    }

    private ItemStack staff() { return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(hand); }

    @Override protected void refreshStaffSettings() { settingsLoaded = false; }
    @Override protected void initContent() {
        if (!settingsLoaded) {
            timers = WondrousStaffAcceleration.usesEntityTimers(staff());
            speed = WondrousStaffAcceleration.getEntityTimerSpeed(staff());
            settingsLoaded = true;
        }
        int left = contentLeft();
        int top = contentTop();
        int bodyWidth = contentWidth();
        for (int i = 0; i < 2; i++) {
            boolean mode = i == 1;
            int modeWidth = (bodyWidth - 3) / 2;
            var button = new SelectableAE2Button(left + i * (modeWidth + 3), top, modeWidth, 19,
                    Component.translatable("gui.godofthings.entity_acceleration." + (mode ? "timers" : "ticks")),
                    ignored -> { timers = mode; save(); });
            button.setSelected(timers == mode);
            button.setTooltip(Tooltip.create(Component.translatable("gui.godofthings.entity_acceleration."
                    + (mode ? "timer_tooltip" : "tick_tooltip"))));
            button.active = WondrousConfig.serverStaffAcceleration();
            addRenderableWidget(button);
        }
        int columns = Math.max(1, Math.min(8, (bodyWidth + 3) / 48));
        int gearWidth = Math.max(1, (bodyWidth - (columns - 1) * 3) / columns);
        for (int i = 0; i < 15; i++) {
            int gear = 1 << (i + 1);
            var button = new SelectableAE2Button(left + (i % columns) * (gearWidth + 3),
                    top + 42 + (i / columns) * 21, gearWidth, 18, Component.literal("x" + gear),
                    ignored -> { speed = gear; save(); });
            button.setSelected(speed == gear);
            button.setTooltip(Tooltip.create(Component.translatable("gui.godofthings.entity_acceleration.speed_tooltip", gear)));
            button.active = timers && WondrousConfig.serverStaffAcceleration();
            addRenderableWidget(button);
        }
        hintTop = 50 + ((15 + columns - 1) / columns) * 21;
        int hintHeight = font.split(hint(), Math.max(1, bodyWidth)).size() * (font.lineHeight + 2);
        int backTop = hintTop + hintHeight + 8;
        addRenderableWidget(new SelectableAE2Button(left + Math.max(0, bodyWidth - 80), top + backTop,
                Math.min(80, bodyWidth), 18,
                Component.translatable("gui.godofthings.back"), ignored -> onClose()));
        setContentExtent(backTop + 22);
    }

    private void save() {
        ItemStack held = staff();
        if (!held.is(WondrousItems.WONDROUS_STAFF.get())) return;
        held.set(StaffComponents.ENTITY_TIMER_MODE.get(), timers);
        held.set(StaffComponents.ENTITY_TIMER_SPEED.get(), speed);
        WondrousNetwork.sendEntityTimerSettings(timers, speed, hand);
        rebuildWidgets();
    }

    private Component hint() {
        return Component.translatable("gui.godofthings.entity_acceleration."
                + (timers ? "timer_hint" : "tick_hint"));
    }

    @Override protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(font, Component.translatable("gui.godofthings.entity_acceleration.speed"),
                contentLeft(), contentTop() + 28, StretcherScreenStyle.TEXT_COLOR, false);
        int y = contentTop() + hintTop;
        for (var line : font.split(hint(), Math.max(1, contentWidth()))) {
            graphics.drawString(font, line, contentLeft(), y, StretcherScreenStyle.SUBTLE_TEXT_COLOR, false);
            y += font.lineHeight + 2;
        }
    }

    @Override public void tick() {
        if (minecraft.player != null && !staff().is(WondrousItems.WONDROUS_STAFF.get())) { onClose(); return; }
        boolean currentTimers = WondrousStaffAcceleration.usesEntityTimers(staff());
        int currentSpeed = WondrousStaffAcceleration.getEntityTimerSpeed(staff());
        if (timers != currentTimers || speed != currentSpeed) {
            timers = currentTimers;
            speed = currentSpeed;
            rebuildWidgets();
        }
    }
    @Override public void onClose() { returnToPage(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
