package com.godofthings.armor.skill;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔法增幅（MAGIC 26 个）的属性桥接：把技能等级换算成外部魔法 mod 的属性修正。
 *
 * <h3>为什么可以"零编译依赖"</h3>
 * NeoForge 里所有属性（含其它 mod 注册的）最终都在原版属性注册表 {@link BuiltInRegistries#ATTRIBUTE} 中，
 * 因此只要知道<b>属性 ID 字符串</b>就能取到它 —— <b>完全不需要 import 铁魔法 / 新生魔艺 / Goety 的任何类</b>。
 * 这也意味着：即使这三个 mod 一个都没装，本类也只是"找不到属性 → 什么都不做"，<b>绝不会有
 * NoClassDefFoundError</b>，天然满足可选依赖（soft dependency）要求。
 *
 * <h3>解析策略</h3>
 * 每个技能给一串<b>候选属性 ID</b>，按顺序找第一个存在的；全都找不到时，退化为
 * <b>在该 mod 的命名空间里按关键词扫描</b>（见 scan 方法），这样即使 mod 更新改名也大概率仍能命中。
 *
 * <h3>数值（每级）</h3>
 * 百分比类属性（法术强度 / 冷却缩减 / 咏唱缩短）用 {@code ADD_VALUE} 直接叠加到 1.0 基数上；
 * 上限类（魔力上限）用 {@code ADD_VALUE} 加固定值。数值偏保守，且这些 mod 内部自身还有上限。
 */
public final class MagicAttributeBridge
{
    /** 修正器 ID 前缀（每个技能一个，便于精确移除） */
    private static final String MOD_PREFIX = "godofthings:magic_";

    /** 一个技能的增幅定义：每级数值 + 运算方式 + 候选属性 ID + 兜底扫描关键词 */
    private record Boost(double perLevel, AttributeModifier.Operation op,
                         List<String> candidates, String namespace, List<String> keywords) {}

    private static final Map<String, Boost> BOOSTS = new LinkedHashMap<>();

    private static void put(String skillId, double perLevel, AttributeModifier.Operation op,
                            String namespace, List<String> keywords, String... candidates)
    {
        BOOSTS.put(skillId, new Boost(perLevel, op, List.of(candidates), namespace, keywords));
    }

    static
    {
        AttributeModifier.Operation FLAT = AttributeModifier.Operation.ADD_VALUE;
        AttributeModifier.Operation PCT = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

        // ---- 通用 / 新生魔艺（2 个）----
        put(ArmorSkills.MANA_AMP, 0.10, PCT, "ars_nouveau", List.of("max", "mana"),
                "ars_nouveau:max_mana", "ars_nouveau:mana_boost", "irons_spellbooks:max_mana");
        put(ArmorSkills.ARS_MANA_REGEN, 0.10, PCT, "ars_nouveau", List.of("regen", "mana"),
                "ars_nouveau:mana_regen", "ars_nouveau:mana_regen_bonus", "irons_spellbooks:mana_regen");

        // ---- 铁魔法（13 个）----
        put(ArmorSkills.IRON_MANA_AMP, 10.0, FLAT, "irons_spellbooks", List.of("max", "mana"),
                "irons_spellbooks:max_mana");
        put(ArmorSkills.IRON_MANA_REGEN, 0.05, FLAT, "irons_spellbooks", List.of("regen", "mana"),
                "irons_spellbooks:mana_regen");
        put(ArmorSkills.IRON_CAST_TIME, 0.005, FLAT, "irons_spellbooks", List.of("cast", "time"),
                "irons_spellbooks:cast_time_reduction");
        put(ArmorSkills.IRON_COOLDOWN, 0.005, FLAT, "irons_spellbooks", List.of("cooldown"),
                "irons_spellbooks:cooldown_reduction");
        put(ArmorSkills.IRON_FIRE, 0.05, FLAT, "irons_spellbooks", List.of("fire", "power"),
                "irons_spellbooks:fire_spell_power");
        put(ArmorSkills.IRON_ICE, 0.05, FLAT, "irons_spellbooks", List.of("ice", "power"),
                "irons_spellbooks:ice_spell_power");
        put(ArmorSkills.IRON_LIGHTNING, 0.05, FLAT, "irons_spellbooks", List.of("lightning", "power"),
                "irons_spellbooks:lightning_spell_power");
        put(ArmorSkills.IRON_HOLY, 0.05, FLAT, "irons_spellbooks", List.of("holy", "power"),
                "irons_spellbooks:holy_spell_power");
        put(ArmorSkills.IRON_ENDER, 0.05, FLAT, "irons_spellbooks", List.of("ender", "power"),
                "irons_spellbooks:ender_spell_power");
        put(ArmorSkills.IRON_BLOOD, 0.05, FLAT, "irons_spellbooks", List.of("blood", "power"),
                "irons_spellbooks:blood_spell_power");
        put(ArmorSkills.IRON_EVOCATION, 0.05, FLAT, "irons_spellbooks", List.of("evocation", "power"),
                "irons_spellbooks:evocation_spell_power");
        put(ArmorSkills.IRON_NATURE, 0.05, FLAT, "irons_spellbooks", List.of("nature", "power"),
                "irons_spellbooks:nature_spell_power");
        put(ArmorSkills.IRON_ELDRITCH, 0.05, FLAT, "irons_spellbooks", List.of("eldritch", "power"),
                "irons_spellbooks:eldritch_spell_power");

        // ---- Goety（11 个）----
        put(ArmorSkills.GOETY_POTENCY, 0.05, PCT, "goety", List.of("potency"), "goety:focus_potency", "goety:potency");
        put(ArmorSkills.GOETY_SOUL_DISCOUNT, 0.02, PCT, "goety", List.of("discount", "soul"),
                "goety:soul_discount", "goety:soul_cost_reduction");
        put(ArmorSkills.GOETY_ABYSS, 0.05, FLAT, "goety", List.of("abyss", "power"), "goety:abyss_power");
        put(ArmorSkills.GOETY_FROST, 0.05, FLAT, "goety", List.of("frost", "power"), "goety:frost_power");
        put(ArmorSkills.GOETY_GEOMANCY, 0.05, FLAT, "goety", List.of("geomancy", "power"), "goety:geomancy_power");
        put(ArmorSkills.GOETY_NECROMANCY, 0.05, FLAT, "goety", List.of("necromancy", "power"), "goety:necromancy_power");
        put(ArmorSkills.GOETY_NETHER, 0.05, FLAT, "goety", List.of("nether", "power"), "goety:nether_power");
        put(ArmorSkills.GOETY_STORM, 0.05, FLAT, "goety", List.of("storm", "power"), "goety:storm_power");
        put(ArmorSkills.GOETY_VOID, 0.05, FLAT, "goety", List.of("void", "power"), "goety:void_power");
        put(ArmorSkills.GOETY_WILD, 0.05, FLAT, "goety", List.of("wild", "power"), "goety:wild_power");
        put(ArmorSkills.GOETY_WIND, 0.05, FLAT, "goety", List.of("wind", "power"), "goety:wind_power");
    }

    /** 缓存的解析结果（属性一旦解析成功就长期有效；null 表示"该 mod 没这个属性"） */
    private static final Map<String, Holder<Attribute>> RESOLVED = new LinkedHashMap<>();

    private MagicAttributeBridge() {}

    private static ResourceLocation id(String s)
    {
        return ResourceLocation.tryParse(s);
    }

    /** 精确候选 → 命名空间关键词扫描，两级解析 */
    private static Holder<Attribute> resolve(String skillId, Boost boost)
    {
        if (RESOLVED.containsKey(skillId))
        {
            return RESOLVED.get(skillId);
        }
        Holder<Attribute> found = null;
        for (String candidate : boost.candidates())
        {
            ResourceLocation rl = id(candidate);
            if (rl != null && BuiltInRegistries.ATTRIBUTE.containsKey(rl))
            {
                found = BuiltInRegistries.ATTRIBUTE.getHolder(rl).orElse(null);
                break;
            }
        }
        if (found == null)
        {
            found = scan(boost.namespace(), boost.keywords());
        }
        RESOLVED.put(skillId, found);
        return found;
    }

    /** 兜底：在该 mod 的命名空间里找同时命中全部关键词的属性（改名也能命中） */
    private static Holder<Attribute> scan(String namespace, List<String> keywords)
    {
        for (Map.Entry<net.minecraft.resources.ResourceKey<Attribute>, Attribute> e
                : BuiltInRegistries.ATTRIBUTE.entrySet())
        {
            ResourceLocation rl = e.getKey().location();
            if (!rl.getNamespace().equals(namespace))
            {
                continue;
            }
            String path = rl.getPath();
            boolean all = true;
            for (String kw : keywords)
            {
                if (!path.contains(kw))
                {
                    all = false;
                    break;
                }
            }
            if (all)
            {
                return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(e.getValue());
            }
        }
        return null;
    }

    /** 每个技能的修正器 ID */
    private static ResourceLocation modifierId(String skillId)
    {
        return ResourceLocation.fromNamespaceAndPath("godofthings", "magic_" + skillId);
    }

    /**
     * 按当前技能等级挂上 / 更新全部魔法增幅（幂等：先移除旧的再加新的）。
     * 外部 mod 没装的技能会自动跳过（解析不到属性），不报错。
     */
    public static void ensure(ServerPlayer player, Map<String, Integer> levels)
    {
        for (Map.Entry<String, Boost> entry : BOOSTS.entrySet())
        {
            String skillId = entry.getKey();
            Boost boost = entry.getValue();
            int level = ArmorSkillData.effectiveLevel(levels, skillId);
            Holder<Attribute> attribute = level > 0 ? resolve(skillId, boost) : null;
            if (attribute == null)
            {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null)
            {
                continue;
            }
            ResourceLocation modId = modifierId(skillId);
            instance.removeModifier(modId); // 幂等：先清旧的
            if (level > 0)
            {
                instance.addTransientModifier(new AttributeModifier(
                        modId, boost.perLevel() * level, boost.op()));
            }
        }
    }

    /** 移除全部魔法增幅修正（脱下套装 / 关技能时调用） */
    public static void removeAll(ServerPlayer player)
    {
        for (Map.Entry<String, Boost> entry : BOOSTS.entrySet())
        {
            String skillId = entry.getKey();
            Holder<Attribute> attribute = RESOLVED.get(skillId);
            if (attribute == null)
            {
                attribute = resolve(skillId, entry.getValue());
            }
            if (attribute == null)
            {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null)
            {
                instance.removeModifier(modifierId(skillId));
            }
        }
    }

    /** 全部魔法增幅技能 id（自检用） */
    public static List<String> allSkills()
    {
        return new ArrayList<>(BOOSTS.keySet());
    }

    /** 当前能解析到属性的技能（用于日志自检） */
    public static List<String> availableSkills()
    {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Boost> entry : BOOSTS.entrySet())
        {
            if (resolve(entry.getKey(), entry.getValue()) != null)
            {
                out.add(entry.getKey());
            }
        }
        return out;
    }
}
