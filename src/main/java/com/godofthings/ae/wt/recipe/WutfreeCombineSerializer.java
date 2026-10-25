package com.godofthings.ae.wt.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

import de.mari_023.ae2wtlib.api.registration.WTDefinition;

/**
 * {@link WutfreeCombine} 的序列化器（照抄 ae2wtlib 的 CombineSerializer，JSON 格式完全一致：
 * terminalA / terminalB / terminalAName / terminalBName）。
 */
public class WutfreeCombineSerializer implements net.minecraft.world.item.crafting.RecipeSerializer<WutfreeCombine>
{
    public static final String NAME = "wutfree_combine";
    private static final MapCodec<WutfreeCombine> MAP_CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    Ingredient.CODEC.fieldOf("terminalA").forGetter(WutfreeCombine::getTerminalA),
                    Ingredient.CODEC.fieldOf("terminalB").forGetter(WutfreeCombine::getTerminalB),
                    WTDefinition.CODEC.fieldOf("terminalAName")
                            .forGetter(WutfreeCombine::getTerminalADefinition),
                    WTDefinition.CODEC.fieldOf("terminalBName")
                            .forGetter(WutfreeCombine::getTerminalBDefinition))
                    .apply(builder, WutfreeCombine::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, WutfreeCombine> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WutfreeCombine::getTerminalA,
            Ingredient.CONTENTS_STREAM_CODEC, WutfreeCombine::getTerminalB,
            WTDefinition.STREAM_CODEC, WutfreeCombine::getTerminalADefinition,
            WTDefinition.STREAM_CODEC, WutfreeCombine::getTerminalBDefinition,
            WutfreeCombine::new);

    @Override
    public MapCodec<WutfreeCombine> codec()
    {
        return MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, WutfreeCombine> streamCodec()
    {
        return STREAM_CODEC;
    }
}
