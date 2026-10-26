package com.godofthings.elevator.client;

import com.godofthings.Godofthings;
import com.godofthings.elevator.block.ElevatorBlockEntity;
import com.godofthings.elevator.block.ElevatorContainer;
import com.godofthings.elevator.network.RemoveCamoPayload;
import com.godofthings.elevator.network.SetArrowPayload;
import com.godofthings.elevator.network.SetDirectionalPayload;
import com.godofthings.elevator.network.SetFacingPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * 电梯设置界面：有方向开关 / 隐藏箭头 / 取消伪装 / 四向朝向按钮。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.client.gui.ElevatorScreen}（MIT），逐行对照编写。
 * 图片尺寸与原版一致（200×100），贴图为本模组 {@code textures/gui/elevator_gui.png}。</p>
 */
public class ElevatorScreen extends AbstractContainerScreen<ElevatorContainer>
{
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "textures/gui/elevator_gui.png");

    private final ElevatorBlockEntity tile;
    private final Direction playerFacing;

    private Checkbox dirButton;
    private Checkbox hideArrowButton;
    private Button resetCamoButton;

    public ElevatorScreen(ElevatorContainer container, Inventory inv, Component titleIn)
    {
        super(container, inv, titleIn);
        this.imageWidth = 200;
        this.imageHeight = 100;
        this.tile = container.getTile();
        this.playerFacing = container.getPlayerFacing();
    }

    @Override
    protected void init()
    {
        super.init();

        // 有方向开关
        dirButton = Checkbox.builder(Component.translatable("block.godofthings.elevator.directional"), this.font)
                .pos(this.leftPos + 8, this.topPos + 25)
                .selected(this.tile.getBlockState().getValue(com.godofthings.elevator.ElevatorBlockBase.DIRECTIONAL))
                .onValueChange((checkbox, selected) -> PacketDistributor.sendToServer(
                        new SetDirectionalPayload(selected, this.tile.getBlockPos())))
                .build();
        this.addRenderableWidget(dirButton);

        // 隐藏箭头开关
        hideArrowButton = Checkbox.builder(Component.translatable("block.godofthings.elevator.hide_arrow"), this.font)
                .pos(this.leftPos + 8, this.topPos + 50)
                .selected(!this.tile.getBlockState().getValue(com.godofthings.elevator.ElevatorBlockBase.SHOW_ARROW))
                .onValueChange((checkbox, selected) -> PacketDistributor.sendToServer(
                        new SetArrowPayload(!selected, this.tile.getBlockPos())))
                .build();
        hideArrowButton.visible = this.tile.getBlockState().getValue(com.godofthings.elevator.ElevatorBlockBase.DIRECTIONAL);
        this.addRenderableWidget(hideArrowButton);

        // 取消伪装
        resetCamoButton = Button.builder(Component.translatable("block.godofthings.elevator.reset_camo"),
                        b -> PacketDistributor.sendToServer(new RemoveCamoPayload(this.tile.getBlockPos())))
                .pos(this.leftPos + 8, this.topPos + 75)
                .size(110, 20)
                .build();
        this.addRenderableWidget(resetCamoButton);
        resetCamoButton.active = this.tile.getHeldState() != null;

        // 四向朝向按钮（按玩家朝向旋转排布）
        Direction[] dirs = orderFacing();
        int[][] positions = facingPositions();
        for (int i = 0; i < 4; i++)
        {
            final Direction dir = dirs[i];
            Button btn = Button.builder(Component.literal(shortName(dir)), b -> PacketDistributor.sendToServer(
                            new SetFacingPayload(dir, this.tile.getBlockPos())))
                    .pos(this.leftPos + positions[i][0], this.topPos + positions[i][1])
                    .size(20, 20)
                    .build();
            btn.visible = this.tile.getBlockState().getValue(com.godofthings.elevator.ElevatorBlockBase.DIRECTIONAL);
            btn.active = this.tile.getBlockState().getValue(com.godofthings.elevator.ElevatorBlockBase.FACING) != dir;
            this.addRenderableWidget(btn);
        }
    }

    /** 按玩家朝向把四个方向排成 上/右/下/左（与上游 FacingControllerWrapper 的 rotate 一致）。 */
    private Direction[] orderFacing()
    {
        Direction[] base = { Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST };
        int shift = this.playerFacing.get2DDataValue();
        Direction[] out = new Direction[4];
        for (int i = 0; i < 4; i++)
        {
            out[i] = base[(i - shift + 4) % 4];
        }
        return out;
    }

    /** 四个按钮的位置：上 / 右 / 下 / 左，以 x=120,y=20 为基准。 */
    private int[][] facingPositions()
    {
        int x = 120;
        int y = 20;
        return new int[][] { { x + 20, y }, { x + 40, y + 20 }, { x + 20, y + 40 }, { x, y + 20 } };
    }

    private String shortName(Direction dir)
    {
        return switch (dir)
        {
            case NORTH -> Component.translatable("block.godofthings.elevator.directional_north").getString();
            case SOUTH -> Component.translatable("block.godofthings.elevator.directional_south").getString();
            case EAST -> Component.translatable("block.godofthings.elevator.directional_east").getString();
            default -> Component.translatable("block.godofthings.elevator.directional_west").getString();
        };
    }

    @Override
    public void containerTick()
    {
        super.containerTick();
        // 服务端状态变了（开关/伪装被改）就重建控件，保证勾选状态与服务端一致。
        // 本版本的 Checkbox 只有 selected() 没有 setter，重建比硬改字段更稳。
        boolean directional = this.tile.getBlockState().getValue(com.godofthings.elevator.ElevatorBlockBase.DIRECTIONAL);
        boolean showArrow = this.tile.getBlockState().getValue(com.godofthings.elevator.ElevatorBlockBase.SHOW_ARROW);
        boolean hasCamo = this.tile.getHeldState() != null;

        if (this.dirButton.selected() != directional
                || this.hideArrowButton.selected() != !showArrow
                || this.resetCamoButton.active != hasCamo)
        {
            this.rebuildWidgets();
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        gui.blit(GUI_TEXTURE, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY)
    {
        gui.drawString(this.font, this.title, 8, 8, 14737632, false);
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
