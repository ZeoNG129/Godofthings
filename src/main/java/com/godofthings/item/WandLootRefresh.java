package com.godofthings.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 神之工具的「战利品箱刷新」（移植自万象担架 {@code WondrousStaffLootRefresh}，190 行）。
 *
 * <h3>上游做法与我们的简化</h3>
 * 上游用一个 Mixin 在<b>容器首次被打开、原版即将丢弃战利品表之前</b>把表记到持久数据里
 * （键 {@code useless_stretcher:source_loot_table}），之后才刷新得动 —— 因为原版
 * {@code unpackLootTable} 之后 {@code getLootTable()} 就变成 null 了。
 * <p>我们没有加那个 Mixin，改为<b>在玩家手拿神之工具右键容器的瞬间记账</b>（那时表还在），
 * 所以：<b>第一次右键 = 记住并刷新；之后每次右键 = 用记住的表重刷</b>。
 * 对"从未被任何玩家打开过"的箱子同样有效。
 *
 * <h3>刷新三件套（上游同款）</h3>
 * {@code clearContent()} → {@code setLootTable(源表, 随机种子)} → {@code unpackLootTable(player)}
 */
public final class WandLootRefresh
{
    /** 原始战利品表存在方块实体的持久数据里（按方块实体保存，随区块存档） */
    private static final String SOURCE_TABLE = "godofthings:wand_source_loot_table";

    private WandLootRefresh() {}

    /** 判断能否刷新：原版的可随机化战利品容器 */
    public static boolean isSupported(BlockEntity be)
    {
        return be instanceof RandomizableContainerBlockEntity;
    }

    /** 从持久数据取回（或记录）源战利品表 */
    private static ResourceKey<LootTable> sourceTable(RandomizableContainerBlockEntity container)
    {
        ResourceKey<LootTable> current = container.getLootTable();
        CompoundTag data = container.getPersistentData();
        if (current != null)
        {
            data.putString(SOURCE_TABLE, current.location().toString());
            return current;
        }
        String saved = data.getString(SOURCE_TABLE);
        if (saved.isEmpty())
        {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(saved);
        return rl == null ? null : ResourceKey.create(Registries.LOOT_TABLE, rl);
    }

    /**
     * 刷新该位置的战利品箱。
     *
     * @return true = 刷新成功；false = 不支持或没记录到源表
     */
    public static boolean tryRefresh(Player player, BlockPos pos)
    {
        if (!(player.level() instanceof ServerLevel level))
        {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RandomizableContainerBlockEntity container))
        {
            return false;
        }
        ResourceKey<LootTable> source = sourceTable(container);
        if (source == null)
        {
            return false;
        }
        try
        {
            container.clearContent();
            container.setLootTable(source, player.getRandom().nextLong());
            container.unpackLootTable(player);
            container.setChanged();
        }
        catch (RuntimeException ignored)
        {
            return false; // 某些容器拒绝重刷：静默失败，不影响游戏
        }
        // 双箱的另一半一起刷，否则大箱子只刷新一半
        BlockPos other = connectedChest(level, pos);
        if (other != null && level.getBlockEntity(other) instanceof RandomizableContainerBlockEntity sibling
                && isSupported(sibling))
        {
            ResourceKey<LootTable> siblingSource = sourceTable(sibling);
            if (siblingSource != null)
            {
                try
                {
                    sibling.clearContent();
                    sibling.setLootTable(siblingSource, player.getRandom().nextLong());
                    sibling.unpackLootTable(player);
                    sibling.setChanged();
                }
                catch (RuntimeException ignored)
                {
                    // 忽略
                }
            }
        }
        return true;
    }

    /** 双联箱的另一半（大箱子） */
    private static BlockPos connectedChest(ServerLevel level, BlockPos pos)
    {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock))
        {
            return null;
        }
        if (!(state.hasProperty(net.minecraft.world.level.block.ChestBlock.TYPE))
                || state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)
                == net.minecraft.world.level.block.state.properties.ChestType.SINGLE)
        {
            return null;
        }
        return pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
    }
}