package com.godofthings.aalink;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.godofthings.Godofthings;

/**
 * AALink 移植的注册（方块 / 物品 / 方块实体）。
 *
 * <p>按项目约定，所有 {@code DeferredRegister.register(...)} 调用都集中在
 * {@link Godofthings} 的静态初始化里完成，本类只持有注册器与条目引用，
 * 由 {@code Godofthings} 调用 {@link #register(IEventBus)} 完成接线。</p>
 */
public final class AaLinkRegistry
{
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Godofthings.MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Godofthings.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    Godofthings.MODID);

    public static final DeferredBlock<AaLinkConnectorBlock> WIRELESS_CONNECTOR =
            BLOCKS.register("wireless", () -> new AaLinkConnectorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 7)
                            .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> WIRELESS_CONNECTOR_ITEM =
            ITEMS.registerSimpleBlockItem("wireless", WIRELESS_CONNECTOR);

    /**
     * 无线连接器（卡）：v7.11.0 起替换原「无线连接工具」（AALink 版），
     * 照抄 AE2LT 过载频率卡的功能与贴图，绑定对象改为无线连接器。
     * 注册名保留 wireless_link_tool（旧存档物品不丢）。
     */
    public static final DeferredItem<ConnectorCardItem> WIRELESS_LINK_TOOL =
            ITEMS.registerItem("wireless_link_tool",
                    props -> new ConnectorCardItem(props.stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AaLinkConnectorBlockEntity>>
            WIRELESS_CONNECTOR_BE = BLOCK_ENTITIES.register("wireless",
                    () -> BlockEntityType.Builder.of(AaLinkConnectorBlockEntity::new,
                            WIRELESS_CONNECTOR.get()).build(null));

    private AaLinkRegistry()
    {
    }

    /** 由 Godofthings 构造器调用，把三个注册器挂到 mod 事件总线。 */
    public static void register(IEventBus modEventBus)
    {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }

    /** 物品是否属于 AALink（供创造栏与手册分类使用）。 */
    public static boolean isAaLinkItem(Item item)
    {
        return item == WIRELESS_CONNECTOR_ITEM.get() || item == WIRELESS_LINK_TOOL.get();
    }
}
