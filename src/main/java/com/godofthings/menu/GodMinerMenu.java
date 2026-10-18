package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodMinerBlockEntity;
import com.godofthings.godgrid.AbstractGodGridMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.block.Block;

/**
 * 神之矿机的容器：网格槽位（17 输入占位 + 17 输出 + 加速槽）、玩家物品栏、以及
 * 「面配置 / AE / 排序」三个按钮全部由 {@link AbstractGodGridMenu} 统一提供，
 * 本类只保留矿机特有的按钮（开始-停止 / 半径调整 / 时运 3 / 精准采集）。
 */
public class GodMinerMenu extends AbstractGodGridMenu
{
    /** 本机特有按钮 id：6 / 10 / 12 已被基类占用（面配置 / AE 开关 / 排序） */
    public static final int BTN_RUN = 0;
    public static final int BTN_FORTUNE = 7;
    public static final int BTN_SILK_TOUCH = 8;
    /** 半径调整六档（-100 / -10 / -1 / +1 / +10 / +100）占 BTN_RADIUS_BASE .. BTN_RADIUS_BASE+5 */
    public static final int BTN_RADIUS_BASE = 20;

    private final GodMinerBlockEntity miner;

    public GodMinerMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodMinerBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodMinerMenu(int containerId, Inventory playerInv, GodMinerBlockEntity be)
    {
        super(Godofthings.GOD_MINER_MENU.get(), containerId, playerInv, be);
        this.miner = be;

        // 运行 / 半径 / 当前深度：仅界面显示用（AE 开关与排序模式由基类同步）
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.isRunning() ? 1 : 0; }
            @Override
            public void set(int value) { be.setRunning(value != 0); }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.getRadius(); }
            @Override
            public void set(int value) { be.setRadius(value); }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.getCurrentY(); }
            @Override
            public void set(int value) { be.setCurrentY(value); }
        });
    }

    /** 界面取矿机状态用（基类只暴露 BlockEntity） */
    public GodMinerBlockEntity getMiner()
    {
        return miner;
    }

    @Override
    protected Block gridBlock()
    {
        return Godofthings.GOD_MINER.get();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        // 先交给基类：6=面配置 / 10=AE 开关 / 12=排序
        if (super.clickMenuButton(player, buttonId))
        {
            return true;
        }
        // 服务端守卫：玩家必须在方块 8 格内且同维度，防止异常客户端/跨维度操控
        if (miner.getLevel() == null || miner.getLevel().dimension() != player.level().dimension()
                || player.distanceToSqr(miner.getBlockPos().getX() + 0.5,
                miner.getBlockPos().getY() + 0.5, miner.getBlockPos().getZ() + 0.5) > 8.0 * 8.0)
        {
            return false;
        }
        if (buttonId == BTN_RUN)
        {
            // 服务端权威翻转运行状态；客户端的乐观更新会由 broadcastChanges 的 DataSlot 收敛
            miner.setRunning(!miner.isRunning());
            this.broadcastChanges();
            return true;
        }
        if (buttonId >= BTN_RADIUS_BASE && buttonId < BTN_RADIUS_BASE + 6)
        {
            int delta = switch (buttonId - BTN_RADIUS_BASE)
            {
                case 0 -> -100;
                case 1 -> -10;
                case 2 -> -1;
                case 3 -> 1;
                case 4 -> 10;
                default -> 100;
            };
            miner.setRadius(miner.getRadius() + delta); // setRadius 内部已 clamp 到 1..maxRadius
            this.broadcastChanges();
            return true;
        }
        if (buttonId == BTN_FORTUNE)
        {
            // 时运 3（与精准采集互斥）
            miner.toggleFortune();
            this.broadcastChanges();
            return true;
        }
        if (buttonId == BTN_SILK_TOUCH)
        {
            // 精准采集（与时运互斥）
            miner.toggleSilkTouch();
            this.broadcastChanges();
            return true;
        }
        return false;
    }
}
