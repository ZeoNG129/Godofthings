package com.godofthings.manual;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 手册里的一条。
 *
 * @param id       条目 id（物品 / 方块用注册名，系统条目用 {@code system.<id>}）
 * @param category 分类（决定在哪一页签里）
 * @param icon     列表左侧的小图标（系统条目用一个代表性物品）
 * @param title    标题（物品 / 方块条目直接用该物品的显示名，不额外写语言键）
 * @param body     正文（语言键 {@code manual.godofthings.<id>}，多行用 \n）
 * @param keywords 搜索用的预拼小写串（标题 + 正文 + id），避免每帧重复拼字符串
 */
public record ManualEntry(String id, ManualCategory category, ItemStack icon,
                          Component title, Component body, String keywords)
{
    /** 正文的显示行（先按显式 \n 折，再按宽度折） */
    public boolean matches(String query)
    {
        return query.isEmpty() || keywords.contains(query);
    }
}
