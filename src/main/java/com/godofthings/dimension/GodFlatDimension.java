package com.godofthings.dimension;

import com.godofthings.Godofthings;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 注册神之平坦维度的区块生成器 Codec。
 * 移植自 useless_mod 的 UselessDimension2（1.20.1）。
 */
public class GodFlatDimension
{
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Godofthings.MODID);

    public static final RegistryObject<Codec<GodFlatDimGen>> SUPERFLAT_GEN_CODEC =
            CHUNK_GENERATORS.register("superflat_gen", () -> GodFlatDimGen.CODEC);
}
