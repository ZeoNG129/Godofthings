package com.godofthings.torcherino.client.screen;

import com.godofthings.torcherino.Torcherino;
import com.godofthings.torcherino.api.Tier;
import com.godofthings.torcherino.client.screen.widgets.GradatedSliderWidget;
import com.godofthings.torcherino.client.screen.widgets.StateButtonWidget;
import com.godofthings.torcherino.network.TorcherinoNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * 加速火把调节界面：速度 / X / Z / Y 范围滑块 + 红石模式按钮。
 * 移植自 Torcherino（MIT License）。
 */
public final class TorcherinoScreen extends Screen
{
    private static final ResourceLocation SCREEN_TEXTURE =
            ResourceLocation.tryBuild(Torcherino.MOD_ID, "textures/screens/torcherino.png");
    private static final int screenWidth = 245;
    private static final int screenHeight = 123;

    private final BlockPos blockPos;
    private final Tier tier;
    private final Component cached_title;
    private int xRange, zRange, yRange, speed, redstoneMode, left, top;

    public TorcherinoScreen(Component title, int xRange, int zRange, int yRange, int speed, int redstoneMode,
                            BlockPos pos, ResourceLocation tierID)
    {
        super(title);
        this.tier = Torcherino.getTier(tierID);
        this.blockPos = pos;
        this.xRange = xRange;
        this.zRange = zRange;
        this.yRange = yRange;
        this.speed = speed == 0 ? 1 : speed;
        this.redstoneMode = redstoneMode;
        this.cached_title = title;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    protected void init()
    {
        left = (width - screenWidth) / 2;
        top = (height - screenHeight) / 2;
        Tier safeTier = tier == null ? new Tier(4, 4, 1) : tier;

        this.addRenderableWidget(new GradatedSliderWidget(left + 8, top + 20, 205,
                (double) (speed - 1) / (safeTier.maxSpeed() - 1), safeTier.maxSpeed())
        {
            @Override
            protected void updateMessage()
            {
                this.setMessage(Component.translatable("gui.godofthings.torcherino.speed", 100 * TorcherinoScreen.this.speed));
            }

            @Override
            protected void applyValue()
            {
                TorcherinoScreen.this.speed = 1 + (int) Math.round(value * (safeTier.maxSpeed() - 1));
                value = (double) (speed - 1) / (safeTier.maxSpeed() - 1);
            }
        });
        this.addRenderableWidget(new GradatedSliderWidget(left + 8, top + 45, 205,
                (double) xRange / safeTier.xzRange(), safeTier.xzRange())
        {
            @Override
            protected void updateMessage()
            {
                this.setMessage(Component.translatable("gui.godofthings.torcherino.x_range", TorcherinoScreen.this.xRange * 2 + 1));
            }

            @Override
            protected void applyValue()
            {
                TorcherinoScreen.this.xRange = (int) Math.round(value * safeTier.xzRange());
                value = (double) xRange / safeTier.xzRange();
            }
        });
        this.addRenderableWidget(new GradatedSliderWidget(left + 8, top + 70, 205,
                (double) zRange / safeTier.xzRange(), safeTier.xzRange())
        {
            @Override
            protected void updateMessage()
            {
                this.setMessage(Component.translatable("gui.godofthings.torcherino.z_range", TorcherinoScreen.this.zRange * 2 + 1));
            }

            @Override
            protected void applyValue()
            {
                TorcherinoScreen.this.zRange = (int) Math.round(value * safeTier.xzRange());
                value = (double) zRange / safeTier.xzRange();
            }
        });
        this.addRenderableWidget(new GradatedSliderWidget(left + 8, top + 95, 205,
                (double) yRange / safeTier.yRange(), safeTier.yRange())
        {
            @Override
            protected void updateMessage()
            {
                this.setMessage(Component.translatable("gui.godofthings.torcherino.y_range", TorcherinoScreen.this.yRange * 2 + 1));
            }

            @Override
            protected void applyValue()
            {
                TorcherinoScreen.this.yRange = (int) Math.round(value * safeTier.yRange());
                value = (double) yRange / safeTier.yRange();
            }
        });
        this.addRenderableWidget(new StateButtonWidget(this, left + 217, top + 20, font)
        {
            ItemStack buttonIcon;

            @Override
            protected void initialize()
            {
                this.setButtonMessage();
                this.setButtonIcon();
            }

            private void setButtonMessage()
            {
                String translationKey = switch (TorcherinoScreen.this.redstoneMode)
                {
                    case 0 -> "gui.godofthings.torcherino.mode.normal";
                    case 1 -> "gui.godofthings.torcherino.mode.inverted";
                    case 2 -> "gui.godofthings.torcherino.mode.ignored";
                    case 3 -> "gui.godofthings.torcherino.mode.off";
                    default -> "gui.godofthings.torcherino.mode.error";
                };
                this.setNarrationMessage(Component.translatable("gui.godofthings.torcherino.mode",
                        Component.translatable(translationKey)));
            }

            private void setButtonIcon()
            {
                switch (TorcherinoScreen.this.redstoneMode)
                {
                    case 0 -> this.buttonIcon = new ItemStack(Items.REDSTONE);
                    case 1 -> this.buttonIcon = new ItemStack(Items.REDSTONE_TORCH);
                    case 2 -> this.buttonIcon = new ItemStack(Items.GUNPOWDER);
                    case 3 -> this.buttonIcon = new ItemStack(Items.REDSTONE_LAMP);
                    default -> this.buttonIcon = new ItemStack(Items.FURNACE);
                }
            }

            @Override
            protected void nextState()
            {
                TorcherinoScreen.this.redstoneMode = (TorcherinoScreen.this.redstoneMode + 1) % 4;
                this.initialize();
            }

            @Override
            protected ItemStack getButtonIcon()
            {
                return buttonIcon;
            }
        });
    }

    @Override
    public void render(GuiGraphics context, int x, int y, float partialTicks)
    {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        RenderSystem.setShaderTexture(0, SCREEN_TEXTURE);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        context.blit(SCREEN_TEXTURE, left, top, 0, 0, screenWidth, screenHeight);
        context.drawString(font, cached_title.getVisualOrderText(), (int) ((width - font.width(cached_title)) / 2.0f), top + 6, 4210752, false);
        super.render(context, x, y, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || minecraft.options.keyInventory.matches(keyCode, 0))
        {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose()
    {
        TorcherinoNetwork.sendUpdate(blockPos, xRange, zRange, yRange, speed, redstoneMode);
        super.onClose();
    }
}
