package com.godofthings.infinityprovider;

import appeng.api.AECapabilities;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.definitions.AEBlocks;

import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * 无限样板供应器的 AE2 能力注册（移植自上游 {@code AddBlockEntityToAE2Network} +
 * {@code CustomPatternProviderRegisterGenericHandler}）：方块实体并网 +
 * 缓存返回物品栏（GENERIC_INTERNAL_INV，part 形态挂在 AE2 线缆方块上）。
 */
@EventBusSubscriber(modid = GodofthingsModId.MODID)
public final class InfProviderCapabilities {

    private InfProviderCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 方块实体并网（IN_WORLD_GRID_NODE_HOST）
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                InfProviderRegistry.P1_BE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                InfProviderRegistry.P32_BE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                InfProviderRegistry.P1024_BE.get(), (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                InfProviderRegistry.PINF_BE.get(), (blockEntity, side) -> blockEntity);

        // 方块形态：缓存返回物品栏
        event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV,
                InfProviderRegistry.P1_BE.get(), (blockEntity, side) -> blockEntity.getLogic().getReturnInv());
        event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV,
                InfProviderRegistry.P32_BE.get(), (blockEntity, side) -> blockEntity.getLogic().getReturnInv());
        event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV,
                InfProviderRegistry.P1024_BE.get(), (blockEntity, side) -> blockEntity.getLogic().getReturnInv());
        event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV,
                InfProviderRegistry.PINF_BE.get(), (blockEntity, side) -> blockEntity.getLogic().getReturnInv());

        // part 形态：把能力挂到 AE2 的线缆方块上，按 side 取出对应 part 的返回物品栏
        registerPartReturnInv(event, InfProviderPart.Provider1Part.class);
        registerPartReturnInv(event, InfProviderPart.Provider32Part.class);
        registerPartReturnInv(event, InfProviderPart.Provider1024Part.class);
        registerPartReturnInv(event, InfProviderPart.ProviderInfPart.class);
    }

    private static void registerPartReturnInv(RegisterCapabilitiesEvent event,
            Class<? extends InfProviderPart> partClass) {
        event.registerBlock(AECapabilities.GENERIC_INTERNAL_INV,
                (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof CableBusBlockEntity cableBus
                            && cableBus.getCableBus().getPart(side) != null
                            && partClass.isInstance(cableBus.getCableBus().getPart(side))) {
                        return ((InfProviderPart) cableBus.getCableBus().getPart(side))
                                .getLogic().getReturnInv();
                    }
                    return null;
                },
                new Block[] { AEBlocks.CABLE_BUS.block() });
    }

    /** commonSetup 里调用：绑定四对方块与方块实体 + 注册 BE 物品形态。 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void bindAll() {
        bind((appeng.block.AEBaseEntityBlock) InfProviderRegistry.P1_BLOCK.get(),
                InfProviderRegistry.P1_BE.get(), InfProviderBlockEntity.Provider1.class,
                InfProviderRegistry.P1_ITEM.get());
        bind((appeng.block.AEBaseEntityBlock) InfProviderRegistry.P32_BLOCK.get(),
                InfProviderRegistry.P32_BE.get(), InfProviderBlockEntity.Provider32.class,
                InfProviderRegistry.P32_ITEM.get());
        bind((appeng.block.AEBaseEntityBlock) InfProviderRegistry.P1024_BLOCK.get(),
                InfProviderRegistry.P1024_BE.get(), InfProviderBlockEntity.Provider1024.class,
                InfProviderRegistry.P1024_ITEM.get());
        bind((appeng.block.AEBaseEntityBlock) InfProviderRegistry.PINF_BLOCK.get(),
                InfProviderRegistry.PINF_BE.get(), InfProviderBlockEntity.ProviderInf.class,
                InfProviderRegistry.PINF_ITEM.get());
    }

    private static void bind(appeng.block.AEBaseEntityBlock block,
            net.minecraft.world.level.block.entity.BlockEntityType type,
            Class<? extends AEBaseBlockEntity> entityClass,
            net.minecraft.world.item.Item item) {
        InfProviderBlock.bindBlockAndEntity(block, type, entityClass);
        AEBaseBlockEntity.registerBlockEntityItem(type, item);
    }
}
