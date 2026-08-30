package com.godofthings.wand.api;

public interface IWandCore extends IWandUpgrade
{
    int getColor();

    IWandAction getWandAction();
}
