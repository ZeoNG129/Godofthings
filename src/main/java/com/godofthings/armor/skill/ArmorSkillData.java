package com.godofthings.armor.skill;

import com.godofthings.Godofthings;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * 神之套装技能等级的读写入口。
 * <p>
 * 服务端权威值存在玩家的 {@code godofthings:armor_skills} 数据附件里，
 * 客户端用静态镜像承接服务端推来的整张表（{@code ArmorSkillSyncPayload}）。
 *
 * <h3>「开关」与「等级」是两件事（值编码）</h3>
 * 用户要求：<b>关闭技能后再打开，等级必须保留</b>（不能重置回 1 级）。
 * 因此表里存的是<b>带符号等级</b>：
 * <ul>
 *   <li><b>正数</b>：已开启，等级 = 该值</li>
 *   <li><b>负数</b>：已关闭，但<b>记住等级</b> = 绝对值（再次开启即恢复原等级）</li>
 *   <li><b>不存在 / 0</b>：从未解锁（或已被"重置"清掉）</li>
 * </ul>
 * 读等级一律用 {@link #level}（取绝对值），判断是否生效用 {@link #isEnabled}。
 */
public final class ArmorSkillData
{
    /** 客户端镜像（服务端在登录、每次改动、客户端主动请求时推送） */
    private static volatile Map<String, Integer> clientLevels = Map.of();

    private ArmorSkillData() {}

    /** 取该玩家全部技能数据（自动区分物理端）。 */
    public static Map<String, Integer> get(Player player)
    {
        if (player.level().isClientSide)
        {
            return clientLevels;
        }
        return player.getData(Godofthings.ARMOR_SKILLS);
    }

    /** 记住的等级（忽略开关状态，取绝对值）。 */
    public static int level(Map<String, Integer> levels, String skillId)
    {
        Integer v = levels.get(skillId);
        return v == null ? 0 : Math.abs(v);
    }

    /** 是否已开启（值为正）。 */
    public static boolean isEnabled(Map<String, Integer> levels, String skillId)
    {
        Integer v = levels.get(skillId);
        return v != null && v > 0;
    }

    public static int level(Player player, String skillId)
    {
        return level(get(player), skillId);
    }

    public static boolean isEnabled(Player player, String skillId)
    {
        return isEnabled(get(player), skillId);
    }

    /** 生效等级：未开启返回 0，开启返回记住的等级。 */
    public static int effectiveLevel(Map<String, Integer> levels, String skillId)
    {
        return isEnabled(levels, skillId) ? level(levels, skillId) : 0;
    }

    /** 保留开关状态地写入一个新等级（内部用）。level 为 0 表示清除该技能。 */
    private static void write(Player player, String skillId, int level, boolean enabled)
    {
        ArmorSkillDef def = ArmorSkills.get(skillId);
        if (def == null)
        {
            return;
        }
        int clamped = Math.max(0, Math.min(def.maxLevel(), level));
        Map<String, Integer> levels = new HashMap<>(player.getData(Godofthings.ARMOR_SKILLS));
        if (clamped <= 0)
        {
            levels.remove(skillId);
        }
        else
        {
            levels.put(skillId, enabled ? clamped : -clamped);
        }
        player.setData(Godofthings.ARMOR_SKILLS, levels);
    }

    /** 设定等级（并把技能置为开启）；level ≤ 0 表示清除。 */
    public static void setLevel(Player player, String skillId, int level)
    {
        write(player, skillId, level, level > 0);
    }

    /** 只切换开关，<b>保留等级</b>；从未解锁过的技能开启时给默认解锁等级。 */
    public static void setEnabled(Player player, String skillId, boolean on)
    {
        ArmorSkillDef def = ArmorSkills.get(skillId);
        if (def == null)
        {
            return;
        }
        int remembered = level(player, skillId);
        if (on && remembered <= 0)
        {
            write(player, skillId, ArmorSkills.UNLOCK_LEVEL, true);
            return;
        }
        write(player, skillId, Math.max(1, remembered), on);
    }

    /** 服务端清空全部技能（真"重置"，等级一并清除）。 */
    public static void clearAll(Player player)
    {
        player.setData(Godofthings.ARMOR_SKILLS, new HashMap<>());
    }

    /**
     * 整列操作。
     *
     * @param level &gt;0 = 设为该等级并开启；&lt;0 = <b>只关闭、保留等级</b>；0 = 清除该列全部技能
     */
    public static void setCategoryLevel(Player player, ArmorSkillCategory category, int level)
    {
        for (ArmorSkillDef def : ArmorSkills.of(category))
        {
            if (level == 0)
            {
                setLevel(player, def.id(), 0);          // 真清除（重置）
            }
            else if (level < 0)
            {
                setEnabled(player, def.id(), false);    // 只关，等级留着
            }
            else
            {
                setLevel(player, def.id(), level);      // 设等级（会开启）
            }
        }
    }

    // ---- 客户端镜像 ----

    public static Map<String, Integer> getClientLevels()
    {
        return clientLevels;
    }

    public static void setClientLevels(Map<String, Integer> levels)
    {
        clientLevels = Map.copyOf(levels);
    }

    public static int clientLevel(String skillId)
    {
        return level(clientLevels, skillId);
    }
}
