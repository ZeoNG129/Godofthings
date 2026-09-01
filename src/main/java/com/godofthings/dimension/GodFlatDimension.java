package com.godofthings.dimension;

import com.godofthings.Godofthings;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 注册神之平坦维度的区块生成器 Codec。
 * 移植自 useless_mod 的 UselessDimension2（1.20.1）。
 * 1.21.1：CHUNK_GENERATOR 注册表的值类型由 Codec&lt;? extends ChunkGenerator&gt; 改为
 * MapCodec&lt;? extends ChunkGenerator&gt;（主类 Godofthings 构造器将本 DeferredRegister 注册到 mod 总线）。
 */
public class GodFlatDimension
{
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Godofthings.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<GodFlatDimGen>> SUPERFLAT_GEN_CODEC =
            CHUNK_GENERATORS.register("superflat_gen", () -> GodFlatDimGen.CODEC);
}
