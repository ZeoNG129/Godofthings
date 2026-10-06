package com.godofthings.block.entity;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 机器的主人（神之共鸣归属用）：记录放置机器的玩家 UUID。
 *
 * <p><b>v5.15.4 修复神之共鸣对机器不起效果</b>：共鸣链路是
 * 「机器以<b>主人 UUID</b> 触发事件 / 直查 → {@code ArmorSkillHandler.ownerOf} 按 UUID 反查在线主人
 * → 主人在线 + 穿齐全套 + 对应「神之共鸣」开关打开 → 生效」。
 * 此前机器既不记录主人、假玩家也用固定假 UUID，{@code ownerOf} 永远查不到人，共鸣恒不生效。</p>
 *
 * <p>主人在方块 {@code setPlacedBy} 时写入（各机器方块类覆写），并随方块实体 NBT 持久化。</p>
 */
public interface MachineOwner
{
    /** 记录主人（放置者）；传 null 清除。 */
    void setOwner(@Nullable UUID owner);

    /** 主人 UUID；从未记录（旧存档 / 非玩家放置）时为 null（共鸣视为不生效）。 */
    @Nullable
    UUID getOwner();
}
