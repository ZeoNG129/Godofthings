package com.godofthings.mept.client;

import com.godofthings.mept.client.MeptKeyBindings;
import com.godofthings.mept.client.MeptClientOverlay;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import com.godofthings.mept.ItemMEPlacementTool;
import com.godofthings.mept.MeptDataComponents;
import com.godofthings.mept.WandMenu;
import com.godofthings.mept.network.UpdateWandConfigPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Radial menu for ME Placement Tool item selection.
 * Restored functionality for NeoForge 1.21.1
 */
public class MeptRadialMenuScreen extends Screen {
    private static final float PRECISION = 5.0f;
    private static final int MAX_SLOTS = 18;
    private static final float OPEN_ANIMATION_LENGTH = 0.25f;

    private float totalTime;
    private float prevTick;
    private float extraTick;
    private int selectedItem = -1;
    private boolean closing = false;
    private int currentSelectedSlot = -1; // Currently selected slot from config

    // Slot data
    private final List<SlotData> slots = new ArrayList<>();
    private final ItemStack wandStack;

    public record SlotData(int index, ItemStack displayStack, String name) {}

    public MeptRadialMenuScreen() {
        super(Component.literal(""));
        this.minecraft = Minecraft.getInstance();
        // Look up the wand from main hand first, off hand second, so the radial menu works in either hand.
        this.wandStack = minecraft.player != null
                ? com.godofthings.mept.BasePlacementToolItem.findHeldTool(minecraft.player, ItemMEPlacementTool.class)
                : ItemStack.EMPTY;
        loadSlots();
        loadCurrentSelection();
    }

    private void loadCurrentSelection() {
        if (wandStack.isEmpty()) return;
        CompoundTag cfg = wandStack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg != null && cfg.contains("SelectedSlot")) {
            currentSelectedSlot = cfg.getInt("SelectedSlot");
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private void loadSlots() {
        slots.clear();
        if (wandStack.isEmpty()) return;

        CompoundTag cfg = wandStack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg == null) return;

        ItemStackHandler handler = new ItemStackHandler(MAX_SLOTS);
        if (cfg.contains("items")) {
            handler.deserializeNBT(minecraft.level.registryAccess(), cfg.getCompound("items"));
        }

        CompoundTag fluids = cfg.contains("fluids") ? cfg.getCompound("fluids") : new CompoundTag();

        int slotCount = handler.getSlots();
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            String fluidId = fluids.getString(Integer.toString(i));

            if (!stack.isEmpty()) {
                 // Simplified item display for now, assuming standard items
                 slots.add(new SlotData(i, stack, stack.getHoverName().getString()));
            } else if (fluidId != null && !fluidId.isEmpty()) {
                var rl = net.minecraft.resources.ResourceLocation.tryParse(fluidId);
                if (rl != null) {
                    var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(rl);
                    if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                        var aeFluidKey = appeng.api.stacks.AEFluidKey.of(fluid);
                        var genericStack = new appeng.api.stacks.GenericStack(
                                aeFluidKey, appeng.api.stacks.AEFluidKey.AMOUNT_BLOCK);
                        ItemStack displayStack = appeng.api.stacks.GenericStack.wrapInItemStack(genericStack);
                        slots.add(new SlotData(i, displayStack,
                                aeFluidKey.getDisplayName().getString()));
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        if (totalTime < OPEN_ANIMATION_LENGTH) {
            extraTick++;
        }

        // Check raw input to ensure menu doesn't close while holding key
        boolean keyIsDown = MeptKeyBindings.OPEN_RADIAL_MENU.isDown();
        if (!keyIsDown) {
            var key = MeptKeyBindings.OPEN_RADIAL_MENU.getKey();
            long windowHandle = Minecraft.getInstance().getWindow().getWindow();
            if (key.getType() == InputConstants.Type.KEYSYM) {
                keyIsDown = InputConstants.isKeyDown(windowHandle, key.getValue());
            } else if (key.getType() == InputConstants.Type.MOUSE) {
                keyIsDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            }
        }

        if (!keyIsDown) {
            this.onClose();
        }
    }

    private void selectSlot(int slotIndex) {
        if (wandStack.isEmpty()) return;

        currentSelectedSlot = slotIndex; // Update local state for highlight
        
        CompoundTag cfg = wandStack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg == null) cfg = new CompoundTag();
        else cfg = cfg.copy();
        
        cfg.putInt("SelectedSlot", slotIndex);
        
        // Update client side item immediately
        wandStack.set(MeptDataComponents.PLACEMENT_CONFIG.get(), cfg);

        // Send to server
        PacketDistributor.sendToServer(new UpdateWandConfigPayload(cfg));

        // Show overlay
        String name = "Empty";
        for (SlotData slot : slots) {
            if (slot.index == slotIndex) {
                name = slot.name;
                break;
            }
        }
        MeptClientOverlay.showSelectedOverlay(name);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (slots.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("message.godofthings.mept.no_configured_item"), width / 2, height / 2, 0xFFFFFF);
            return;
        }

        PoseStack ms = graphics.pose();
        float openAnimation = closing ? 1.0f - totalTime / OPEN_ANIMATION_LENGTH : totalTime / OPEN_ANIMATION_LENGTH;
        float currTick = partialTicks;
        totalTime += (currTick + extraTick - prevTick) / 20f;
        extraTick = 0;
        prevTick = currTick;

        float animProgress = Mth.clamp(openAnimation, 0, 1);
        animProgress = (float) (1 - Math.pow(1 - animProgress, 3));
        
        int numberOfSlices = Math.min(MAX_SLOTS, slots.size());
        // Dynamic radius based on number of slices - reduced to 2/3 size
        float baseRadius = Math.max(40, 30 + numberOfSlices * 2);
        float radiusIn = Math.max(0.1f, baseRadius * animProgress);
        float radiusOut = radiusIn + Math.max(35, 25 + numberOfSlices * 1.5f) * animProgress;
        float itemRadius = (radiusIn + radiusOut) * 0.5f;

        int centerX = width / 2;
        int centerY = height / 2;

        double mouseAngle = Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
        double mouseDistance = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        float slot0 = (((0 - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
        if (mouseAngle < slot0) {
            mouseAngle += 360;
        }

        ms.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Determine selected item
        if (!closing) {
            selectedItem = -1;
            for (int i = 0; i < numberOfSlices; i++) {
                float sliceBorderLeft = (((i - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
                float sliceBorderRight = (((i + 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
                // Allow selection anywhere based on angle only (inside and outside the ring)
                if (mouseAngle >= sliceBorderLeft && mouseAngle < sliceBorderRight) {
                    selectedItem = i;
                    break;
                }
            }
        }

        // Draw gray background ring
        drawSlice(buffer, centerX, centerY, 9, radiusIn, radiusOut, 0, 360, 80, 80, 80, 120);

        // Only draw highlights for hovered and currently selected slices
        int mousedOverSlot = -1;
        for (int i = 0; i < numberOfSlices; i++) {
            float sliceBorderLeft = (((i - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
            float sliceBorderRight = (((i + 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
            
            // Calculate adjusted index for checking current selection
            int adjusted = ((i + (numberOfSlices / 2 + 1)) % numberOfSlices) - 1;
            adjusted = adjusted == -1 ? numberOfSlices - 1 : adjusted;
            boolean isCurrentlySelected = adjusted < slots.size() && slots.get(adjusted).index == currentSelectedSlot;
            
            if (selectedItem == i) {
                // Hovered slice - blue highlight
                drawSlice(buffer, centerX, centerY, 10, radiusIn, radiusOut, sliceBorderLeft, sliceBorderRight, 63, 161, 191, 150);
                mousedOverSlot = selectedItem;
            } else if (isCurrentlySelected) {
                // Currently selected slot - green highlight
                drawSlice(buffer, centerX, centerY, 10, radiusIn, radiusOut, sliceBorderLeft, sliceBorderRight, 80, 180, 80, 130);
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        // Draw divider lines
        buffer = tessellator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < numberOfSlices; i++) {
            float angle = (float) Math.toRadians((((i - 0.5f) / (float) numberOfSlices) + 0.25f) * 360);
            float x1 = centerX + radiusIn * (float) Math.cos(angle);
            float y1 = centerY + radiusIn * (float) Math.sin(angle);
            float x2 = centerX + radiusOut * (float) Math.cos(angle);
            float y2 = centerY + radiusOut * (float) Math.sin(angle);
            buffer.addVertex(x1, y1, 11).setColor(200, 200, 200, 100);
            buffer.addVertex(x2, y2, 11).setColor(200, 200, 200, 100);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        // Draw hovered item name
        if (mousedOverSlot != -1) {
            int adjusted = ((mousedOverSlot + (numberOfSlices / 2 + 1)) % numberOfSlices) - 1;
            adjusted = adjusted == -1 ? numberOfSlices - 1 : adjusted;
            if (adjusted >= 0 && adjusted < slots.size()) {
                graphics.drawCenteredString(font, slots.get(adjusted).name, centerX, (height - font.lineHeight) / 2, 0xFFFFFF);
            }
        }

        ms.popPose();

        // Draw item icons
        RenderSystem.disableDepthTest();
        for (int i = 0; i < numberOfSlices; i++) {
            float angle = ((i / (float) numberOfSlices) - 0.25f) * 2 * (float) Math.PI;
            if (numberOfSlices % 2 != 0) {
                angle += Math.PI / numberOfSlices;
            }
            float posX = centerX - 8 + itemRadius * (float) Math.cos(angle);
            float posY = centerY - 8 + itemRadius * (float) Math.sin(angle);

            SlotData slot = slots.get(i);
            if (!slot.displayStack.isEmpty()) {
                graphics.renderItem(slot.displayStack, (int) posX, (int) posY);
            }
        }
        // Restore depth test so we don't leak state into subsequent rendering
        RenderSystem.enableDepthTest();

        if (mousedOverSlot != -1) {
            int adjusted = ((mousedOverSlot + (numberOfSlices / 2 + 1)) % numberOfSlices) - 1;
            adjusted = adjusted == -1 ? numberOfSlices - 1 : adjusted;
            selectedItem = adjusted;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectedItem >= 0 && selectedItem < slots.size()) {
            selectSlot(slots.get(selectedItem).index);
        }
        return true;
    }

    private void drawSlice(BufferBuilder buffer, float x, float y, float z, float radiusIn, float radiusOut, 
                           float startAngle, float endAngle, int r, int g, int b, int a) {
        float angle = endAngle - startAngle;
        int sections = Math.max(1, Mth.ceil(angle / PRECISION));

        for (int i = 0; i < sections; i++) {
            float angle1 = (float) Math.toRadians(startAngle + (i / (float) sections) * angle);
            float angle2 = (float) Math.toRadians(startAngle + ((i + 1) / (float) sections) * angle);

            float x1In = x + radiusIn * (float) Math.cos(angle1);
            float y1In = y + radiusIn * (float) Math.sin(angle1);
            float x1Out = x + radiusOut * (float) Math.cos(angle1);
            float y1Out = y + radiusOut * (float) Math.sin(angle1);
            float x2In = x + radiusIn * (float) Math.cos(angle2);
            float y2In = y + radiusIn * (float) Math.sin(angle2);
            float x2Out = x + radiusOut * (float) Math.cos(angle2);
            float y2Out = y + radiusOut * (float) Math.sin(angle2);

            buffer.addVertex(x1In, y1In, z).setColor(r, g, b, a);
            buffer.addVertex(x1Out, y1Out, z).setColor(r, g, b, a);
            buffer.addVertex(x2Out, y2Out, z).setColor(r, g, b, a);
            buffer.addVertex(x2In, y2In, z).setColor(r, g, b, a);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
