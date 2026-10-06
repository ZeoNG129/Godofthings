package com.godofthings.measurement;

// 基于 Measurements（作者 Mrbysco，MIT License）移植并改名「神之测量」：
// https://github.com/Mrbysco/measurements
// 测量框列表（纯客户端）：第一次右击放起点，第二次右击定终点并完结；
// 再右击开始下一个框；潜行右击撤销上一个框。

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MeasureBoxHandler
{
    protected static final Random random = new Random();
    private static final List<MeasureBox> boxList = new ArrayList<>();

    public static InteractionResult addBox(Player playerEntity, BlockPos blockPos)
    {
        if (playerEntity.isShiftKeyDown())
        {
            undo();
            return InteractionResult.SUCCESS;
        }

        if (!boxList.isEmpty())
        {
            MeasureBox lastBox = boxList.get(boxList.size() - 1);

            if (lastBox.isFinished())
            {
                final MeasureBox box = new MeasureBox(blockPos, playerEntity.level().dimension());
                boxList.add(box);
            }
            else
            {
                lastBox.setBlockEnd(blockPos);
                lastBox.setFinished();
            }
        }
        else
        {
            final MeasureBox box = new MeasureBox(blockPos, playerEntity.level().dimension());
            boxList.add(box);
        }

        // 返回 FAIL：客户端不发使用包，右击不会触发方块本身的功能（如开箱子）
        return InteractionResult.FAIL;
    }

    public static List<MeasureBox> getBoxList()
    {
        return boxList;
    }

    public static void undo()
    {
        if (!boxList.isEmpty())
        {
            boxList.remove(boxList.size() - 1);
        }
    }

    public static void clear()
    {
        boxList.clear();
    }
}
