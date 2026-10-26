package com.godofthings.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.godofthings.aeskill.AeSkillEffects;
import com.godofthings.aeskill.cpu.InstantCrafting;
import com.godofthings.aeskill.cpu.VirtualCpus;
import com.google.common.collect.ImmutableSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/**
 * 「算力之神 / 合成之神」在合成服务层的接线：虚拟 CPU 池刷新、公平轮转、最终产物归属。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.AeCraftingServiceMixin}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>。上游那套「第三方 CPU 装饰器适配」
 * 与兼容插件本模组用不到（上游自己在 1.1.5 已改成保守退出），这里直接不移植，
 * 因此也不引入 {@code CpuAdapterRegistry}。</p>
 */
@Mixin(value = CraftingService.class)
public abstract class AeCraftingServiceMixin
{
    @Shadow
    @Final
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Unique
    private boolean god$ticking;

    @Unique
    private boolean god$ticked;

    @Unique
    private int god$cursor;

    @Unique
    private boolean god$hasVirtual;

    @Unique
    private final ArrayList<CraftingCPUCluster> god$order = new ArrayList<>();

    @Unique
    private void god$refresh()
    {
        if (AeSkillEffects.getCpuInfiniteLevel() == 0 && !god$hasVirtual)
        {
            return;
        }
        VirtualCpus.refresh(craftingCPUClusters, (CraftingService) (Object) this);
        god$hasVirtual = false;
        for (var cpu : craftingCPUClusters)
        {
            if (VirtualCpus.state(cpu).parent != null)
            {
                god$hasVirtual = true;
                break;
            }
        }
    }

    @Inject(method = "updateCPUClusters", at = @At("TAIL"), require = 1)
    private void god$restoreSlots(CallbackInfo ci)
    {
        for (var cpu : craftingCPUClusters)
        {
            if (!VirtualCpus.state(cpu).children.isEmpty())
            {
                god$hasVirtual = true;
                break;
            }
        }
        god$refresh();
    }

    @Inject(method = { "getCpus", "submitJob" }, at = @At("HEAD"), require = 1)
    private void god$allocateSlots(CallbackInfoReturnable<?> ci)
    {
        if (!god$ticking)
        {
            god$refresh();
        }
    }

    @Inject(method = "onServerEndTick", at = @At("HEAD"), require = 1)
    private void god$beginTick(CallbackInfo ci)
    {
        god$refresh();
        god$ticking = true;
        god$ticked = false;
    }

    @Inject(method = "onServerEndTick", at = @At("RETURN"), require = 1)
    private void god$endTick(CallbackInfo ci)
    {
        god$ticking = false;
    }

    /**
     * 每个 CPU 的原生逻辑按轮转顺序各跑一次，且都在原生变更聚合之前。
     * 这样 HashSet 里排第一的那台 CPU 不会独吞瞬时合成的时间预算。
     *
     * <p><b>必须 require = 0</b>：若实例里同时装着原版 {@code zifeng_ae2_skills}，
     * 它在同一调用点也做了 @Redirect 且优先级相同，Mixin 会判定冲突并跳过本注入
     * （日志：{@code @Redirect conflict. Skipping ...}）。写成 require = 1 时这一步会直接
     * 抛 InjectionError 让整个游戏崩在加载阶段——冲突只是「借用别人已装的功能」，
     * 不该是崩溃理由。跳过时代价仅是失去公平轮转，功能本身仍由对方的注入提供。</p>
     */
    @Redirect(method = "onServerEndTick", at = @At(value = "INVOKE",
            target = "Lappeng/crafting/execution/CraftingCpuLogic;tickCraftingLogic(Lappeng/api/networking/energy/IEnergyService;Lappeng/me/service/CraftingService;)V"),
            require = 0)
    private void god$fairTick(CraftingCpuLogic ignored, IEnergyService energy, CraftingService service)
    {
        if (AeSkillEffects.getAssemblerInfiniteLevel() == 0 && AeSkillEffects.getCpuInfiniteLevel() < 5)
        {
            ignored.tickCraftingLogic(energy, service);
            return;
        }
        if (god$ticked)
        {
            return;
        }
        god$ticked = true;
        ArrayList<CraftingCPUCluster> order = god$order;
        if (order.size() != craftingCPUClusters.size() || !craftingCPUClusters.containsAll(order))
        {
            order.clear();
            order.addAll(craftingCPUClusters);
            order.sort(Comparator.comparingInt(System::identityHashCode));
        }
        if (order.isEmpty())
        {
            return;
        }
        int first = Math.floorMod(god$cursor++, order.size());
        for (int i = 0; i < order.size(); i++)
        {
            order.get((first + i) % order.size()).craftingLogic.tickCraftingLogic(energy, service);
        }
    }

    /**
     * 完成量已经记到发起方那台 CPU 上；不要让同级兄弟 CPU 用同一批物品把自己的订单也判定完成
     * （原生 insert 在这种情况下返回 0）。
     */
    @Redirect(method = "insertIntoCpus", at = @At(value = "INVOKE",
            target = "Lappeng/crafting/execution/CraftingCpuLogic;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"),
            require = 0)
    private long god$ownFinalOutput(CraftingCpuLogic logic, AEKey key, long amount, Actionable mode)
    {
        if (InstantCrafting.isDeliveringFinalOutput() && logic.getFinalJobOutput() != null
                && key.matches(logic.getFinalJobOutput()))
        {
            return 0;
        }
        return logic.insert(key, amount, mode);
    }
}
