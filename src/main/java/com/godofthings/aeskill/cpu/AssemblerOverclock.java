package com.godofthings.aeskill.cpu;

import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import com.godofthings.aeskill.AeSkillEffects;
import com.godofthings.aeskill.TickBudget;
import com.godofthings.aeskill.divine.DivineGrace;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 装配机的「额外原生工作循环」：每多跑一轮就沿用该机器自己的电力/原料/产出规则。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.AssemblerOverclock}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>。</p>
 *
 * <h2>通用性（不依赖模组类名）</h2>
 * 挂钩点是 {@code TickManagerService.unsafeTickingRequest}，对任意 {@link IGridTickable} 生效；
 * 判定走本类的 {@link #supports(IGridTickable, IGridNode)}，主路径是 AE2 官方接口
 * {@link ICraftingMachine#acceptsPlans()} —— <b>任何</b>第三方「能被供应模板的装配机」都满足它。
 *
 * <p><b>v7.16.3 补移植说明</b>：本类在 7.16.0 移植时被漏掉了，直接后果是
 * 「装配超频」只作用于 AE2 原生分子装配室（走进度倍率），对第三方装配机完全没效果，
 * 整体体感比原版慢。</p>
 */
public final class AssemblerOverclock
{
    private AssemblerOverclock() {}

    /**
     * 判定顺序有语义，勿随意调整：
     * <ol>
     *   <li>{@link MolecularAssemblerBlockEntity} —— AE2 原生分子装配室的进度钩子<b>已经</b>加过乘数，
     *       这里必须返回 false，否则会双倍加速。</li>
     *   <li>{@link ICraftingMachine#acceptsPlans()} —— <b>通用主路径</b>：AE2 官方接口，模组无关。</li>
     * </ol>
     */
    public static boolean supports(IGridTickable tickable, IGridNode node)
    {
        // AE2 原生进度钩子已应用倍率，绝不重复叠加。
        if (tickable instanceof MolecularAssemblerBlockEntity)
        {
            return false;
        }
        // 通用主路径：AE2 官方接口判定，任意第三方装配机在此生效。
        return tickable instanceof ICraftingMachine machine && machine.acceptsPlans();
    }

    public static TickRateModulation tick(IGridTickable tickable, IGridNode node, int elapsed)
    {
        TickRateModulation result = tickable.tickingRequest(node, elapsed);
        int bonus = AeSkillEffects.getAssemblerSpeedLevel();
        if (bonus == 0 || result == TickRateModulation.SLEEP || result == TickRateModulation.IDLE
                || !supports(tickable, node))
        {
            return result;
        }
        // 「神明之力」本 tick 已经在驱动这台机器时让路：两个技能不叠加同一份额外 tick。
        if (tickable instanceof BlockEntity blockEntity && DivineGrace.isAccelerated(blockEntity))
        {
            return result;
        }
        long began = System.nanoTime();
        long deadline = TickBudget.deadline(began);
        try
        {
            for (int i = 0; i < bonus && System.nanoTime() < deadline; i++)
            {
                if (!node.isActive())
                {
                    break;
                }
                result = tickable.tickingRequest(node, elapsed);
                if (result == TickRateModulation.SLEEP || result == TickRateModulation.IDLE)
                {
                    break;
                }
            }
            return result;
        }
        finally
        {
            TickBudget.charge(began);
        }
    }
}
