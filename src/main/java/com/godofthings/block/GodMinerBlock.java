package com.godofthings.block;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodMinerBlockEntity;
import com.godofthings.item.WandItemUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class GodMinerBlock extends BaseEntityBlock
{
    public static final MapCodec<GodMinerBlock> CODEC = simpleCodec(GodMinerBlock::new);

    public GodMinerBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new GodMinerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return level.isClientSide ? null
                : createTickerHelper(type, Godofthings.GOD_MINER_BE.get(), GodMinerBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    // 中键拾取/掉落物需要返回矿机物品（GodMinerItem 不是 BlockItem，需手动关联）
    @Override
    public net.minecraft.world.item.Item asItem()
    {
        return Godofthings.GOD_MINER_ITEM.get();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.openMenu(state.getMenuProvider(level, pos), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof MenuProvider mp ? mp : null;
    }

    // 放置时读取物品上的附魔（效率/时运/精准采集），并初始化起始挖掘深度
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof GodMinerBlockEntity be)
        {
            // 1.21.1：附魔存于 ENCHANTMENTS 数据组件（键为 Holder<Enchantment>），替换旧 getItemEnchantmentLevel
            ItemEnchantments stackEnchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            int efficiency = stackEnchants.getLevel(WandItemUtils.enchantHolder(Enchantments.EFFICIENCY, level.registryAccess()));
            int fortune = stackEnchants.getLevel(WandItemUtils.enchantHolder(Enchantments.FORTUNE, level.registryAccess()));
            boolean silkTouch = stackEnchants.getLevel(WandItemUtils.enchantHolder(Enchantments.SILK_TOUCH, level.registryAccess())) > 0;
            be.setEnchants(efficiency, fortune, silkTouch);
            be.resetDigging();
        }
    }

    // 挖掉矿机时物品一并消失，不掉落（避免大量掉落物造成卡顿）
}
