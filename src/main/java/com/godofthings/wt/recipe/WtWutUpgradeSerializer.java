package com.godofthings.wt.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

import com.godofthings.wt.core.WtTerminalDefinition;

/**
 * 单终端升级配方序列化器（照抄 ae2wtlib 的 {@code UpgradeSerializer}，[本地改动]
 * WTDefinition.CODEC/STREAM_CODEC 换本模组的 terminalName 字符串编解码）。
 */
public class WtWutUpgradeSerializer implements net.minecraft.world.item.crafting.RecipeSerializer<WtWutUpgrade>
{
    public static final String NAME = "wutfree_upgrade";
    private static final MapCodec<WtWutUpgrade> MAP_CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    Ingredient.CODEC.fieldOf("terminal").forGetter(WtWutUpgrade::getTerminal),
                    WtTerminalDefinitionHolder.CODEC.fieldOf("terminalName")
                            .forGetter(WtWutUpgrade::getTerminalDefinition))
                    .apply(builder, WtWutUpgrade::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, WtWutUpgrade> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WtWutUpgrade::getTerminal,
            WtTerminalDefinitionHolder.STREAM_CODEC, WtWutUpgrade::getTerminalDefinition,
            WtWutUpgrade::new);

    @Override
    public MapCodec<WtWutUpgrade> codec()
    {
        return MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, WtWutUpgrade> streamCodec()
    {
        return STREAM_CODEC;
    }
}
