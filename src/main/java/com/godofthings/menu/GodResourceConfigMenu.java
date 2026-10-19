package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodResourceBlockEntity;
import com.godofthings.block.entity.GodResourceVariant;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 神之资源三机（矿物 / 作物 / 复制）共用的面配置界面菜单（照抄神之熔炉面配置）：
 * 无槽位，只有 6 个面模式 DataSlot。按钮 0-5 = 循环切换对应面模式；按钮 6 = 返回主界面。
 * <p>stillValid 按变体校验各自的方块（矿物 / 作物 / 复制各认自己那一台）。</p>
 */
public class GodResourceConfigMenu extends AbstractContainerMenu
{
    private final GodResourceBlockEntity be;
    private final ContainerLevelAccess access;

    public GodResourceConfigMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodResourceBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodResourceConfigMenu(int containerId, Inventory playerInv, GodResourceBlockEntity be)
    {
        super(Godofthings.GOD_RESOURCE_CONFIG_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 6 个面模式 DataSlot，方向顺序 = Direction.values() = DOWN,UP,NORTH,SOUTH,WEST,EAST
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            this.addDataSlot(new DataSlot()
            {
                @Override
                public int get()
                {
                    return be.getFaceMode(Direction.values()[idx]);
                }

                @Override
                public void set(int value)
                {
                    be.setFaceMode(Direction.values()[idx], value);
                }
            });
        }
    }

    public GodResourceBlockEntity getBlockEntity()
    {
        return be;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId >= 0 && buttonId < 6)
        {
            be.cycleFaceMode(Direction.values()[buttonId]);
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 6 && player instanceof ServerPlayer serverPlayer)
        {
            // 返回主界面（标题沿用菜单提供者给的方块名）
            serverPlayer.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return be.getDisplayName();
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodResourceMenu(containerId, inventory, be);
                }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, blockFor(be.getVariant()));
    }

    /** 按变体返回对应方块（stillValid 校验用，与 {@link GodResourceMenu#gridBlock()} 一致）。 */
    private static Block blockFor(GodResourceVariant variant)
    {
        return switch (variant)
        {
            case ORE -> Godofthings.GOD_ORE_MACHINE.get();
            case CROP -> Godofthings.GOD_CROP_MACHINE.get();
            case DUPLICATE -> Godofthings.GOD_DUPLICATE_MACHINE.get();
        };
    }

    // 无槽位，快速移动无操作
    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
