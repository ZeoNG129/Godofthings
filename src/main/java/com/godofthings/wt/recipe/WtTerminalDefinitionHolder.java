package com.godofthings.wt.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.godofthings.wt.core.WtTerminalDefinition;

/**
 * 终端定义的编解码持有器（照抄 ae2wtlib 的 {@code WTDefinition.CODEC / STREAM_CODEC}，
 * [本地改动] 用 terminalName 字符串在 {@link WtTerminalDefinition} 注册表里查定义）。
 */
public final class WtTerminalDefinitionHolder
{
    private WtTerminalDefinitionHolder()
    {
    }

    public static final Codec<WtTerminalDefinition> CODEC = Codec.STRING.comapFlatMap(s ->
    {
        try
        {
            return DataResult.success(WtTerminalDefinition.of(s));
        } catch (IllegalArgumentException e)
        {
            return DataResult.error(() -> "WtTerminalDefinition " + s + " does not exist");
        }
    }, WtTerminalDefinition::terminalName);

    public static final StreamCodec<io.netty.buffer.ByteBuf, WtTerminalDefinition> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8
                    .map(WtTerminalDefinition::of, WtTerminalDefinition::terminalName);
}
