package com.godofthings.command;

import com.godofthings.Godofthings;
import com.godofthings.waypoint.Waypoint;
import com.godofthings.waypoint.WaypointData;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 传送点指令：
 * - /setpoint <name>：把当前位置（含面对方向）保存为传送点
 * - /point <name>：传送到指定传送点（跨维度，恢复面对方向）
 * - /delpoint <name>：删除指定传送点
 * /point 与 /delpoint 均带已有点位名的 tab 补全。无数量上限、无冷却。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class PointCommands
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(
                Commands.literal("setpoint")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> setPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        event.getDispatcher().register(
                Commands.literal("point")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(PointCommands::suggestNames)
                                .executes(ctx -> teleport(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        event.getDispatcher().register(
                Commands.literal("delpoint")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(PointCommands::suggestNames)
                                .executes(ctx -> delete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
    }

    private static CompletableFuture<Suggestions> suggestNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder)
    {
        for (String name : WaypointData.get(ctx.getSource().getServer()).names())
        {
            builder.suggest(name);
        }
        return builder.buildFuture();
    }

    private static int setPoint(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        Waypoint wp = new Waypoint();
        wp.name = name.trim();
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
        Waypoint wp = WaypointData.get(source.getServer()).get(name.trim());
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

    private static int delete(CommandSourceStack source, String name)
    {
        WaypointData data = WaypointData.get(source.getServer());
        if (data.get(name.trim()) == null)
        {
            source.sendFailure(Component.literal("传送点不存在：" + name));
            return 0;
        }
        data.remove(name.trim());
        source.sendSuccess(() -> Component.literal("已删除传送点 " + name).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
