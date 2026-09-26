package com.godofthings.armor.skill;

import com.godofthings.Godofthings;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * 神之套装技能等级的读写入口。
 * <p>
 * 服务端：权威值存在玩家的 {@code godofthings:armor_skills} 数据附件里
 * （{@code Map<技能id, 等级>}，0/不存在 = 未解锁；随玩家存档持久化、死亡复制）。
 * <p>
 * 客户端：不写附件，用静态镜像承接服务端推来的整张表（{@code ArmorSkillSyncPayload}），
 * 界面绘制与客户端侧判断读到的都是同一份数据。
 */
public final class ArmorSkillData
{
    /** 客户端镜像（服务端在登录、每次改动、客户端主动请求时推送） */
    private static volatile Map<String, Integer> clientLevels = Map.of();

    private ArmorSkillData() {}

    /** 取该玩家全部技能等级（自动区分物理端）。 */
    public static Map<String, Integer> get(Player player)
    {
        if (player.level().isClientSide)
        {
            return clientLevels;
        }
        return player.getData(Godofthings.ARMOR_SKILLS);
    }

    public static int level(Map<String, Integer> levels, String skillId)
    {
        Integer lv = levels.get(skillId);
        return lv == null ? 0 : Math.max(0, lv);
    }

    public static int level(Player player, String skillId)
    {
        return level(get(player), skillId);
    }

    /** 服务端写入单个技能的等级（0 = 关闭）。 */
    public static void setLevel(Player player, String skillId, int level)
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
            levels.put(skillId, clamped);
        }
        player.setData(Godofthings.ARMOR_SKILLS, levels);
    }

    /** 服务端清空全部技能。 */
    public static void clearAll(Player player)
    {
        player.setData(Godofthings.ARMOR_SKILLS, new HashMap<>());
    }

    /** 服务端把指定分类整体设为某等级（0 = 全关）。 */
    public static void setCategoryLevel(Player player, ArmorSkillCategory category, int level)
    {
        Map<String, Integer> levels = new HashMap<>(player.getData(Godofthings.ARMOR_SKILLS));
        for (ArmorSkillDef def : ArmorSkills.of(category))
        {
            if (level <= 0)
            {
                levels.remove(def.id());
            }
            else
            {
                levels.put(def.id(), Math.min(def.maxLevel(), level));
            }
        }
        player.setData(Godofthings.ARMOR_SKILLS, levels);
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
