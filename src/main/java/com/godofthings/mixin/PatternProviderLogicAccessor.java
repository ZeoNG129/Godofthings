package com.godofthings.mixin;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Direction;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;

/**
 * 暴露 {@link PatternProviderLogic} 的私有成员，供增强供应器逻辑
 * （无线派发 / 回收扫描 / 自适应倍增）复用 AE2 原版的内部状态与行为
 * （移植自 AE2LT 的 PatternProviderLogicAccessor，仅保留本模组用到的条目）。
 */
@Mixin(PatternProviderLogic.class)
public interface PatternProviderLogicAccessor {

    @Invoker("onPushPatternSuccess")
    void invokeOnPushPatternSuccess(IPatternDetails pattern);

    @Invoker("doWork")
    boolean invokeDoWork();

    @Invoker("hasWorkToDo")
    boolean invokeHasWorkToDo();

    @Accessor("mainNode")
    IManagedGridNode getMainNode();

    @Accessor("host")
    PatternProviderLogicHost getHost();

    /** 全部已加载样板的输入键并集（次要键已丢弃），阻挡模式判断用。 */
    @Accessor("patternInputs")
    Set<AEKey> getPatternInputs();

    @Accessor("sendList")
    List<GenericStack> getSendList();

    @Invoker("addToSendList")
    void invokeAddToSendList(AEKey what, long amount);

    @Accessor("sendDirection")
    Direction getSendDirection();

    @Accessor("sendDirection")
    void setSendDirection(Direction direction);
}
