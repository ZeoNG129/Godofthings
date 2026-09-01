package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodCraftBlockEntity;
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
 * 神之合成面配置菜单：6 个面模式循环切换。
 */
public class GodCraftConfigMenu extends AbstractContainerMenu
{
    private final GodCraftBlockEntity be;
    private final ContainerLevelAccess access;

    public GodCraftConfigMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodCraftBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodCraftConfigMenu(int containerId, Inventory playerInv, GodCraftBlockEntity be)
    {
        super(Godofthings.GOD_CRAFT_CONFIG_MENU.get(), containerId);
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

    public GodCraftBlockEntity getBlockEntity()
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
            serverPlayer.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("container.godofthings.god_craft");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodCraftMenu(containerId, inventory, be);
                }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_CRAFT.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
