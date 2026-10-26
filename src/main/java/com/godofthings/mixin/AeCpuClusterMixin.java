package com.godofthings.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.godofthings.aeskill.cpu.CpuExtension;
import com.godofthings.aeskill.cpu.VirtualCpus;
import com.godofthings.aeskill.cpu.VirtualCpuState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给 AE2 的合成 CPU 簇挂上「虚拟 CPU」能力。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.AeCpuClusterMixin}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>：注入点、顺序、语义全部照抄，
 * 只把包名、方法前缀（{@code zifeng$ → god$}）和语言键换成本模组。</p>
 *
 * <p>子 CPU 是真实克隆出来的 {@link CraftingCPUCluster}，它没有自己的方块实体，
 * 所以这里把「节点 / 动作源 / 世界 / 脏标记 / 销毁状态 / 名字」全部转发给父簇。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class AeCpuClusterMixin implements CpuExtension
{
    @Unique
    private boolean god$readingBase;

    @Unique
    private final VirtualCpuState god$state = new VirtualCpuState();

    @Override
    public VirtualCpuState god$state()
    {
        return god$state;
    }

    @Override
    public boolean god$readingBase()
    {
        return god$readingBase;
    }

    /** 读未放大的原始存储（重入保护，避免放大逻辑自我递归）。 */
    @Override
    public long god$baseStorage()
    {
        boolean previous = god$readingBase;
        god$readingBase = true;
        try
        {
            return ((CraftingCPUCluster) (Object) this).getAvailableStorage();
        }
        finally
        {
            god$readingBase = previous;
        }
    }

    /** 读未放大的原始并行单元数。 */
    @Override
    public int god$baseCoProcessors()
    {
        boolean previous = god$readingBase;
        god$readingBase = true;
        try
        {
            return ((CraftingCPUCluster) (Object) this).getCoProcessors();
        }
        finally
        {
            god$readingBase = previous;
        }
    }

    @Inject(method = "getNode", at = @At("HEAD"), cancellable = true, require = 1)
    private void god$node(CallbackInfoReturnable<IGridNode> ci)
    {
        if (god$state.parent != null)
        {
            ci.setReturnValue(god$state.parent.getNode());
        }
    }

    @Inject(method = "getSrc", at = @At("HEAD"), cancellable = true, require = 1)
    private void god$source(CallbackInfoReturnable<IActionSource> ci)
    {
        if (god$state.parent != null)
        {
            ci.setReturnValue(god$state.parent.getSrc());
        }
    }

    @Inject(method = "getLevel", at = @At("HEAD"), cancellable = true, require = 1)
    private void god$level(CallbackInfoReturnable<Level> ci)
    {
        if (god$state.parent != null)
        {
            ci.setReturnValue(god$state.parent.getLevel());
        }
    }

    @Inject(method = "markDirty", at = @At("HEAD"), cancellable = true, require = 1)
    private void god$dirty(CallbackInfo ci)
    {
        if (god$state.parent != null)
        {
            god$state.parent.markDirty();
            ci.cancel();
        }
    }

    @Inject(method = "isDestroyed", at = @At("RETURN"), cancellable = true, require = 1)
    private void god$destroyed(CallbackInfoReturnable<Boolean> ci)
    {
        if (god$state.retired || (god$state.parent != null && god$state.parent.isDestroyed()))
        {
            ci.setReturnValue(true);
        }
    }

    /** 子 CPU 在界面上显示成「父CPU名 #槽位」。 */
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true, require = 1)
    private void god$name(CallbackInfoReturnable<Component> ci)
    {
        CraftingCPUCluster parent = god$state.parent;
        if (parent == null)
        {
            return;
        }
        Component name = parent.getName();
        var pos = parent.getBoundsMin();
        if (name == null)
        {
            name = Component.translatable("gui.godofthings.aeskill.cpu_at", pos.getX(), pos.getY(), pos.getZ());
        }
        ci.setReturnValue(Component.translatable("gui.godofthings.aeskill.virtual_cpu", name, god$state.slot + 1));
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), require = 1)
    private void god$save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci)
    {
        VirtualCpus.write((CraftingCPUCluster) (Object) this, tag, registries);
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), require = 1)
    private void god$load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci)
    {
        VirtualCpus.read((CraftingCPUCluster) (Object) this, tag, registries);
    }
}
