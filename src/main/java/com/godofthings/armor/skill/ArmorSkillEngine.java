package com.godofthings.armor.skill;

import java.util.Map;

/**
 * 神之套装技能的效果引擎（掉落侧）。
 *
 * <p><b>v5.12.1 死代码清理</b>：属性修饰符引擎（{@code applyAll} / {@code removeAll}）已删除。
 * v5.9.0 按用户要求删掉「基础属性 / 特殊增幅 / 魔法增幅」等属性类节点后，
 * 全部现役节点的属性效果列表恒为空，{@code applyAll} / {@code removeAll} 遍历空列表
 * → <b>每次调用都是空转</b>，连带「物理减伤」自定义属性（{@code ModAttributes.DAMAGE_REDUCTION}，
 * 已注册却无任何技能使用）与 {@code RangedAttributeMixin}（原版属性解上限，白名单失去受益者）
 * 一起成为死设施，三者已一并移除。若将来重新引入属性类节点，
 * 从 VERSIONING.md 的 5.12.1 条目回溯当时的实现（Zifeng Skill Tree 的
 * {@code SkillEffects.applyAll} 等价物：ADD_VALUE / ADD_MULTIPLIED_TOTAL 修饰符）。</p>
 *
 * <p>现役的 7 个掉落类节点走事件侧（{@code ArmorSkillHandler} 的掉落 / 经验事件），
 * 以下倍率换算函数是它们的唯一数值入口。</p>
 */
public final class ArmorSkillEngine
{
    private ArmorSkillEngine() {}

    // ══════════ 7 个开关式节点的数值换算（掉落 / 经验 / 怪蛋 / 头颅 / 熔炼） ══════════

    /** 生物掉落倍率（猎魂丰收）：1 + 等级（每级 +1 倍） */
    public static double mobDropMultiplier(Map<String, Integer> levels)
    {
        return 1 + ArmorSkillData.effectiveLevel(levels, ArmorSkills.MOB_DROP);
    }

    /** 方块掉落倍率（点石成金）：1 + 等级 */
    public static double blockDropMultiplier(Map<String, Integer> levels)
    {
        return 1 + ArmorSkillData.effectiveLevel(levels, ArmorSkills.BLOCK_DROP);
    }

    /** 战利品爆炸（财源滚滚）：1 + 等级（每级掉落翻一倍） */
    public static double lootBombMultiplier(Map<String, Integer> levels)
    {
        return 1 + ArmorSkillData.effectiveLevel(levels, ArmorSkills.LOOT_BOMB);
    }

    /** 经验倍率（经验飞涨）：1 + 等级 × 2 */
    public static double xpMultiplier(Map<String, Integer> levels)
    {
        return 1 + ArmorSkillData.effectiveLevel(levels, ArmorSkills.XP_GAIN) * 2.0;
    }

    /** 便捷：某技能是否已开启 */
    public static boolean isOn(Map<String, Integer> levels, String skillId)
    {
        return ArmorSkillData.isEnabled(levels, skillId);
    }

    /** 妖魂凝卵：掉落刷怪蛋的概率（每级 10%） */
    public static double spawnEggChance(Map<String, Integer> levels)
    {
        return Math.min(1.0, ArmorSkillData.effectiveLevel(levels, ArmorSkills.MOB_SPAWN_EGG) * 0.10);
    }

    /** 斩首夺颅：掉落头颅的概率（每级 20%） */
    public static double headDropChance(Map<String, Integer> levels)
    {
        return Math.min(1.0, ArmorSkillData.effectiveLevel(levels, ArmorSkills.MOB_HEAD) * 0.20);
    }

    /** 该生物类型对应的头颅物品（无对应则返回 null） */
    public static net.minecraft.world.item.Item headItemFor(net.minecraft.world.entity.EntityType<?> type)
    {
        if (type == net.minecraft.world.entity.EntityType.ZOMBIE) return net.minecraft.world.item.Items.ZOMBIE_HEAD;
        if (type == net.minecraft.world.entity.EntityType.SKELETON) return net.minecraft.world.item.Items.SKELETON_SKULL;
        if (type == net.minecraft.world.entity.EntityType.WITHER_SKELETON) return net.minecraft.world.item.Items.WITHER_SKELETON_SKULL;
        if (type == net.minecraft.world.entity.EntityType.CREEPER) return net.minecraft.world.item.Items.CREEPER_HEAD;
        if (type == net.minecraft.world.entity.EntityType.ENDER_DRAGON) return net.minecraft.world.item.Items.DRAGON_HEAD;
        if (type == net.minecraft.world.entity.EntityType.PIGLIN) return net.minecraft.world.item.Items.PIGLIN_HEAD;
        if (type == net.minecraft.world.entity.EntityType.PLAYER) return net.minecraft.world.item.Items.PLAYER_HEAD;
        if (type == net.minecraft.world.entity.EntityType.ZOMBIFIED_PIGLIN) return net.minecraft.world.item.Items.ZOMBIE_HEAD;
        return null;
    }

    /** 该生物类型对应的刷怪蛋（原版 byId 查表；没有对应刷怪蛋返回 null） */
    public static net.minecraft.world.item.Item spawnEggFor(net.minecraft.world.entity.EntityType<?> type)
    {
        return net.minecraft.world.item.SpawnEggItem.byId(type);
    }
}
