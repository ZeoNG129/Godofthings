package com.godofthings.elevator.block;

import com.godofthings.elevator.ElevatorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 电梯设置界面的容器（无槽位，只承载方块坐标与玩家朝向）。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.level.ElevatorContainer}（MIT），逐行对照编写。</p>
 */
public class ElevatorContainer extends AbstractContainerMenu
{
    private final Direction playerFacing;
    private ElevatorBlockEntity elevatorTile;
    private final BlockPos pos;

    public ElevatorContainer(int id, BlockPos pos, Player player)
    {
        super(ElevatorRegistry.ELEVATOR_CONTAINER.get(), id);

        if (player.level().getBlockEntity(pos) instanceof ElevatorBlockEntity tile)
        {
            elevatorTile = tile;
        }
        playerFacing = player.getDirection();
        this.pos = pos;
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int id)
    {
        return ItemStack.EMPTY; // 本界面没有物品槽位
    }

    @Override
    public boolean stillValid(@NotNull Player playerIn)
    {
        return stillValid(ContainerLevelAccess.create(playerIn.level(), elevatorTile.getBlockPos()),
                playerIn, elevatorTile.getBlockState().getBlock());
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public ElevatorBlockEntity getTile()
    {
        return elevatorTile;
    }

    public Direction getPlayerFacing()
    {
        return playerFacing;
    }
}
