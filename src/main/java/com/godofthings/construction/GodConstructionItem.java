package com.godofthings.construction;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 神之建筑：建筑棒模组的「下界合金棒」精简移植（上游 ConstructionSticks，作者 Mrbysco，MIT License；
 * AE 部分参照 Applied Construction Sticks，作者 benbenlaw）。
 *
 * <p>本物品<b>没有耐久</b>（不毁，永久生效），只靠一个数据组件记模式，三种模式循环：</p>
 * <ul>
 *   <li><b>潜行 + 右键方块</b>：切换模式 0 → 1 → 2 → 0，行动栏提示当前模式。</li>
 *   <li><b>建造（0）</b>：右键方块 = 贴着被点面「平面扩展」铺同种方块（最多
 *       {@value #MAX_BUILD_BLOCKS} 格）；右键空气 = 天使放置（副手的方块，
 *       在眼睛沿视线方向前进 {@value #ANGEL_DISTANCE} 格处放 1 格）。</li>
 *   <li><b>销毁（1）</b>：右键方块 = 拆掉被点方块的连通块（最多 {@value #MAX_DESTROY_BLOCKS} 格，<b>不掉落</b>）。</li>
 *   <li><b>替换（2）</b>：右键方块 = 把被点方块的连通块换成副手拿的方块（最多
 *       {@value #MAX_REPLACE_BLOCKS} 格），旧方块掉成物品。</li>
 * </ul>
 *
 * <p>材料（建造 / 替换 / 天使放置）先扣玩家自己的背包（主背包 36 格 + 快捷栏 + 副手），
 * 不够再去 AE 网络里抽（见 {@link ConstructionAeSource}，用物品绑定的无线访问点）。
 * 创造模式（{@code hasInfiniteMaterials}）不扣材料。</p>
 *
 * <p>所有动作只在服务端执行；客户端一律回报成功（返回 PASS 会让后面的方块 / 副手物品接着响应）。</p>
 */
public class GodConstructionItem extends Item
{
    /** 建造模式：平面扩展建造（右键方块） / 天使放置（右键空气） */
    public static final int MODE_BUILD = 0;
    /** 销毁模式：拆掉同种方块的连通块，不掉落 */
    public static final int MODE_DESTROY = 1;
    /** 替换模式：把同种方块的连通块换成副手方块 */
    public static final int MODE_REPLACE = 2;
    /** 模式总数（切换时 0 → 1 → 2 → 0） */
    public static final int MODE_COUNT = 3;

    /** 建造模式一次最多铺多少格（对齐下界合金棒的 limit） */
    public static final int MAX_BUILD_BLOCKS = 1024;
    /** 销毁模式一次最多拆多少格（对齐下界合金棒的 destruction） */
    public static final int MAX_DESTROY_BLOCKS = 81;
    /** 替换模式一次最多换多少格（对齐下界合金棒的 limit） */
    public static final int MAX_REPLACE_BLOCKS = 1024;

    /** 天使放置：从眼睛沿视线方向前进几格（取整后放 1 格；上游默认 angel = 16 是「隔着空气补一格」的距离，这里按移植要求固定 2） */
    public static final int ANGEL_DISTANCE = 2;

    public GodConstructionItem(Properties properties)
    {
        super(properties);
    }

    /** 当前模式；没写过组件就是建造模式 */
    public static int getMode(ItemStack stack)
    {
        return stack.getOrDefault(Godofthings.GOD_CONSTRUCTION_MODE.get(), MODE_BUILD);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        // 批量操作里一律用不可变坐标：后面 BFS 会到处 relative，可变 pos 被复用会串格
        BlockPos clickedPos = context.getClickedPos().immutable();

        // 客户端只回报「这次右键我用了」，真正的动作全在服务端（返回 PASS 会再触发方块 / 副手物品）
        if (level.isClientSide || player == null)
        {
            return InteractionResult.SUCCESS;
        }

        ItemStack wand = context.getItemInHand();
        if (player.isShiftKeyDown())
        {
            cycleMode(player, wand);
            return InteractionResult.SUCCESS;
        }

        // clickedFace = 玩家点中的那个面（也就是玩家面向的那一面），
        // 它对建造来说是「上方」，对被点方块来说是「外侧」；
        // face.getOpposite() 指回方块本体，是每一格的「背面」。
        Direction face = context.getClickedFace();
        switch (getMode(wand))
        {
            case MODE_DESTROY -> ConstructionActions.destroy(player, level, clickedPos, face);
            case MODE_REPLACE -> ConstructionActions.replace(player, wand, level, clickedPos);
            default -> ConstructionActions.build(player, wand, level, clickedPos, face);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack wand = player.getItemInHand(hand);
        if (level.isClientSide)
        {
            return InteractionResultHolder.success(wand);
        }

        // 右键空气只有建造模式有事可做：天使放置。
        // 潜行时空手右键不触发（潜行是「切模式」的手势，避免顺手在脚下放方块）。
        if (getMode(wand) == MODE_BUILD && !player.isShiftKeyDown())
        {
            ConstructionActions.angel(player, wand, level);
        }
        return InteractionResultHolder.success(wand);
    }

    /** 模式 0 → 1 → 2 → 0，并给一条行动栏提示 */
    private static void cycleMode(Player player, ItemStack wand)
    {
        int next = (getMode(wand) + 1) % MODE_COUNT;
        wand.set(Godofthings.GOD_CONSTRUCTION_MODE.get(), next);
        player.displayClientMessage(modeMessage(next), true);
    }

    /** 三个模式键分别写死字面量，方便 check-lang.ps1 静态扫描到 */
    private static Component modeMessage(int mode)
    {
        return switch (mode)
        {
            case MODE_DESTROY -> Component.translatable("message.godofthings.god_construction.mode_destroy");
            case MODE_REPLACE -> Component.translatable("message.godofthings.god_construction.mode_replace");
            default -> Component.translatable("message.godofthings.god_construction.mode_build");
        };
    }

    /**
     * 扣 1 个材料：先扣玩家自己的背包（主背包 36 格 + 快捷栏 + 副手），不够再走 AE 网络。
     *
     * @return true = 扣到了（或是创造模式）；false = 材料用完了，批量操作应当停下
     */
    public static boolean takeMaterial(Player player, ItemStack wand, ItemStack wanted)
    {
        if (wanted.isEmpty())
        {
            return false;
        }
        if (player.hasInfiniteMaterials())
        {
            return true; // 创造 / 旁观：不扣材料
        }

        Inventory inventory = player.getInventory();
        if (takeFromStacks(inventory.items, wanted) || takeFromStacks(inventory.offhand, wanted))
        {
            return true;
        }
        return player instanceof ServerPlayer serverPlayer
                && ConstructionAeSource.extract(serverPlayer, wand, wanted, 1) > 0;
    }

    /** 在这些槽位里找「同物品同组件」的栈，扣掉 1 个 */
    private static boolean takeFromStacks(List<ItemStack> stacks, ItemStack wanted)
    {
        for (ItemStack stack : stacks)
        {
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted))
            {
                continue;
            }
            stack.shrink(1);
            return true;
        }
        return false;
    }

    /** 一次批量操作只在第一格播放一次放置音效 */
    public static void playPlaceSound(Level level, BlockPos pos)
    {
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
