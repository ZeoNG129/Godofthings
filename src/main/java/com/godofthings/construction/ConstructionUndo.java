package com.godofthings.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 神之建筑的撤销历史（只在服务端用）。
 *
 * <p>每执行完一次批量操作（建造 / 销毁 / 替换），把那一次动过的格子连同<b>改动前的方块状态</b>
 * 压成一条 {@link UndoEntry}；每个玩家最多保留 {@value #MAX_STEPS} 条，超了从最早的一条丢。</p>
 *
 * <p>{@link #undo(Player)} 弹出最近一条，<b>按逆序</b>把每一格复原（逆序是为了「先放的先拆」，
 * 对依赖支撑的方块更安全），返回实际复原的格子数；没有历史就返回 0。</p>
 *
 * <p>撤销本身<b>不</b>产生新的撤销记录（{@link #undo(Player)} 里不走 {@link #push}），
 * 也不会扣 / 退材料 —— 只是一次方块状态回滚。</p>
 */
public final class ConstructionUndo
{
    /** 每个玩家最多保留多少步（照上游 undo_history 的默认值） */
    public static final int MAX_STEPS = 10;

    /** 玩家 UUID → 该玩家的历史（队首最老，队尾最新） */
    private static final Map<UUID, Deque<UndoEntry>> HISTORY = new HashMap<>();

    private ConstructionUndo()
    {
    }

    /**
     * 一次改动：格子坐标（不可变）+ 改动前的方块状态。
     *
     * <p>坐标一律用 {@link BlockPos#immutable()}，否则 BFS 里复用可变 pos 会串格。</p>
     */
    public record BlockChange(BlockPos pos, BlockState oldState)
    {
    }

    /** 一次批量操作的全部改动（顺序 = 实际操作顺序） */
    public record UndoEntry(List<BlockChange> changes)
    {
    }

    /**
     * 压入一条撤销记录。没有改动任何格子的空记录直接忽略，不占历史名额。
     */
    public static void push(Player player, List<BlockChange> changes)
    {
        if (player == null || changes == null || changes.isEmpty())
        {
            return;
        }
        Deque<UndoEntry> entries = HISTORY.computeIfAbsent(player.getUUID(), key -> new ArrayDeque<>());
        // 上限 10 步：先丢最早的，再放新的
        while (entries.size() >= MAX_STEPS)
        {
            entries.removeFirst();
        }
        entries.addLast(new UndoEntry(List.copyOf(changes)));
    }

    /**
     * 撤回该玩家最近一次操作。
     *
     * @return 复原的格子数；0 = 没有历史
     */
    public static int undo(Player player)
    {
        if (player == null)
        {
            return 0;
        }
        Deque<UndoEntry> entries = HISTORY.get(player.getUUID());
        if (entries == null || entries.isEmpty())
        {
            return 0;
        }

        UndoEntry entry = entries.removeLast();
        if (entries.isEmpty())
        {
            HISTORY.remove(player.getUUID()); // 历史空了就别留空壳
        }

        Level level = player.level();
        List<BlockChange> changes = entry.changes();
        int restored = 0;
        // 逆序复原：最后放下的先回滚
        for (int i = changes.size() - 1; i >= 0; i--)
        {
            BlockChange change = changes.get(i);
            level.setBlock(change.pos(), change.oldState(), 2);
            restored++;
        }
        return restored;
    }

    /** 丢掉一个玩家的历史（退出登录 / 换维度时想清空就调它，现在没人调，留着备用） */
    public static void clear(Player player)
    {
        if (player != null)
        {
            HISTORY.remove(player.getUUID());
        }
    }
}
