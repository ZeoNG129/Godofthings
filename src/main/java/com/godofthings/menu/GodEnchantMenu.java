package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodEnchantBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 神之附魔菜单：任意物品 + 自选附魔与等级（无需条件）。
 * heavenly=true 时选择附魔等级默认为最大上限（天神附魔）。
 * 按钮：0=应用；1=等级+；2=等级-；10+索引=选择附魔。
 */
public class GodEnchantMenu extends AbstractContainerMenu
{
    private final GodEnchantBlockEntity be;
    private final ContainerLevelAccess access;
    private final boolean heavenly;
    private int selectedIndex = 0;
    private int selectedLevel = 1;

    public GodEnchantMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodEnchantBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()),
                extraData.readBoolean());
    }

    public GodEnchantMenu(int containerId, Inventory playerInv, GodEnchantBlockEntity be, boolean heavenly)
    {
        super(Godofthings.GOD_ENCHANT_MENU.get(), containerId);
        this.be = be;
        this.heavenly = heavenly;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 输入槽 x=79 使槽位居中于 176 宽界面
        this.addSlot(new SlotItemHandler(be.getItemHandler(), 0, 79, 35));

        // 物品栏整体下移（按钮区两行后），避免与等级/清除/附魔/批量按钮重叠
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 178 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 232));
        }

        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return selectedIndex; }
            @Override public void set(int value) { selectedIndex = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return selectedLevel; }
            @Override public void set(int value) { selectedLevel = value; }
        });
    }

    public GodEnchantBlockEntity getBlockEntity()
    {
        return be;
    }

    public int getSelectedIndex()
    {
        return selectedIndex;
    }

    public int getSelectedLevel()
    {
        return selectedLevel;
    }

    public boolean isHeavenly()
    {
        return heavenly;
    }

    /** 当前附魔的等级上限：天神附魔突破原版上限（255），普通附魔为原版上限 */
    public int currentMaxLevel()
    {
        List<Enchantment> list = currentList();
        if (selectedIndex >= 0 && selectedIndex < list.size())
        {
            Enchantment ench = list.get(selectedIndex);
            return heavenly ? Godofthings.HEAVENLY_ENCHANT_MAX_LEVEL : ench.getMaxLevel();
        }
        return 1;
    }

    /** 双方一致的附魔列表（注册表顺序），静态缓存避免每次打开界面重复构建 */
    private static List<Enchantment> cachedEnchantList = null;

    public static List<Enchantment> enchantList()
    {
        if (cachedEnchantList == null)
        {
            cachedEnchantList = new ArrayList<>(ForgeRegistries.ENCHANTMENTS.getValues());
        }
        return cachedEnchantList;
    }

    /**
     * 当前输入槽物品可附的魔咒列表（过滤掉不兼容的，如铁镐不能附保护）。
     * 服务端与客户端都用此方法，保证按钮索引一致。
     */
    public static List<Enchantment> enchantListFor(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Enchantment> filtered = new ArrayList<>();
        for (Enchantment ench : enchantList())
        {
            if (ench.canEnchant(stack))
            {
                filtered.add(ench);
            }
        }
        return filtered;
    }

    /** 当前输入槽物品对应的可附魔咒列表 */
    public List<Enchantment> currentList()
    {
        return enchantListFor(be.getItemHandler().getStackInSlot(0));
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        List<Enchantment> list = currentList();
        if (buttonId == 0)
        {
            applyEnchant();
            return true;
        }
        if (buttonId == 3)
        {
            clearEnchant();
            return true;
        }
        if (buttonId == 4)
        {
            applyEnchantToAll(player);
            return true;
        }
        if (buttonId == 1)
        {
            if (selectedIndex >= 0 && selectedIndex < list.size())
            {
                Enchantment ench = list.get(selectedIndex);
                int max = heavenly ? Godofthings.HEAVENLY_ENCHANT_MAX_LEVEL : ench.getMaxLevel();
                if (selectedLevel < max)
                {
                    selectedLevel++;
                }
            }
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 2)
        {
            if (selectedLevel > 1)
            {
                selectedLevel--;
            }
            this.broadcastChanges();
            return true;
        }
        if (buttonId >= 10)
        {
            int index = buttonId - 10;
            if (index >= 0 && index < list.size())
            {
                selectedIndex = index;
                Enchantment ench = list.get(index);
                // 天神附魔：选择即默认最高等级（突破原版，255）
                selectedLevel = heavenly ? Godofthings.HEAVENLY_ENCHANT_MAX_LEVEL : 1;
            }
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    /** 把当前选中的附魔以当前等级附加到物品上（无需任何条件） */
    private void applyEnchant()
    {
        List<Enchantment> list = currentList();
        if (selectedIndex < 0 || selectedIndex >= list.size())
        {
            return;
        }
        Enchantment ench = list.get(selectedIndex);
        ItemStack stack = be.getItemHandler().getStackInSlot(0);
        if (stack.isEmpty())
        {
            return;
        }
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        enchants.put(ench, selectedLevel);
        EnchantmentHelper.setEnchantments(enchants, stack);
        be.getItemHandler().setStackInSlot(0, stack);
        be.setChanged();
    }

    /** 清除物品上的所有附魔 */
    private void clearEnchant()
    {
        ItemStack stack = be.getItemHandler().getStackInSlot(0);
        if (stack.isEmpty())
        {
            return;
        }
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        enchants.clear();
        EnchantmentHelper.setEnchantments(enchants, stack);
        be.getItemHandler().setStackInSlot(0, stack);
        be.setChanged();
        selectedIndex = 0;
        selectedLevel = 1;
        this.broadcastChanges();
    }

    /**
     * 批量模式（天神附魔）：把当前选中的附魔以当前等级应用到玩家物品栏中所有可附魔的物品上。
     * 兼容冲突检测：若物品已有冲突附魔则跳过（保留原有附魔）。
     */
    private void applyEnchantToAll(Player player)
    {
        List<Enchantment> list = currentList();
        if (selectedIndex < 0 || selectedIndex >= list.size())
        {
            return;
        }
        Enchantment ench = list.get(selectedIndex);
        int applied = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ench.canEnchant(stack))
            {
                continue;
            }
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            boolean conflict = enchants.keySet().stream().anyMatch(e -> e != ench && !e.isCompatibleWith(ench));
            if (conflict)
            {
                continue;
            }
            enchants.put(ench, selectedLevel);
            EnchantmentHelper.setEnchantments(enchants, stack);
            applied++;
        }
        be.setChanged();
        if (applied > 0 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
        {
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("gui.godofthings.enchant.batch_done", applied), true);
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        // 神之附魔 / 天神附魔 两个方块都有效
        return stillValid(this.access, player, Godofthings.GOD_ENCHANT.get())
                || stillValid(this.access, player, Godofthings.GOD_HEAVEN_ENCHANT.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index == 0)
            {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                if (!this.moveItemStackTo(stack, 0, 1, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty())
            {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
            if (stack.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }
}
