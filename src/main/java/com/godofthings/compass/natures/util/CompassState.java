package com.godofthings.compass.natures.util;

/**
 * 自然罗盘的状态机（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>存进物品的数据组件时写的是 {@link #getID()}（int），读回来用 {@link #fromID(Integer)}。</p>
 */
public enum CompassState
{
    INACTIVE(0), SEARCHING(1), FOUND(2), NOT_FOUND(3);

    private final int id;

    CompassState(int id)
    {
        this.id = id;
    }

    public int getID()
    {
        return id;
    }

    public static CompassState fromID(Integer id)
    {
        for (CompassState state : values())
        {
            if (state.getID() == id)
            {
                return state;
            }
        }
        return null;
    }
}
