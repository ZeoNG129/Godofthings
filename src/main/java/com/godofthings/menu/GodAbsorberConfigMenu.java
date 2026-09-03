package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodAbsorberBlockEntity;
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

/**
 * 神之吸收面配置菜单：无槽位，6 个面模式 DataSlot，按钮 0-5 循环面，6 返回主界面。
 */
public class GodAbsorberConfigMenu extends AbstractContainerMenu
{
    private final GodAbsorberBlockEntity be;
    private final ContainerLevelAccess access;

    public GodAbsorberConfigMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodAbsorberBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodAbsorberConfigMenu(int containerId, Inventory playerInv, GodAbsorberBlockEntity be)
    {
        super(Godofthings.GOD_ABSORBER_CONFIG_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            this.addDataSlot(new DataSlot()
            {
                @Override public int get() { return be.getFaceMode(Direction.values()[idx]); }
                @Override public void set(int value) { be.setFaceMode(Direction.values()[idx], value); }
            });
        }
    }

    public GodAbsorberBlockEntity getBlockEntity() { return be; }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId >= 0 && buttonId < 6)
        {
            be.cycleFaceMode(Direction.values()[buttonId]);
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 6 && player instanceof ServerPlayer sp)
        {
            sp.openMenu(new MenuProvider()
            {
                @Override public Component getDisplayName() { return Component.translatable("container.godofthings.god_absorber"); }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new GodAbsorberMenu(id, inv, be); }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_ABSORBER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
