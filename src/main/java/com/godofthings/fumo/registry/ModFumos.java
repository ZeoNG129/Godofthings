package com.godofthings.fumo.registry;

import com.godofthings.Godofthings;
import com.godofthings.fumo.block.FumoBlock;
import com.godofthings.fumo.blockentity.FumoBlockEntity;
import com.godofthings.fumo.item.FumoBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 「HoYooG Fumo」皮肤玩偶的注册处。
 *
 * <p><b>照抄来源与许可（务必保留）</b>：结构照抄 <b>AE2 Lightning Tech Reborn</b>
 * （作者 MOAKIEE、CystrySU、gjmhmm8、_leng、TedXenon、MHanHanBing）的 fumo 玩偶系统。
 * 上游对该系统采用<b>双许可</b>：</p>
 * <ul>
 *   <li><b>源码 → GNU LGPL 3.0</b>（见仓库 {@code LICENSES/AE2LT-LGPL-3.0.txt}）——
 *       包内 {@code block/FumoBlock}、{@code blockentity/FumoBlockEntity}、{@code item/FumoBlockItem}、
 *       {@code client/FumoBlockRenderer}、{@code client/SpinningFumoBakedModel} 五个类保持 LGPL-3.0；</li>
 *   <li><b>素材 → CC BY-NC-SA 3.0</b>（见 {@code LICENSES/AE2LT-ASSETS-CC-BY-NC-SA-3.0.md}）——
 *       {@code assets/godofthings/models/block/hoyoog_fumo.json} 那份玩家模型保持该协议：
 *       <b>署名、禁止商用、相同方式共享</b>。</li>
 * </ul>
 * <p>例外：{@code assets/godofthings/textures/block/hoyoog_fumo.png} <b>不是</b>上游素材，
 * 它是本模组作者自己的 Minecraft 皮肤（64×64），随本模组按 ARR 分发。</p>
 *
 * <p>与上游的差异：上游用 {@code ModFumos} 一次性注册 5 个玩偶（Moakiee / Cystrysu / 猪咪 /
 * 创造猪咪 / 超维猪咪），本模组只做 1 个，因此把上游的 {@code ModBlocks}/{@code ModItems}/
 * {@code ModBlockEntities} 三个注册器合并到本类，注册名与方块实体 ID 与上游同名（{@code fumo}）。</p>
 */
public final class ModFumos {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Godofthings.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Godofthings.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Godofthings.MODID);

    public static final DeferredBlock<FumoBlock> HOYOOG_FUMO =
            BLOCKS.register("hoyoog_fumo", FumoBlock::new);

    public static final DeferredItem<FumoBlockItem> HOYOOG_FUMO_ITEM =
            ITEMS.register("hoyoog_fumo",
                    () -> new FumoBlockItem(HOYOOG_FUMO.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FumoBlockEntity>> FUMO =
            BLOCK_ENTITY_TYPES.register(
                    "fumo",
                    () -> BlockEntityType.Builder.of(
                            FumoBlockEntity::new,
                            HOYOOG_FUMO.get())
                            .build(null));

    private ModFumos() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
