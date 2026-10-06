package com.godofthings.measurement;

// 基于 Measurements（作者 Mrbysco，MIT License）移植并改名「神之测量」：
// https://github.com/Mrbysco/measurements
// 卷尺物品：右击方块标记测量点。测量逻辑纯客户端（见 MeasureBoxHandler），
// 服务端不参与、不产生任何数据。

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.util.FakePlayer;

public class GodMeasureItem extends Item
{
    public GodMeasureItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();

        // 上游 isValidUser 逻辑内联：真人 + 客户端才测量（假玩家 / 服务端跳过）
        if (player != null && !(player instanceof FakePlayer) && player.level().isClientSide)
        {
            return MeasureBoxHandler.addBox(player, context.getClickedPos());
        }

        return super.useOn(context);
    }
}
