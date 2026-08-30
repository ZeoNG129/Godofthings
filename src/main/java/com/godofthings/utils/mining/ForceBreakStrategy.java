package com.godofthings.utils.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * R键强制单方块破坏策略。
 */
public class ForceBreakStrategy implements MiningStrategy
{
    @Override
    public void handleBreak(BlockEvent.BreakEvent event, ItemStack hand, Player player)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        if (handleChaosCrystal(level, pos, state, player))
        {
            event.setCanceled(true);
            return;
        }

        boolean isSilkTouch = MiningUtils.isSilkTouchMode(hand);

        if (isSilkTouch)
        {
            clearContainerContents(level, pos);
        }

        List<ItemStack> fallbackDrops = MiningUtils.getForcedFallbackDrops(state, level, pos, hand);
        List<ItemStack> drops = MiningUtils.destroyBlockAndCollectDrops(level, pos, state, player, hand);

        if (MiningUtils.hasNoValidDrops(drops) && !MiningUtils.hasNoValidDrops(fallbackDrops))
        {
            drops = fallbackDrops;
        }
        MiningUtils.handleDrops(player, drops, hand);

        if (!isSilkTouch)
        {
            block.popExperience(level, pos, block.getExpDrop(state, level, level.random, pos, 0, 0));
        }

        event.setCanceled(true);
    }

    /** 混沌水晶特殊处理（Draconic Evolution，纯反射，未安装则跳过）。 */
    private boolean handleChaosCrystal(ServerLevel level, BlockPos pos, BlockState state, Player player)
    {
        try
        {
            Class<?> chaosCrystalClass = Class.forName("com.brandon3055.draconicevolution.blocks.ChaosCrystal");
            Class<?> tileChaosCrystalClass = Class.forName("com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal");

            if (chaosCrystalClass.isInstance(state.getBlock()))
            {
                BlockEntity tileEntity = level.getBlockEntity(pos);
                if (tileChaosCrystalClass.isInstance(tileEntity))
                {
                    try
                    {
                        Method setDefeatedMethod = tileChaosCrystalClass.getMethod("setDefeated");
                        setDefeatedMethod.invoke(tileEntity);
                    }
                    catch (Exception e)
                    {
                        Field guardianDefeatedField = tileChaosCrystalClass.getDeclaredField("guardianDefeated");
                        guardianDefeatedField.setAccessible(true);
                        Object managedBool = guardianDefeatedField.get(tileEntity);
                        Method setMethod = managedBool.getClass().getMethod("set", boolean.class);
                        setMethod.invoke(managedBool, true);
                    }

                    Method tickMethod = tileChaosCrystalClass.getMethod("tick");
                    tickMethod.invoke(tileEntity);
                }

                try
                {
                    Class<?> deConfigClass = Class.forName("com.brandon3055.draconicevolution.DEConfig");
                    Field chaosDropCountField = deConfigClass.getDeclaredField("chaosDropCount");
                    chaosDropCountField.setAccessible(true);
                    int chaosDropCount = chaosDropCountField.getInt(null);

                    Class<?> deContentClass = Class.forName("com.brandon3055.draconicevolution.init.DEContent");
                    Field chaosShardField = deContentClass.getDeclaredField("CHAOS_SHARD");
                    chaosShardField.setAccessible(true);
                    Object chaosShardObject = chaosShardField.get(null);

                    if (chaosShardObject instanceof net.minecraftforge.registries.RegistryObject<?> ro)
                    {
                        Object chaosShardItem = ro.get();
                        ItemStack chaosShardStack = new ItemStack((net.minecraft.world.item.Item) chaosShardItem, chaosDropCount);
                        Block.popResource(level, pos, chaosShardStack);
                    }

                    level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(pos.below(2), Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    return true;
                }
                catch (Exception ignored)
                {
                }
            }
        }
        catch (ClassNotFoundException ignored)
        {
        }
        catch (Exception ignored)
        {
        }
        return false;
    }

    private void clearContainerContents(ServerLevel level, BlockPos pos)
    {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        try
        {
            if (be instanceof Container container)
            {
                for (int i = 0; i < container.getContainerSize(); i++)
                {
                    container.setItem(i, ItemStack.EMPTY);
                }
                container.setChanged();
            }
            else
            {
                IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
                if (handler instanceof IItemHandlerModifiable modifiable)
                {
                    for (int i = 0; i < modifiable.getSlots(); i++)
                    {
                        modifiable.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }
}
