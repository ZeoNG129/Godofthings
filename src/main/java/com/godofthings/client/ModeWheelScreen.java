package com.godofthings.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.godofthings.modes.ModeManager;
import com.godofthings.modes.ToolMode;
import com.godofthings.network.WandMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 模式轮盘屏幕。
 * 移植自 useless_mod 的 ModeWheelScreen（1.20.1）。
 */
public class ModeWheelScreen extends Screen
{
    private final ModeManager modeManager;
    private final ItemStack mainHandItem;

    private static final float PRECISION = 5.0f;
    private static final int MAX_SLOTS = 18;
    private static final float OPEN_ANIMATION_LENGTH = 0.25f;

    private float totalTime;
    private float prevTick;
    private float extraTick;
    private boolean closing = false;

    private final List<ModeData> leftModes = new ArrayList<>();
    private final List<ModeData> middleModes = new ArrayList<>();
    private final List<ModeData> rightModes = new ArrayList<>();

    public record ModeData(ToolMode mode, Component name, boolean isActive) {}

    private static final float DISC_RADIUS = 60.0f;
    private static final float DISC_SPACING = 150.0f;

    public ModeWheelScreen(ModeManager modeManager, ItemStack mainHandItem)
    {
        super(Component.literal("Mode Wheel"));
        this.modeManager = modeManager;
        this.mainHandItem = mainHandItem;
        this.minecraft = Minecraft.getInstance();
        loadModes();
    }

    private void loadModes()
    {
        leftModes.clear();
        middleModes.clear();
        rightModes.clear();

        for (ToolMode mode : ToolMode.values())
        {
            if (mode != ToolMode.CHAIN_MINING)
            {
                boolean shouldAddMode = true;
                if (mode == ToolMode.OMNITOOL_MODE)
                {
                    net.minecraft.resources.ResourceLocation omnitoolId =
                            net.minecraft.resources.ResourceLocation.tryParse("omnitools:omni_wrench");
                    shouldAddMode = net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(omnitoolId);
                }

                if (shouldAddMode)
                {
                    boolean isActive = modeManager.isModeActive(mode);
                    Component name = isActive
                            ? Component.translatable("tooltip.godofthings.mode_with_status", mode.getTooltip(), Component.translatable("text.godofthings.active"))
                            : mode.getTooltip();
                    ModeData modeData = new ModeData(mode, name, isActive);

                    switch (mode)
                    {
                        case SILK_TOUCH, FORTUNE -> leftModes.add(modeData);
                        case WRENCH_MODE, MALLET_MODE, CROWBAR_MODE, HAMMER_MODE, SCREWDRIVER_MODE, OMNITOOL_MODE -> middleModes.add(modeData);
                        case FORCE_MINING, AE_STORAGE_PRIORITY, ENHANCED_CHAIN_MINING -> rightModes.add(modeData);
                        default -> {}
                    }
                }
            }
        }
    }

    @Override
    public void tick()
    {
        if (totalTime < OPEN_ANIMATION_LENGTH)
        {
            extraTick++;
        }

        if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_G))
        {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        if (leftModes.isEmpty() && middleModes.isEmpty() && rightModes.isEmpty())
        {
            graphics.drawCenteredString(font, Component.translatable("message.godofthings.no_modes"), width / 2, height / 2, 0xFFFFFF);
            return;
        }

        PoseStack ms = graphics.pose();
        float openAnimation = closing ? 1.0f - totalTime / OPEN_ANIMATION_LENGTH : totalTime / OPEN_ANIMATION_LENGTH;
        float currTick = minecraft.getFrameTime();
        totalTime += (currTick + extraTick - prevTick) / 20f;
        extraTick = 0;
        prevTick = currTick;

        float animProgress = Mth.clamp(openAnimation, 0, 1);
        animProgress = (float) (1 - Math.pow(1 - animProgress, 3));

        int centerY = height / 2;
        int leftCenterX = (int) (width / 2 - DISC_SPACING);
        int middleCenterX = width / 2;
        int rightCenterX = (int) (width / 2 + DISC_SPACING);

        ms.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        drawDisc(buffer, leftCenterX, centerY, DISC_RADIUS, leftModes, mouseX, mouseY, animProgress);
        drawDisc(buffer, middleCenterX, centerY, DISC_RADIUS, middleModes, mouseX, mouseY, animProgress);
        drawDisc(buffer, rightCenterX, centerY, DISC_RADIUS, rightModes, mouseX, mouseY, animProgress);

        BufferUploader.drawWithShader(buffer.end());

        buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        drawDiscDividers(buffer, leftCenterX, centerY, DISC_RADIUS, leftModes.size(), animProgress);
        drawDiscDividers(buffer, middleCenterX, centerY, DISC_RADIUS, middleModes.size(), animProgress);
        drawDiscDividers(buffer, rightCenterX, centerY, DISC_RADIUS, rightModes.size(), animProgress);

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        drawModeNames(graphics, leftCenterX, centerY, DISC_RADIUS, leftModes, animProgress);
        drawModeNames(graphics, middleCenterX, centerY, DISC_RADIUS, middleModes, animProgress);
        drawModeNames(graphics, rightCenterX, centerY, DISC_RADIUS, rightModes, animProgress);

        drawHoverText(graphics, leftCenterX, centerY, DISC_RADIUS, leftModes, mouseX, mouseY);
        drawHoverText(graphics, middleCenterX, centerY, DISC_RADIUS, middleModes, mouseX, mouseY);
        drawHoverText(graphics, rightCenterX, centerY, DISC_RADIUS, rightModes, mouseX, mouseY);

        ms.popPose();
    }

    private void drawDisc(BufferBuilder buffer, int centerX, int centerY, float radius,
                          List<ModeData> modes, int mouseX, int mouseY, float animProgress)
    {
        if (modes.isEmpty()) return;

        float radiusIn = Math.max(0.1f, radius * 0.4f * animProgress);
        float radiusOut = Math.max(0.1f, radius * animProgress);
        int numberOfSlices = modes.size();

        drawSlice(buffer, centerX, centerY, 9, radiusIn, radiusOut, 0, 360, 80, 80, 80, 120);

        double mouseAngle = Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
        double mouseDistance = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        float slot0 = (((0 - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
        if (mouseAngle < slot0)
        {
            mouseAngle += 360;
        }

        int selectedItem = -1;
        for (int i = 0; i < numberOfSlices; i++)
        {
            float sliceBorderLeft = (((i - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
            float sliceBorderRight = (((i + 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
            if (mouseAngle >= sliceBorderLeft && mouseAngle < sliceBorderRight &&
                    mouseDistance >= radiusIn && mouseDistance < radiusOut)
            {
                selectedItem = i;
                break;
            }
        }

        for (int i = 0; i < numberOfSlices; i++)
        {
            float sliceBorderLeft = (((i - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
            float sliceBorderRight = (((i + 0.5f) / (float) numberOfSlices) + 0.25f) * 360;

            if (selectedItem == i)
            {
                drawSlice(buffer, centerX, centerY, 10, radiusIn, radiusOut, sliceBorderLeft, sliceBorderRight, 63, 161, 191, 150);
            }
            else
            {
                int adjusted = ((i + (numberOfSlices / 2 + 1)) % numberOfSlices) - 1;
                adjusted = adjusted == -1 ? numberOfSlices - 1 : adjusted;

                if (adjusted >= 0 && adjusted < modes.size() && modes.get(adjusted).isActive())
                {
                    drawSlice(buffer, centerX, centerY, 10, radiusIn, radiusOut, sliceBorderLeft, sliceBorderRight, 80, 180, 80, 130);
                }
            }
        }
    }

    private void drawDiscDividers(BufferBuilder buffer, int centerX, int centerY, float radius,
                                  int sliceCount, float animProgress)
    {
        if (sliceCount <= 0) return;

        float radiusIn = radius * 0.4f * animProgress;
        float radiusOut = radius * animProgress;

        for (int i = 0; i < sliceCount; i++)
        {
            float angle = (float) Math.toRadians((((i - 0.5f) / (float) sliceCount) + 0.25f) * 360);
            float x1 = centerX + radiusIn * (float) Math.cos(angle);
            float y1 = centerY + radiusIn * (float) Math.sin(angle);
            float x2 = centerX + radiusOut * (float) Math.cos(angle);
            float y2 = centerY + radiusOut * (float) Math.sin(angle);
            buffer.vertex(x1, y1, 11).color(200, 200, 200, 100).endVertex();
            buffer.vertex(x2, y2, 11).color(200, 200, 200, 100).endVertex();
        }
    }

    private void drawModeNames(GuiGraphics graphics, int centerX, int centerY, float radius,
                               List<ModeData> modes, float animProgress)
    {
        if (modes.isEmpty()) return;

        float textRadius = radius * 0.7f * animProgress;
        int numberOfSlices = modes.size();

        for (int i = 0; i < numberOfSlices; i++)
        {
            float angle = ((i / (float) numberOfSlices) - 0.25f) * 2 * (float) Math.PI;
            if (numberOfSlices % 2 != 0)
            {
                angle += Math.PI / numberOfSlices;
            }

            Component name = modes.get(i).mode.getTooltip();
            int nameWidth = font.width(name);
            float textX = centerX - nameWidth / 2 + textRadius * (float) Math.cos(angle);
            float textY = centerY - font.lineHeight / 2 + textRadius * (float) Math.sin(angle);

            graphics.drawString(font, name, (int) textX, (int) textY, 0xFFFFFF, false);
        }
    }

    private void drawHoverText(GuiGraphics graphics, int centerX, int centerY, float radius,
                               List<ModeData> modes, int mouseX, int mouseY)
    {
        if (modes.isEmpty()) return;

        float radiusIn = radius * 0.4f;
        float radiusOut = radius;
        int numberOfSlices = modes.size();

        double mouseAngle = Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
        double mouseDistance = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        float slot0 = (((0 - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
        if (mouseAngle < slot0)
        {
            mouseAngle += 360;
        }

        int selectedItem = -1;
        for (int i = 0; i < numberOfSlices; i++)
        {
            float sliceBorderLeft = (((i - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
            float sliceBorderRight = (((i + 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
            if (mouseAngle >= sliceBorderLeft && mouseAngle < sliceBorderRight &&
                    mouseDistance >= radiusIn && mouseDistance < radiusOut)
            {
                selectedItem = i;
                break;
            }
        }

        if (selectedItem >= 0 && selectedItem < modes.size())
        {
            int adjusted = ((selectedItem + (numberOfSlices / 2 + 1)) % numberOfSlices) - 1;
            adjusted = adjusted == -1 ? numberOfSlices - 1 : adjusted;

            if (adjusted >= 0 && adjusted < modes.size())
            {
                Component name = modes.get(adjusted).name;
                int nameWidth = font.width(name);
                graphics.drawString(font, name, centerX - nameWidth / 2, centerY - font.lineHeight / 2, 0xFFFFFF, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int centerY = height / 2;
        int leftCenterX = (int) (width / 2 - DISC_SPACING);
        int middleCenterX = width / 2;
        int rightCenterX = (int) (width / 2 + DISC_SPACING);

        if (checkDiscClick((int) mouseX, (int) mouseY, leftCenterX, centerY, DISC_RADIUS, leftModes)) return true;
        if (checkDiscClick((int) mouseX, (int) mouseY, middleCenterX, centerY, DISC_RADIUS, middleModes)) return true;
        if (checkDiscClick((int) mouseX, (int) mouseY, rightCenterX, centerY, DISC_RADIUS, rightModes)) return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean checkDiscClick(int mouseX, int mouseY, int centerX, int centerY,
                                   float radius, List<ModeData> modes)
    {
        if (modes.isEmpty()) return false;

        float radiusIn = radius * 0.4f;
        float radiusOut = radius;
        int numberOfSlices = modes.size();

        double mouseAngle = Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
        double mouseDistance = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        float slot0 = (((0 - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
        if (mouseAngle < slot0)
        {
            mouseAngle += 360;
        }

        if (mouseDistance < radiusIn || mouseDistance > radiusOut)
        {
            return false;
        }

        for (int i = 0; i < numberOfSlices; i++)
        {
            float sliceBorderLeft = (((i - 0.5f) / (float) numberOfSlices) + 0.25f) * 360;
            float sliceBorderRight = (((i + 0.5f) / (float) numberOfSlices) + 0.25f) * 360;

            if (mouseAngle >= sliceBorderLeft && mouseAngle < sliceBorderRight)
            {
                int adjusted = ((i + (numberOfSlices / 2 + 1)) % numberOfSlices) - 1;
                adjusted = adjusted == -1 ? numberOfSlices - 1 : adjusted;

                if (adjusted >= 0 && adjusted < modes.size())
                {
                    handleModeClick(modes.get(adjusted).mode);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == InputConstants.KEY_ESCAPE)
        {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return true;
    }

    private void handleModeClick(ToolMode mode)
    {
        WandMessages.sendModeSwitch(mode);
        modeManager.toggleMode(mode);
        onClose();
    }

    private void drawSlice(BufferBuilder buffer, float x, float y, float z, float radiusIn, float radiusOut,
                           float startAngle, float endAngle, int r, int g, int b, int a)
    {
        float angle = endAngle - startAngle;
        int sections = Math.max(1, Mth.ceil(angle / PRECISION));

        for (int i = 0; i < sections; i++)
        {
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

            buffer.vertex(x1In, y1In, z).color(r, g, b, a).endVertex();
            buffer.vertex(x1Out, y1Out, z).color(r, g, b, a).endVertex();
            buffer.vertex(x2Out, y2Out, z).color(r, g, b, a).endVertex();
            buffer.vertex(x2In, y2In, z).color(r, g, b, a).endVertex();
        }
    }

    public static void show(ModeManager modeManager, ItemStack mainHandItem)
    {
        Minecraft.getInstance().setScreen(new ModeWheelScreen(modeManager, mainHandItem));
    }
}
