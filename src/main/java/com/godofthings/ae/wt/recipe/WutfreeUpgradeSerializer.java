package com.godofthings.ae.wt.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

import de.mari_023.ae2wtlib.api.registration.WTDefinition;

/**
 * {@link WutfreeUpgrade} 的序列化器（照抄 ae2wtlib 的 UpgradeSerializer，JSON 格式完全一致：
 * terminal / terminalName）。
 */
public class WutfreeUpgradeSerializer implements net.minecraft.world.item.crafting.RecipeSerializer<WutfreeUpgrade>
{
    public static final String NAME = "wutfree_upgrade";
    private static final MapCodec<WutfreeUpgrade> MAP_CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    Ingredient.CODEC.fieldOf("terminal").forGetter(WutfreeUpgrade::getTerminal),
                    WTDefinition.CODEC.fieldOf("terminalName")
                            .forGetter(WutfreeUpgrade::getTerminalDefinition))
                    .apply(builder, WutfreeUpgrade::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, WutfreeUpgrade> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WutfreeUpgrade::getTerminal,
            WTDefinition.STREAM_CODEC, WutfreeUpgrade::getTerminalDefinition,
            WutfreeUpgrade::new);

    @Override
    public MapCodec<WutfreeUpgrade> codec()
    {
        return MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, WutfreeUpgrade> streamCodec()
    {
        return STREAM_CODEC;
    }
}
