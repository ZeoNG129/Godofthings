package com.godofthings.block.entity;

/**
 * 熔炉某个面的自动输入/输出模式。
 * NONE   = 不启用，漏斗/自动化对其无任何互动
 * INPUT  = 该面自动抽取相邻容器物品进输入格
 * OUTPUT = 该面把输出格物品自动推送给相邻容器
 * BOTH   = 同一个面既自动抽入原料，又自动推出产物（输入和输出）
 */
public enum FaceMode
{
    NONE(0),
    INPUT(1),
    OUTPUT(2),
    BOTH(3);

    private final int id;

    FaceMode(int id)
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }

    public static FaceMode fromId(int id)
    {
        return switch (((id % 4) + 4) % 4)
        {
            case 1 -> INPUT;
            case 2 -> OUTPUT;
            case 3 -> BOTH;
            default -> NONE;
        };
    }
}
