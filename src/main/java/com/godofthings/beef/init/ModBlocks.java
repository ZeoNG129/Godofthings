package com.godofthings.beef.init;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.content.blocks.TeleportPadBlock;
import com.godofthings.beef.world.teleport.UselessDimTeleporter;
import com.godofthings.beef.world.teleport.UselessDimTeleporter2;
import com.godofthings.beef.world.teleport.UselessDimTeleporter3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 无用维度（奇数 / 偶数 / 三维度）的三个传送方块。
 *
 * <p>逐字照抄上游 {@code com.sorrowmist.useless.init.ModBlocks} 里 {@code TELEPORT_BLOCK} 那三段
 * （注册名、构造参数、方块属性完全一致）。上游该类的其余方块属于机器子系统，未随本次照抄带入。</p>
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UselessMod.MODID);

    public static final DeferredBlock<Block> TELEPORT_BLOCK = BLOCKS.register(
            "teleport_block",
            () -> new TeleportPadBlock(
                    UselessDimTeleporter::new,
                    BlockBehaviour.Properties.of()
                            .strength(2.0f, 65536.0f)
                            .requiresCorrectToolForDrops()
            )
    );

    public static final DeferredBlock<Block> TELEPORT_BLOCK_2 = BLOCKS.register(
            "teleport_block_2",
            () -> new TeleportPadBlock(
                    UselessDimTeleporter2::new,
                    BlockBehaviour.Properties.of()
                            .strength(2.0f, 65536.0f)
                            .requiresCorrectToolForDrops()
            )
    );

    public static final DeferredBlock<Block> TELEPORT_BLOCK_3 = BLOCKS.register(
            "teleport_block_3",
            () -> new TeleportPadBlock(
                    UselessDimTeleporter3::new,
                    BlockBehaviour.Properties.of()
                            .strength(2.0f, 65536.0f)
                            .requiresCorrectToolForDrops()
            )
    );

    private ModBlocks() {
    }
}
