package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.MagicAttributeBridge;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 魔法增幅的属性维护：穿齐全套 → 每 20 tick 按技能等级挂上外部魔法 mod 的属性修正；
 * 脱下套装 → 立刻移除全部修正。
 * <p>独立成一个事件处理器，不侵入已有的技能 tick 逻辑；幂等，异常不外抛。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class MagicAttributeHandler
{
    private MagicAttributeHandler() {}

    /** 自检日志只打一次 */
    private static boolean LOGGED = false;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0)
        {
            return;
        }
        try
        {
            if (!LOGGED)
            {
                LOGGED = true;
                java.util.List<String> ok = MagicAttributeBridge.availableSkills();
                java.util.List<String> all = MagicAttributeBridge.allSkills();
                java.util.List<String> missing = new java.util.ArrayList<>(all);
                missing.removeAll(ok);
                LOGGER.info("[Godofthings] 魔法增幅属性自检：已匹配 {} / {} 个技能；未匹配（对应 mod 未提供该属性，技能将不生效）：{}",
                        ok.size(), all.size(), missing);
            }
            if (GodArmorHandler.isFullSetWorn(player))
            {
                MagicAttributeBridge.ensure(player, ArmorSkillData.get(player));
            }
            else
            {
                MagicAttributeBridge.removeAll(player);
            }
        }
        catch (Exception ignored)
        {
            // 外部 mod 属性异常绝不影响游戏运行
        }
    }
}