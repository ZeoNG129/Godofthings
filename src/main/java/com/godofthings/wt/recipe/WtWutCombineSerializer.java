package com.godofthings.wt.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

import com.godofthings.wt.core.WtTerminalDefinition;

/**
 * 双终端合并配方序列化器（照抄 ae2wtlib 的 {@code CombineSerializer}，[本地改动]
 * WTDefinition.CODEC/STREAM_CODEC 换本模组的 terminalName 字符串编解码）。
 */
public class WtWutCombineSerializer implements net.minecraft.world.item.crafting.RecipeSerializer<WtWutCombine>
{
    public static final String NAME = "wutfree_combine";
    private static final MapCodec<WtWutCombine> MAP_CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    Ingredient.CODEC.fieldOf("terminalA").forGetter(WtWutCombine::getTerminalA),
                    Ingredient.CODEC.fieldOf("terminalB").forGetter(WtWutCombine::getTerminalB),
                    WtTerminalDefinitionHolder.CODEC.fieldOf("terminalAName")
                            .forGetter(WtWutCombine::getTerminalADefinition),
                    WtTerminalDefinitionHolder.CODEC.fieldOf("terminalBName")
                            .forGetter(WtWutCombine::getTerminalBDefinition))
                    .apply(builder, WtWutCombine::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, WtWutCombine> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WtWutCombine::getTerminalA,
            Ingredient.CONTENTS_STREAM_CODEC, WtWutCombine::getTerminalB,
            WtTerminalDefinitionHolder.STREAM_CODEC, WtWutCombine::getTerminalADefinition,
            WtTerminalDefinitionHolder.STREAM_CODEC, WtWutCombine::getTerminalBDefinition,
            WtWutCombine::new);

    @Override
    public MapCodec<WtWutCombine> codec()
    {
        return MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, WtWutCombine> streamCodec()
    {
        return STREAM_CODEC;
    }
}
