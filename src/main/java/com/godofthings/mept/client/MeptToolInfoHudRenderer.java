package com.godofthings.mept.client;

import appeng.api.util.AEColor;
import com.godofthings.mept.ItemMECablePlacementTool;
import com.godofthings.mept.ItemMEPlacementTool;
import com.godofthings.mept.ItemMultiblockPlacementTool;
import com.godofthings.mept.MeptDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders tool information HUD on the right side of the crosshair.
 * Displays different information based on the tool type held by the player.
 * Shows HUD for 2 seconds when switching to the tool, then auto-hides.
 */
@OnlyIn(Dist.CLIENT)
public class MeptToolInfoHudRenderer {

    private static final int CROSSHAIR_OFFSET_X = 15;
    private static final float FONT_SCALE = 0.75f;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int TEXT_COLOR_DIM = 0xAAAAAA;
    
    // HUD display duration in milliseconds
    private static final long HUD_DISPLAY_DURATION = 2000L;
    
    // Track the last held tool item to detect switching
    private Item lastHeldToolItem = null;
    // Track when the tool was switched to
    private long toolSwitchTime = 0L;
    // Cache built HUD lines; rebuilt only when the tool stack changes
    private ItemStack cachedToolStack = ItemStack.EMPTY;
    private List<String> cachedLines = new ArrayList<>();

    @SubscribeEvent
    public void onRenderGuiOverlay(RenderGuiLayerEvent.Post event) {
        // Only render after crosshair layer
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }

        // Check if any radial menu is open - don't render HUD if so
        if (mc.screen instanceof MeptRadialMenuScreen || 
            mc.screen instanceof MeptDualLayerRadialMenuScreen) {
            return;
        }

        // Check main hand and off hand for placement tools
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // Determine current tool item
        Item currentToolItem = null;
        ItemStack currentToolStack = ItemStack.EMPTY;
        
        if (isPlacementTool(mainHand)) {
            currentToolItem = mainHand.getItem();
            currentToolStack = mainHand;
        } else if (isPlacementTool(offHand)) {
            currentToolItem = offHand.getItem();
            currentToolStack = offHand;
        }
        
        // Handle tool switching detection
        long currentTime = System.currentTimeMillis();
        
        if (currentToolItem != null) {
            // Check if we just switched to a tool
            if (lastHeldToolItem != currentToolItem) {
                // Switched to a new tool, reset timer
                toolSwitchTime = currentTime;
                lastHeldToolItem = currentToolItem;
            }
            
            // Check if we're within the display duration
            if (currentTime - toolSwitchTime > HUD_DISPLAY_DURATION) {
                // Time expired, don't render
                return;
            }
        } else {
            // Not holding any tool, reset state
            lastHeldToolItem = null;
            toolSwitchTime = 0L;
            return;
        }

        // Rebuild lines only when the tool stack (or its components) changed -
        // parsing NBT and building translated strings every frame is wasteful.
        if (!ItemStack.matches(cachedToolStack, currentToolStack)) {
            cachedToolStack = currentToolStack.copy();
            cachedLines = new ArrayList<>();
            if (currentToolStack.getItem() instanceof ItemMEPlacementTool) {
                collectMEPlacementToolInfo(currentToolStack, cachedLines);
            } else if (currentToolStack.getItem() instanceof ItemMultiblockPlacementTool) {
                collectMultiblockToolInfo(currentToolStack, cachedLines);
            } else if (currentToolStack.getItem() instanceof ItemMECablePlacementTool) {
                collectCableToolInfo(currentToolStack, cachedLines);
            }
        }

        if (!cachedLines.isEmpty()) {
            renderHudLines(event.getGuiGraphics(), mc, cachedLines);
        }
    }

    /**
     * Check if the given item stack is a placement tool.
     * TODO: Add ItemMECablePlacementTool check when ME Cable Placement Tool is added
     */
    private boolean isPlacementTool(ItemStack stack) {
        return stack.getItem() instanceof ItemMEPlacementTool ||
               stack.getItem() instanceof ItemMultiblockPlacementTool ||
               stack.getItem() instanceof ItemMECablePlacementTool;
    }

    /**
     * Collect information for ME Placement Tool.
     */
    private void collectMEPlacementToolInfo(ItemStack tool, List<String> lines) {
        CompoundTag cfg = tool.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg == null) {
            return;
        }

        int selected = cfg.getInt("SelectedSlot");
        if (selected < 0 || selected >= 18) selected = 0;

        // Get item from config using similar logic to ItemMEPlacementTool
        ItemStack target = getItemFromConfig(cfg, selected);
        if (target != null && !target.isEmpty()) {
            String itemName = target.getHoverName().getString();
            lines.add(Component.translatable("godofthings.mept.hud.item", itemName).getString());
        }
    }
    
    /**
     * Get item from config tag for the specified slot.
     */
    private ItemStack getItemFromConfig(CompoundTag cfg, int slot) {
        if (cfg == null) return ItemStack.EMPTY;
        var level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        
        CompoundTag itemsTag = cfg.contains("items") ? cfg.getCompound("items") : cfg;
        if (itemsTag.contains("Items")) {
            net.minecraft.nbt.ListTag list = itemsTag.getList("Items", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                if (itemTag.getInt("Slot") == slot) {
                    return ItemStack.parseOptional(level.registryAccess(), itemTag);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Collect information for Multiblock Placement Tool.
     */
    private void collectMultiblockToolInfo(ItemStack tool, List<String> lines) {
        // First collect item info same as ME Placement Tool
        collectMEPlacementToolInfo(tool, lines);

        // Add placement count
        int placementCount = ItemMultiblockPlacementTool.getPlacementCount(tool);
        lines.add(Component.translatable("godofthings.mept.hud.placement_count", placementCount).getString());

        // Add direction mode
        ItemMultiblockPlacementTool.DirectionMode direction = ItemMultiblockPlacementTool.getDirectionMode(tool);
        String directionName = Component.translatable(direction.translationKey()).getString();
        lines.add(Component.translatable("godofthings.mept.hud.direction", directionName).getString());
    }

    /**
     * Collect information for Cable Placement Tool.
     */
    private void collectCableToolInfo(ItemStack tool, List<String> lines) {
        // Cable Type
        ItemMECablePlacementTool.CableType cableType = ItemMECablePlacementTool.getCableType(tool);
        String cableTypeName = getCableTypeName(cableType);
        lines.add(Component.translatable("godofthings.mept.hud.cable_type", cableTypeName).getString());

        // Color - built-in upgrade: always show the selected color
        AEColor color = ItemMECablePlacementTool.getColor(tool);
        String colorName = getColorName(color);
        lines.add(Component.translatable("godofthings.mept.hud.color", colorName).getString());

        // Mode
        ItemMECablePlacementTool.PlacementMode mode = ItemMECablePlacementTool.getMode(tool);
        String modeName = getModeName(mode);
        lines.add(Component.translatable("godofthings.mept.hud.mode", modeName).getString());
    }

    /**
     * Get localized cable type name.
     */
    private String getCableTypeName(ItemMECablePlacementTool.CableType type) {
        return switch (type) {
            case GLASS -> Component.translatable("godofthings.mept.cable.glass").getString();
            case COVERED -> Component.translatable("godofthings.mept.cable.covered").getString();
            case SMART -> Component.translatable("godofthings.mept.cable.smart").getString();
            case DENSE_COVERED -> Component.translatable("godofthings.mept.cable.dense_covered").getString();
            case DENSE_SMART -> Component.translatable("godofthings.mept.cable.dense_smart").getString();
        };
    }

    /**
     * Get localized color name.
     */
    private String getColorName(AEColor color) {
        return switch (color) {
            case WHITE -> Component.translatable("godofthings.mept.color.white").getString();
            case ORANGE -> Component.translatable("godofthings.mept.color.orange").getString();
            case MAGENTA -> Component.translatable("godofthings.mept.color.magenta").getString();
            case LIGHT_BLUE -> Component.translatable("godofthings.mept.color.light_blue").getString();
            case YELLOW -> Component.translatable("godofthings.mept.color.yellow").getString();
            case LIME -> Component.translatable("godofthings.mept.color.lime").getString();
            case PINK -> Component.translatable("godofthings.mept.color.pink").getString();
            case GRAY -> Component.translatable("godofthings.mept.color.gray").getString();
            case LIGHT_GRAY -> Component.translatable("godofthings.mept.color.light_gray").getString();
            case CYAN -> Component.translatable("godofthings.mept.color.cyan").getString();
            case PURPLE -> Component.translatable("godofthings.mept.color.purple").getString();
            case BLUE -> Component.translatable("godofthings.mept.color.blue").getString();
            case BROWN -> Component.translatable("godofthings.mept.color.brown").getString();
            case GREEN -> Component.translatable("godofthings.mept.color.green").getString();
            case RED -> Component.translatable("godofthings.mept.color.red").getString();
            case BLACK -> Component.translatable("godofthings.mept.color.black").getString();
            case TRANSPARENT -> Component.translatable("godofthings.mept.color.transparent").getString();
        };
    }

    /**
     * Get localized mode name.
     */
    private String getModeName(ItemMECablePlacementTool.PlacementMode mode) {
        return switch (mode) {
            case LINE -> Component.translatable("godofthings.mept.mode.line").getString();
            case PLANE_FILL -> Component.translatable("godofthings.mept.mode.plane_fill").getString();
            case PLANE_BRANCHING -> Component.translatable("godofthings.mept.mode.plane_branching").getString();
        };
    }

    /**
     * Render HUD lines on the right side of the crosshair with small font.
     */
    private void renderHudLines(GuiGraphics guiGraphics, Minecraft mc, List<String> lines) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Center of screen (where crosshair is)
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Start position (right of crosshair)
        int startX = centerX + CROSSHAIR_OFFSET_X;
        int startY = centerY - (lines.size() * (int)(LINE_HEIGHT * FONT_SCALE)) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(FONT_SCALE, FONT_SCALE, 1.0f);

        // Scale coordinates to match the scaled rendering
        float scaledStartX = startX / FONT_SCALE;
        float scaledStartY = startY / FONT_SCALE;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int y = (int)(scaledStartY + i * LINE_HEIGHT);
            
            // Draw with shadow for better visibility
            guiGraphics.drawString(mc.font, line, (int)scaledStartX, y, TEXT_COLOR, true);
        }

        guiGraphics.pose().popPose();
    }
}
