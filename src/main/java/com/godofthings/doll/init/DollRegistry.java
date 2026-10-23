package com.godofthings.doll.init;

import com.godofthings.Godofthings;
import com.godofthings.doll.block.TwoIDollDisplayBlock;
import com.godofthings.doll.blockentity.TwoIDollDisplayBlockEntity;
import com.godofthings.doll.entity.TwoIDollEntity;
import com.godofthings.doll.item.TwoIDollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 玩偶系统自己的注册表（实体 / 物品 / 方块 / 方块实体 / 音效）。
 *
 * <p>原 mod 把这些散在 {@code ModEntities} / {@code ModItems} / {@code ModBlocks} /
 * {@code ModBlockEntities} / {@code ModSounds} 五个类里，跟手杖、胶囊、范围加速混在一起。
 * 移植时拆出来独立成表，godofthings 主类只在构造器里挂一次。</p>
 *
 * <p><b>为什么不复用 Godofthings.ITEMS / BLOCKS</b>：玩偶是可选功能（要 Curios 才是完整形态），
 * 独立注册表便于以后整体摘除，也不会让主模组的物品表多出 2 个 id 影响 check-lang 的计数口径。</p>
 */
public final class DollRegistry
{
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Godofthings.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Godofthings.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Godofthings.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Godofthings.MODID);

    /** 玩偶实体：0.44 × 0.96，追踪 10 格，2 tick 同步一次（走路动画靠它） */
    public static final DeferredHolder<EntityType<?>, EntityType<TwoIDollEntity>> TWO_I_DOLL_ENTITY =
            ENTITIES.register("two_i_doll", () -> EntityType.Builder
                    .of(TwoIDollEntity::new, MobCategory.MISC)
                    .sized(0.44F, 0.96F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .noSummon()
                    .build("two_i_doll"));

    /** 玩偶物品（可放出 / 可收回 / 可放饰品栏） */
    public static final DeferredHolder<Item, TwoIDollItem> TWO_I_DOLL_ITEM =
            ITEMS.register("two_i_doll", TwoIDollItem::new);

    /** 玩偶摆件方块 */
    public static final DeferredHolder<Block, TwoIDollDisplayBlock> TWO_I_DOLL_DISPLAY =
            BLOCKS.register("two_i_doll_display", () -> new TwoIDollDisplayBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOL)
                            .strength(0.4F)
                            .sound(SoundType.WOOL)
                            .noOcclusion()));

    /** 摆件方块的物品形态 */
    public static final DeferredHolder<Item, BlockItem> TWO_I_DOLL_DISPLAY_ITEM =
            ITEMS.register("two_i_doll_display", () -> new BlockItem(TWO_I_DOLL_DISPLAY.get(),
                    new Item.Properties()));

    /** 摆件方块实体 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TwoIDollDisplayBlockEntity>> TWO_I_DOLL_DISPLAY_BE =
            BLOCK_ENTITIES.register("two_i_doll_display", () -> BlockEntityType.Builder
                    .of(TwoIDollDisplayBlockEntity::new, TWO_I_DOLL_DISPLAY.get())
                    .build(null));

    private DollRegistry()
    {
    }

    /** 挂到 mod 事件总线（由 {@code Godofthings} 构造器调用一次） */
    public static void register(IEventBus modEventBus)
    {
        ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }

    /** 注册实体属性（独立方法，供 {@code EntityAttributeCreationEvent} 调用） */
    public static void registerAttributes(EntityAttributeCreationEvent event)
    {
        event.put(TWO_I_DOLL_ENTITY.get(), TwoIDollEntity.createAttributes().build());
    }
}
