package com.godofthings.superfusion.client;

import com.godofthings.superfusion.ModuleConfigLogic;
import com.godofthings.superfusion.SuperFusionComponents;
import com.godofthings.superfusion.SuperFusionItem;
import com.godofthings.superfusion.network.FusionActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 超级融合卡设置界面（移植自 Mekanism-Card 的 {@code SuperFusionCardScreen}）。
 * <p>
 * 三个按钮分别控制：融合模式、模块操作模式（安装/清空）、目标匹配模式（精确/模糊）。
 * 界面本身用纯色块绘制（照抄上游），不依赖额外贴图。
 */
public class SuperFusionScreen extends Screen
{
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 116;
    private static final int PADDING = 8;
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_HIGHLIGHT = 0xFFFFFFFF;
    private static final int COLOR_MID_HIGHLIGHT = 0xFFDBDBDB;
    private static final int COLOR_SHADOW = 0xFF555555;
    private static final int COLOR_DEEP_SHADOW = 0xFF373737;
    private static final int COLOR_RECESSED = 0xFF9B9B9B;
    private static final int COLOR_TEXT = 0xFF404040;
    private static final int COLOR_DIM = 0xFF666666;
    private static final int COLOR_CYAN = 0xFF007F8F;
    private static final int COLOR_GREEN = 0xFF2E7D32;
    private static final int COLOR_ORANGE = 0xFF9A5A00;

    private Button fusionModeButton;
    private Button moduleModeButton;
    private Button targetModeButton;

    public SuperFusionScreen()
    {
        super(Component.translatable("gui.godofthings.super_fusion.title"));
    }

    /** 取手上那张卡（界面上所有读写都基于它） */
    private ItemStack getCardStack()
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
        {
            return ItemStack.EMPTY;
        }
        ItemStack mainHand = player.getMainHandItem();
        return mainHand.getItem() instanceof SuperFusionItem ? mainHand : ItemStack.EMPTY;
    }

    @Override
    protected void init()
    {
        super.init();
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        int buttonX = x + 66;
        int buttonWidth = 100;
        int buttonHeight = 14;

        fusionModeButton = addRenderableWidget(Button.builder(Component.empty(), button -> onCycleFusionMode())
                .bounds(buttonX, y + 34, buttonWidth, buttonHeight).build());
        moduleModeButton = addRenderableWidget(Button.builder(Component.empty(), button -> onToggleModuleMode())
                .bounds(buttonX, y + 51, buttonWidth, buttonHeight).build());
        targetModeButton = addRenderableWidget(Button.builder(Component.empty(), button -> onToggleTargetMode())
                .bounds(buttonX, y + 68, buttonWidth, buttonHeight).build());
    }

    private void onCycleFusionMode()
    {
        ItemStack stack = getCardStack();
        if (stack.isEmpty())
        {
            return;
        }
        SuperFusionItem.FusionMode current = SuperFusionItem.getMode(stack);
        SuperFusionItem.FusionMode[] values = SuperFusionItem.FusionMode.values();
        SuperFusionItem.FusionMode next = values[(current.ordinal() + 1) % values.length];
        // 客户端先本地切一次，界面立刻有反馈；服务端收到包后再同步一次
        stack.set(SuperFusionComponents.FUSION_MODE.get(), next.ordinal());
        if (next == SuperFusionItem.FusionMode.MEMORY_COPY)
        {
            SuperFusionItem.setFuzzy(stack, false);
        }
        PacketDistributor.sendToServer(FusionActionPayload.cycleMode());
    }

    private void onToggleModuleMode()
    {
        ItemStack stack = getCardStack();
        if (stack.isEmpty() || SuperFusionItem.getMode(stack) != SuperFusionItem.FusionMode.MODULE_UPGRADE)
        {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null)
        {
            return;
        }
        ModuleConfigLogic.Mode targetMode = ModuleConfigLogic.getCurrentMode(stack) == ModuleConfigLogic.Mode.INSTALL
                ? ModuleConfigLogic.Mode.CLEAR
                : ModuleConfigLogic.Mode.INSTALL;
        ModuleConfigLogic.setMode(stack, player, targetMode);
    }

    private void onToggleTargetMode()
    {
        ItemStack stack = getCardStack();
        if (stack.isEmpty() || SuperFusionItem.getMode(stack) == SuperFusionItem.FusionMode.MEMORY_COPY)
        {
            return;
        }
        boolean fuzzy = !SuperFusionItem.isFuzzy(stack);
        SuperFusionItem.setFuzzy(stack, fuzzy);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        // 不画模糊背景，保持与上游一致的朴素外观
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ItemStack stack = getCardStack();
        if (stack.isEmpty())
        {
            onClose();
            return;
        }

        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        drawRaisedPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.renderItem(stack, x + PADDING, y + 6);
        graphics.drawString(font, title, x + 29, y + 10, COLOR_TEXT, false);

        drawRecessedPanel(graphics, x + PADDING, y + 31, PANEL_WIDTH - PADDING * 2, 54);
        SuperFusionItem.FusionMode fusionMode = SuperFusionItem.getMode(stack);
        ModuleConfigLogic.Mode moduleMode = ModuleConfigLogic.getCurrentMode(stack);
        fusionModeButton.setMessage(compactFusionModeName(fusionMode));
        moduleModeButton.setMessage(moduleMode.getDisplayName());
        targetModeButton.setMessage(Component.translatable(SuperFusionItem.isFuzzy(stack)
                ? "gui.godofthings.super_fusion.target.fuzzy"
                : "gui.godofthings.super_fusion.target.precise"));
        moduleModeButton.active = fusionMode == SuperFusionItem.FusionMode.MODULE_UPGRADE;
        targetModeButton.active = fusionMode != SuperFusionItem.FusionMode.MEMORY_COPY;
        updateButtonTooltips(fusionMode, moduleMode, SuperFusionItem.isFuzzy(stack));

        graphics.drawString(font, Component.translatable("gui.godofthings.super_fusion.fusion_mode_short"),
                x + 13, y + 37, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.godofthings.super_fusion.module_mode_short"),
                x + 13, y + 54, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.godofthings.super_fusion.target_mode_short"),
                x + 13, y + 71, COLOR_TEXT, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component compactFusionModeName(SuperFusionItem.FusionMode mode)
    {
        return Component.translatable(switch (mode)
        {
            case TIER_INSTALL -> "gui.godofthings.super_fusion.mode_compact.tier";
            case MODULE_UPGRADE -> "gui.godofthings.super_fusion.mode_compact.module";
            case MEMORY_COPY -> "gui.godofthings.super_fusion.mode_compact.memory";
            case FULL_PASTE -> "gui.godofthings.super_fusion.mode_compact.full";
        });
    }

    private void updateButtonTooltips(SuperFusionItem.FusionMode fusionMode, ModuleConfigLogic.Mode moduleMode,
                                      boolean fuzzy)
    {
        String fusionTooltip = switch (fusionMode)
        {
            case TIER_INSTALL -> "gui.godofthings.super_fusion.tooltip.mode.tier";
            case MODULE_UPGRADE -> "gui.godofthings.super_fusion.tooltip.mode.module";
            case MEMORY_COPY -> "gui.godofthings.super_fusion.tooltip.mode.memory";
            case FULL_PASTE -> "gui.godofthings.super_fusion.tooltip.mode.full";
        };
        fusionModeButton.setTooltip(Tooltip.create(Component.translatable(fusionTooltip)));
        moduleModeButton.setTooltip(Tooltip.create(Component.translatable(
                moduleMode == ModuleConfigLogic.Mode.INSTALL
                        ? "gui.godofthings.super_fusion.tooltip.module.install"
                        : "gui.godofthings.super_fusion.tooltip.module.clear")));
        String targetTooltip = fusionMode == SuperFusionItem.FusionMode.MEMORY_COPY
                ? "gui.godofthings.super_fusion.tooltip.target.memory"
                : fuzzy
                ? "gui.godofthings.super_fusion.tooltip.target.fuzzy"
                : "gui.godofthings.super_fusion.tooltip.target.precise";
        targetModeButton.setTooltip(Tooltip.create(Component.translatable(targetTooltip)));
    }

    private void drawRaisedPanel(GuiGraphics graphics, int x, int y, int width, int height)
    {
        graphics.fill(x, y, x + width, y + height, COLOR_DEEP_SHADOW);
        graphics.fill(x, y, x + width - 2, y + height - 2, COLOR_PANEL);
        graphics.fill(x, y, x + width - 2, y + 2, COLOR_HIGHLIGHT);
        graphics.fill(x, y, x + 2, y + height - 2, COLOR_HIGHLIGHT);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, COLOR_MID_HIGHLIGHT);
        graphics.fill(x + 2, y + 2, x + 3, y + height - 2, COLOR_MID_HIGHLIGHT);
        graphics.fill(x + 2, y + height - 4, x + width - 2, y + height - 2, COLOR_SHADOW);
        graphics.fill(x + width - 4, y + 2, x + width - 2, y + height - 2, COLOR_SHADOW);
    }

    private void drawRecessedPanel(GuiGraphics graphics, int x, int y, int width, int height)
    {
        graphics.fill(x, y, x + width, y + height, COLOR_HIGHLIGHT);
        graphics.fill(x, y, x + width - 1, y + height - 1, COLOR_SHADOW);
        graphics.fill(x + 2, y + 2, x + width - 1, y + height - 1, COLOR_RECESSED);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
