package com.godofthings.mixin;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 突破原版属性上限。
 * <p>
 * <b>移植自 Zifeng Skill Tree（子枫的百宝箱）的 {@code RangedAttributeMixin}</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）。
 *
 * <h3>为什么必须要有它</h3>
 * 原版 {@link RangedAttribute#sanitizeValue} 会把属性值 clamp 到 {@code [min, max]}，
 * 而原版上限对"技能堆叠"来说非常低，例如：
 * <pre>
 *   MAX_HEALTH        1024   ← 血魄淬炼 100 级 × +20 = +2000 会被砍到 1024
 *   ARMOR               30   ← 磐石之躯 2 级就顶到上限
 *   JUMP_STRENGTH       32
 *   ATTACK_DAMAGE     2048
 * </pre>
 * 没有它，技能树里大量等级投入是<b>无效</b>的。
 *
 * <h3>做法</h3>
 * 只对白名单内的属性移除上限（仍保留下限防负数），其余属性（含其他模组注册的属性）
 * 完全保持原版 clamp → 多模组零误伤。白名单按<b>属性实例引用</b>比较，
 * 不受注册名 / 语言 / 映射变化影响。
 * <p>⚠️ 必须用<b>编译期字段引用</b>取属性（不能用反射字符串——生产环境字段名可能被重映射，
 * 反射会静默失败导致白名单恒为空，参考模组就踩过这个坑）。
 *
 * <h3>安全措施</h3>
 * {@code require = 0}：注入失败只静默跳过（表现退回原版 clamp），绝不崩游戏。
 */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin
{
    @Shadow
    @Final
    private double minValue;

    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void godofthings$removeMaxLimit(double value, CallbackInfoReturnable<Double> cir)
    {
        if (isSkillAttribute((Attribute) (Object) this))
        {
            // 只保留下限（防止负数），移除上限限制
            cir.setReturnValue(Math.max(minValue, value));
        }
    }

    private static volatile java.util.Set<Attribute> UNBOUNDED_CACHE;

    /** 白名单应有条目数（与下方 addAttr 调用数一致，用于完整性断言） */
    private static final int EXPECTED_ATTR_COUNT = 10;

    private static java.util.Set<Attribute> unboundedAttributes()
    {
        java.util.Set<Attribute> s = UNBOUNDED_CACHE;
        if (s != null)
        {
            return s;
        }
        java.util.Set<Attribute> t = new java.util.HashSet<>();
        // 技能树阶段 1 用到的属性（全部用编译期字段引用）
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.MINING_EFFICIENCY);
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.LUCK);
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED);
        // 护甲与韧性：参考模组故意不解（理由是其减伤公式本就封顶 80%）。
        // 本项目仍解开——因为原版 ARMOR 上限只有 30，不解的话「磐石之躯」2 级就完全无效；
        // 解上限后至少数值真实、护甲韧性对高伤害攻击照常生效（减伤公式的 80% 封顶仍由原版决定）。
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        addAttr(t, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        // ⚠️ 只缓存「集齐」的结果：首次调用可能发生在 Attributes 类初始化过程中
        //    （其 static 字段构造 RangedAttribute 时可能间接触发 sanitizeValue），
        //    此时读到的 Attributes.XXX 仍是 null；若把不完整集合缓存下来，白名单将永久缺项。
        if (t.size() >= EXPECTED_ATTR_COUNT)
        {
            UNBOUNDED_CACHE = t;
        }
        return t;
    }

    /** 加入白名单（编译期引用；1.21.1 字段是 Holder&lt;Attribute&gt;，故取 .value()） */
    private static void addAttr(java.util.Set<Attribute> out, net.minecraft.core.Holder<Attribute> holder)
    {
        if (holder != null)
        {
            Attribute a = holder.value();
            if (a != null)
            {
                out.add(a);
            }
        }
    }

    private static boolean isSkillAttribute(Attribute attribute)
    {
        return unboundedAttributes().contains(attribute);
    }
}
