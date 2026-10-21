package com.godofthings.compass.explorers.util;

/**
 * 探险者指南针的状态机（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>存进物品的自定义数据时写的是 {@link #getID()}（int），读回来用 {@link #fromID(Integer)}。</p>
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
