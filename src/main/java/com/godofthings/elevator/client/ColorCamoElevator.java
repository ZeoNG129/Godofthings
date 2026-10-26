package com.godofthings.elevator.client;

import com.godofthings.elevator.ElevatorBlockBase;
import com.godofthings.elevator.block.ElevatorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 电梯的方块染色处理器：把染色请求转发给「伪装方块」自己的 {@link BlockColor}。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.client.ColorCamoElevator}（MIT），逐行对照。</p>
 *
 * <p>没有这一步，伪装成草方块 / 树叶 / 水这类需要染色的方块时会显示成灰色。
 * 返回 {@code -1}（即 0xFFFFFFFF）表示不染色，用贴图原色 —— 电梯本体是 16 色独立贴图，本来就不需要染色。</p>
 */
public class ColorCamoElevator implements BlockColor
{
    @Override
    public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex)
    {
        if (level == null || pos == null)
        {
            return -1;
        }
        if (state.getBlock() instanceof ElevatorBlockBase && level.getBlockEntity(pos) instanceof ElevatorBlockEntity tile)
        {
            if (tile.getHeldState() != null)
            {
                return Minecraft.getInstance().getBlockColors().getColor(tile.getHeldState(), level, pos, tintIndex);
            }
        }
        return -1;
    }
}
