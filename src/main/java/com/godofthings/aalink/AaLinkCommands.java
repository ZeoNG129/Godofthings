package com.godofthings.aalink;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * AALink 指令（移植自 AALink 的 AALinkCommands）：远程控制自动连接开关。
 *
 * <p>用法：</p>
 * <ul>
 *   <li>{@code /godofthings aalink on}     开启自动连接</li>
 *   <li>{@code /godofthings aalink off}    关闭自动连接（已建立的连接保持不断开）</li>
 *   <li>{@code /godofthings aalink toggle} 切换开关</li>
 *   <li>{@code /godofthings aalink status} 查看当前开关状态</li>
 * </ul>
 *
 * <p>挂在模组自己的 {@code /godofthings} 根指令下，避免与其他模组抢 {@code /aalink}
 * 这个短名。事件订阅由 {@link com.godofthings.Godofthings} 构造器注册。</p>
 */
public final class AaLinkCommands
{
    private AaLinkCommands()
    {
    }

    /** 由 Godofthings 构造器调用，挂到游戏事件总线（RegisterCommandsEvent）。 */
    public static void register()
    {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(AaLinkCommands.class);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("aalink")
                .requires(source -> source.hasPermission(2)) // 管理员权限
                .then(Commands.literal("on").executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(Commands.literal("off").executes(ctx -> setEnabled(ctx.getSource(), false)))
                .then(Commands.literal("toggle").executes(ctx ->
                {
                    AaLinkBridge.toggleEnabled();
                    ctx.getSource().sendSuccess(() -> Component.translatable(
                            "message.godofthings.aalink.auto_state",
                            Component.translatable(AaLinkBridge.isEnabled()
                                    ? "message.godofthings.aalink.on"
                                    : "message.godofthings.aalink.off")), true);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx ->
                {
                    ctx.getSource().sendSuccess(() -> Component.translatable(
                            "message.godofthings.aalink.auto_status",
                            Component.translatable(AaLinkBridge.isEnabled()
                                    ? "message.godofthings.aalink.on"
                                    : "message.godofthings.aalink.off")), false);
                    return 1;
                })));
    }

    private static int setEnabled(CommandSourceStack source, boolean value)
    {
        AaLinkBridge.setEnabled(value);
        source.sendSuccess(() -> Component.translatable("message.godofthings.aalink.auto_state",
                Component.translatable(value
                        ? "message.godofthings.aalink.on"
                        : "message.godofthings.aalink.off")), true);
        return 1;
    }
}
