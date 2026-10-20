package com.godofthings.item;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 神之配置：一台机器的「配置模板」。
 *
 * <ul>
 *   <li><b>潜行 + 右键</b>：把目标机器的六面 I/O 模式、AE 并网开关、加速槽里的神之加速
 *       <b>数量</b>存进物品数据组件（只记数量，不记物品），提示「已保存：&lt;方块名&gt;」。</li>
 *   <li><b>普通右键</b>：把存下的六面模式与 AE 开关写回同类机器，并按存下的数量从
 *       <b>玩家物品栏</b>扣神之加速补进机器（创造模式不扣）。加速不够时其余照贴并提示
 *       「加速不足（需要 N，实际 M）」；机器类型不符 / 卡里没数据也会分别提示。</li>
 * </ul>
 *
 * <p>能被读写的机器由 {@link ConfigurableMachine} 约定（熔炉 / 矿机 / 矿物 / 作物 /
 * 复制 / 去皮 / 掉落 / 怪蛋 / 砍杀 / 吸收 / 合成台；后三台没有加速槽）。</p>
 */
public class GodConfigItem extends Item
{
    public GodConfigItem(Properties properties)
    {
        super(properties);
    }

    /**
     * 潜行保存走这里：潜行时原版本来就会跳过方块交互（{@code flag1}），
     * 所以 {@code useOn} 一定会被调用，不需要在 {@code onItemUseFirst} 里抢先。
     */
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context)
    {
        return handleUse(context);
    }

    /**
     * 普通右键粘贴必须先在这里拦：机器方块的右键是 {@code useWithoutItem}（打开界面），
     * 它 {@code consumesAction} 之后原版就不会再调用 {@code Item#useOn} 了。
     * 潜行时放行给 {@link #useOn}（保持「潜行 = 保存」的语义只在一处实现）。
     */
    @Override
    public InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown())
        {
            return InteractionResult.PASS;
        }
        return handleUse(context);
    }

    private static InteractionResult handleUse(UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        boolean machine = blockEntity instanceof ConfigurableMachine;
        if (level.isClientSide || player == null)
        {
            // 客户端只负责「是机器就别把机器界面开起来」（服务端才是权威）；
            // 不是机器就 PASS，右键照常走原版流程
            return machine ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(blockEntity instanceof ConfigurableMachine target))
        {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown())
        {
            saveConfig(level, pos, target, player, stack);
        }
        else
        {
            pasteConfig(level, pos, target, player, stack);
        }
        return InteractionResult.SUCCESS;
    }

    /** 保存：读机器当前状态写进卡里（加速只记数量，不搬物品） */
    private static void saveConfig(Level level, BlockPos pos, ConfigurableMachine machine,
                                   Player player, ItemStack stack)
    {
        BlockState state = level.getBlockState(pos);
        String blockId = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        GodConfigData data = new GodConfigData(blockId,
                machine.getFaceModesSnapshot(), machine.isAeEnabled(), machine.acceleratorCount());
        stack.set(Godofthings.GOD_CONFIG_DATA.get(), data);
        player.displayClientMessage(
                Component.translatable("message.godofthings.god_config.saved", state.getBlock().getName()), true);
    }

    /** 粘贴：先校验机器类型，再写回六面模式 / AE 开关，最后补神之加速 */
    private static void pasteConfig(Level level, BlockPos pos, ConfigurableMachine machine,
                                    Player player, ItemStack stack)
    {
        GodConfigData data = stack.get(Godofthings.GOD_CONFIG_DATA.get());
        if (data == null || !data.hasData())
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.god_config.no_data"), true);
            return;
        }
        BlockState state = level.getBlockState(pos);
        String currentId = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        if (!currentId.equals(data.blockId()))
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.god_config.wrong_machine"), true);
            return;
        }

        machine.applyFaceModes(data.copyFaceModes());
        machine.setAeEnabled(data.aeEnabled());

        // 目标数量 - 机器现有数量 = 还差多少个（机器塞不下的部分不算）
        int need = Math.min(data.accelCount() - machine.acceleratorCount(),
                ConfigurableMachine.MAX_ACCELERATORS - machine.acceleratorCount());
        int added = 0;
        if (need > 0)
        {
            Inventory inventory = player.getInventory();
            boolean creative = player.getAbilities().instabuild;
            int available = creative ? need : countAccelerators(inventory);
            int take = Math.min(need, available);
            if (take > 0)
            {
                if (!creative)
                {
                    // 真实扣除；万一扣到的比预想的少，就以真正扣到的数量为准
                    take = takeAccelerators(inventory, take);
                }
                added = machine.addAccelerators(take);
                if (added < take)
                {
                    // 机器没塞下（理论上不会发生，兜底把多扣的还给玩家）
                    ItemStack refund = new ItemStack(Godofthings.GOD_ACCELERATOR.get(), take - added);
                    if (!inventory.add(refund))
                    {
                        player.drop(refund, false);
                    }
                }
            }
        }

        if (need > 0 && added < need)
        {
            player.displayClientMessage(Component.translatable(
                    "message.godofthings.god_config.accel_short", need, added), true);
        }
        else
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.god_config.applied", state.getBlock().getName()), true);
        }
    }

    /** 物品栏里现有的神之加速总数 */
    private static int countAccelerators(Inventory inventory)
    {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof GodAcceleratorItem)
            {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** 从物品栏扣掉最多 {@code want} 个神之加速，返回真正扣到的数量 */
    private static int takeAccelerators(Inventory inventory, int want)
    {
        int left = want;
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof GodAcceleratorItem))
            {
                continue;
            }
            int taken = Math.min(left, stack.getCount());
            stack.shrink(taken);
            left -= taken;
        }
        return want - left;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag)
    {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.god_config"));
    }
}
