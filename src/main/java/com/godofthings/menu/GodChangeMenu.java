package com.godofthings.menu;

import com.godofthings.Godofthings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 神之更改菜单：无槽位，纯按钮界面。
 * 按钮：1=早上 2=中午 3=晚上；4=晴朗 5=下雨 6=雷暴。
 * 由服务端处理，直接修改所在维度的时间和天气。
 */
public class GodChangeMenu extends AbstractContainerMenu
{
    public GodChangeMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId);
    }

    public GodChangeMenu(int containerId)
    {
        super(Godofthings.GOD_CHANGE_MENU.get(), containerId);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel))
        {
            return false;
        }
        switch (buttonId)
        {
            // ---- 时间 ----
            case 1 -> setTime(serverLevel, 0);        // 早上 6:00
            case 2 -> setTime(serverLevel, 6000);     // 中午 12:00
            case 3 -> setTime(serverLevel, 18000);    // 晚上 22:00
            // ---- 天气 ----
            case 4 -> serverLevel.setWeatherParameters(0, 0, false, false);   // 晴朗
            case 5 -> serverLevel.setWeatherParameters(0, 12000, true, false); // 下雨
            case 6 -> serverLevel.setWeatherParameters(0, 12000, true, true);  // 雷暴
            default -> { return false; }
        }
        return true;
    }

    /** 设置世界时间为指定刻（0=日出，6000=正午，18000=午夜），保持天数进度不变 */
    private static void setTime(ServerLevel level, int target)
    {
        long dayTime = level.getDayTime();
        long day = dayTime / 24000L;
        long newTime = day * 24000L + target;
        level.setDayTime(newTime);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
