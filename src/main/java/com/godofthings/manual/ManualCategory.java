package com.godofthings.manual;

import net.minecraft.network.chat.Component;

/**
 * 手册的分类。分页签显示，顺序就是这里声明的顺序。
 */
public enum ManualCategory
{
    /** 入门：先看什么、有哪些快捷键与指令 */
    START("start"),
    /** 物品：工具、装备、便签、杂项 */
    ITEM("item"),
    /** 方块：机器与功能方块 */
    BLOCK("block"),
    /** 系统：杖、护甲技能、便签、传送点、维度、AE2、兼容性、常见问题 */
    SYSTEM("system");

    private final String id;

    ManualCategory(String id)
    {
        this.id = id;
    }

    public String id()
    {
        return id;
    }

    /** 页签上显示的名字 */
    public Component label()
    {
        return Component.translatable("gui.godofthings.manual.tab." + id);
    }
}
