package com.godofthings.aalink;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * 无线网格桥接的自动连接开关（移植自 AALink 的 AALinkCommands）。
 *
 * <p>指令入口已并入本模组自己的根指令，不再注册独立的 {@code /aalink}：
 * {@code /godofthings aalink on} 开启、{@code /godofthings aalink off} 关闭。</p>
 *
 * <p>只保留 on / off 两个子命令；原上游的 toggle / status 由 on/off 的回显覆盖，
 * 避免短名指令与其他模组冲突。</p>
 */
public final class AaLinkCommands
{
    private AaLinkCommands()
    {
    }

    /** 由 {@link com.godofthings.command.GodofthingsCommand} 的 aalink 子指令调用。 */
    public static int setEnabled(CommandSourceStack source, boolean value)
    {
        AaLinkBridge.setEnabled(value);
        source.sendSuccess(() -> Component.translatable("message.godofthings.aalink.auto_state",
                Component.translatable(value
                        ? "message.godofthings.aalink.on"
                        : "message.godofthings.aalink.off")), true);
        return 1;
    }
}
