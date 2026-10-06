package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkills;
import com.godofthings.handler.ArmorSkillHandler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * 神之共鸣（v5.15.4 修复）回归测试：机器共鸣判定纯逻辑。
 *
 * <p>共鸣生效链：机器以主人 UUID 触发事件 / 直查 → {@code ownerOf} 反查在线主人
 * → 主人在线 + 穿齐全套 + 对应「神之共鸣」开关开。此前机器既不记录主人、假玩家也用固定假 UUID，
 * 共鸣恒不生效；本次修复后离线主人 / 未记录主人 / 开关关都不生效（三条回归）。</p>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodResonanceGameTest
{
    private static final String TEMPLATE = "note_data";

    @GameTest(template = TEMPLATE)
    public static void offlineOrNullOwnerNeverResonates(GameTestHelper helper)
    {
        ServerLevel level = (ServerLevel) helper.getLevel();
        // 主人不在线（随机 UUID）→ 不生效
        helper.assertFalse(ArmorSkillHandler.resonanceFor(level, UUID.randomUUID(), ArmorSkills.MACHINE_LOOT_BOMB),
                "主人离线时共鸣不应生效");
        // 主人未记录（旧存档 / 非玩家放置）→ 不生效
        helper.assertFalse(ArmorSkillHandler.resonanceFor(level, null, ArmorSkills.MACHINE_LOOT_BOMB),
                "未记录主人时共鸣不应生效");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boostHelpersDefaultToNeutral(GameTestHelper helper)
    {
        ServerLevel level = (ServerLevel) helper.getLevel();
        // 未记录主人：所有倍率回退中性值（同旧版行为）
        helper.assertTrue(ArmorSkillHandler.lootBombBoost(level, null) == 1.0, "未记录主人时掉落倍率应为 1");
        helper.assertTrue(ArmorSkillHandler.blockDropBoost(level, null) == 1.0, "未记录主人时方块倍率应为 1");
        helper.assertTrue(ArmorSkillHandler.xpBoost(level, null) == 1.0, "未记录主人时经验倍率应为 1");
        helper.assertFalse(ArmorSkillHandler.autoSmeltOn(level, null), "未记录主人时自动熔炼应关");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void resonanceTogglesRoundTrip(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        // 共鸣开关存储往返（机器继承增幅的前提：主人侧开关开）
        ArmorSkillData.setEnabled(player, ArmorSkills.LOOT_BOMB, true);
        ArmorSkillData.setEnabled(player, ArmorSkills.MACHINE_LOOT_BOMB, true);
        helper.assertTrue(ArmorSkillData.isEnabled(player, ArmorSkills.LOOT_BOMB), "基础增幅「神之掉落」应已开");
        helper.assertTrue(ArmorSkillData.isEnabled(player, ArmorSkills.MACHINE_LOOT_BOMB), "共鸣开关「神之共鸣·掉落」应已开");
        ArmorSkillData.setEnabled(player, ArmorSkills.MACHINE_LOOT_BOMB, false);
        helper.assertFalse(ArmorSkillData.isEnabled(player, ArmorSkills.MACHINE_LOOT_BOMB),
                "共鸣开关关掉后机器不再继承增幅");
        helper.succeed();
    }
}
