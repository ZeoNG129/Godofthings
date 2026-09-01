package com.godofthings.command;

import com.godofthings.Godofthings;
import com.godofthings.waypoint.Waypoint;
import com.godofthings.waypoint.WaypointData;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 传送点指令：
 * - /setpoint <name>：把当前位置（含面对方向）保存为传送点
 * - /point <name>：传送到指定传送点（跨维度，恢复面对方向）
 * 无数量上限、无冷却。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class PointCommands
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(
                Commands.literal("setpoint")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> setPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        event.getDispatcher().register(
                Commands.literal("point")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> teleport(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
    }

    private static int setPoint(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        Waypoint wp = new Waypoint();
        wp.name = name;
        wp.dimension = player.level().dimension().location().toString();
        wp.x = player.getX();
        wp.y = player.getY();
        wp.z = player.getZ();
        wp.yaw = player.getYRot();
        wp.pitch = player.getXRot();
        WaypointData.get(source.getServer()).set(wp);
        source.sendSuccess(() -> Component.literal("已设置传送点 " + name).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int teleport(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        Waypoint wp = WaypointData.get(source.getServer()).get(name);
        if (wp == null)
        {
            source.sendFailure(Component.literal("传送点不存在：" + name));
            return 0;
        }
        if (!WaypointData.teleport(player, wp))
        {
            source.sendFailure(Component.literal("目标维度不存在：" + wp.dimension));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已传送到 " + name).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
