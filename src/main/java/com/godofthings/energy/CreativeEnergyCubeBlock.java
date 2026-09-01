package com.godofthings.energy;

import com.godofthings.Godofthings;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 创造能量立方方块：右键打开充能 GUI，相邻 FE 机器自动以最大速率取电。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CreativeEnergyCubeBlock extends BaseEntityBlock
{
    public CreativeEnergyCubeBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return simpleCodec(CreativeEnergyCubeBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new CreativeEnergyCubeEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide)
        {
            return null;
        }
        return createTickerHelper(type, Godofthings.CREATIVE_ENERGY_CUBE_BE.get(), CreativeEnergyCubeEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
        {
            // NeoForge 1.21.1：NetworkHooks.openScreen → 玩家扩展 openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)
            serverPlayer.openMenu(state.getMenuProvider(level, pos), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
    }

    // ------------------------------------------------------------------ 能力注册

    // NeoForge 1.21.1：BlockEntity 不再支持 getCapability 覆写，能力统一在
    // RegisterCapabilitiesEvent（MOD 总线）注册：六面 FE 能量源 + 充能物品格。
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Godofthings.CREATIVE_ENERGY_CUBE_BE.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.CREATIVE_ENERGY_CUBE_BE.get(),
                (be, side) -> be.getItems());
    }
}
