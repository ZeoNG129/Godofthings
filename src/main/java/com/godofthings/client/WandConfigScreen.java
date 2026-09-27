package com.godofthings.client;

import com.godofthings.network.WandMessages;
import com.godofthings.network.WandConfigPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 神之工具的加速配置界面（对照万象担架的 {@code WondrousStaffConfigScreen}）。
 *
 * <p>H 键打开。可调：<b>加速倍率</b>（2 → 1024，共 10 档）与 <b>持续模式</b>
 * （关 = 只加速 30 秒；开 = 永久加速）。每次点击立即把配置发到服务端写进物品数据。
 */
public class WandConfigScreen extends Screen
{
    /** 倍率档位（与上游同量级：2 的幂，上限 1024） */
    private static final int[] SPEEDS = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};

    private int speedIndex;
    private boolean permanent;

    public WandConfigScreen()
    {
        super(Component.translatable("gui.godofthings.wand_config.title"));
        ItemStack stack = heldWand();
        int speed = 2;
        if (!stack.isEmpty())
        {
            var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            speed = tag.getInt(WandConfigPayload.TAG_SPEED);
            this.permanent = tag.getBoolean(WandConfigPayload.TAG_PERMANENT);
        }
        this.speedIndex = nearestIndex(speed);
    }

    private static ItemStack heldWand()
    {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null)
        {
            return ItemStack.EMPTY;
        }
        if (player.getMainHandItem().getItem() instanceof com.godofthings.item.GodFavorWandItem)
        {
            return player.getMainHandItem();
        }
        return player.getOffhandItem().getItem() instanceof com.godofthings.item.GodFavorWandItem
                ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    private static int nearestIndex(int speed)
    {
        int best = 0;
        for (int i = 0; i < SPEEDS.length; i++)
        {
            if (SPEEDS[i] <= speed)
            {
                best = i;
            }
        }
        return best;
    }

    private void push()
    {
        WandMessages.sendWandConfig(SPEEDS[this.speedIndex], this.permanent);
    }

    @Override
    protected void init()
    {
        int cx = this.width / 2;
        int y = this.height / 2 - 40;

        addRenderableWidget(Button.builder(Component.literal("◀"), b ->
        {
            this.speedIndex = (this.speedIndex + SPEEDS.length - 1) % SPEEDS.length;
            push();
        }).bounds(cx - 110, y, 24, 20).build());

        addRenderableWidget(Button.builder(speedLabel(), b ->
        {
            this.speedIndex = (this.speedIndex + 1) % SPEEDS.length;
            push();
            rebuildWidgets();
        }).bounds(cx - 80, y, 160, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▶"), b ->
        {
            this.speedIndex = (this.speedIndex + 1) % SPEEDS.length;
            push();
            rebuildWidgets();
        }).bounds(cx + 86, y, 24, 20).build());

        addRenderableWidget(Button.builder(permanentLabel(), b ->
        {
            this.permanent = !this.permanent;
            push();
            rebuildWidgets();
        }).bounds(cx - 80, y + 26, 160, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 60, y + 62, 120, 20).build());
    }

    private Component speedLabel()
    {
        return Component.translatable("gui.godofthings.wand_config.speed", SPEEDS[this.speedIndex])
                .withStyle(ChatFormatting.AQUA);
    }

    private Component permanentLabel()
    {
        return Component.translatable("gui.godofthings.wand_config.permanent",
                Component.translatable(this.permanent ? "gui.godofthings.wand_config.on"
                        : "gui.godofthings.wand_config.off"))
                .withStyle(this.permanent ? ChatFormatting.GOLD : ChatFormatting.GRAY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFD700);
        g.drawCenteredString(this.font, Component.translatable("gui.godofthings.wand_config.hint"),
                this.width / 2, this.height / 2 + 40, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}