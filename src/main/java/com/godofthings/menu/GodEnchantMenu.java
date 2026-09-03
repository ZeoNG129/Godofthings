package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodEnchantBlockEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

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
    /** 当前侧（服务端 ServerLevel / 客户端 ClientLevel）附魔注册表。绝不能静态缓存：
     *  单人游戏中服务端与客户端在同一 JVM，静态字段会被稍后构造的客户端菜单实例覆盖成客户端注册表，
     *  导致服务端 applyEnchant 用客户端注册表的 Enchantment 对象写入附魔，序列化 container_set_slot
     *  时服务端注册表找不到 id 而踢人（"Can't find id for ... in map"）。 */
    private final Registry<Enchantment> enchantRegistry;
    private final List<Enchantment> enchantList;
    private int selectedIndex = 0;
    private int selectedLevel = 1;
    private int cachedAeEnabled = 1;

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
        // 1.21.1：附魔注册表为数据驱动（无 BuiltInRegistries.ENCHANTMENT）。用本菜单实例所在侧（服务端/客户端）
        // 的注册表，MappedRegistry 迭代顺序即注册表 ID 顺序（等价旧 ForgeRegistries.getValues()）。
        this.enchantRegistry = be.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        this.enchantList = new ArrayList<>(this.enchantRegistry.stream().toList());

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
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 236));
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
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isAeEnabled() ? 1 : 0; }
            @Override public void set(int value) { cachedAeEnabled = value; }
        });
    }

    public boolean isAeEnabled()
    {
        return cachedAeEnabled == 1;
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

    /** 注册表对象 → 组件键 Holder（附魔组件以 Holder<Enchantment> 为键）。
     *  必须用 getResourceKey→getHolderOrThrow 取注册表内真正有 id 的 Holder.Reference；
     *  wrapAsHolder 产生的 Reference 无 id，ItemStack 同步到客户端（container_set_slot）编码附魔组件时
     *  会抛 "Can't find id for ... in map" 导致玩家被踢。 */
    private Holder<Enchantment> holderOf(Enchantment ench)
    {
        return this.enchantRegistry.getResourceKey(ench)
                .<Holder<Enchantment>>map(this.enchantRegistry::getHolderOrThrow)
                .orElseGet(() -> this.enchantRegistry.wrapAsHolder(ench));
    }

    /** 本菜单实例的附魔列表（注册表 ID 顺序） */
    public List<Enchantment> enchantList()
    {
        return this.enchantList;
    }

    /**
     * 当前输入槽物品可附的魔咒列表（过滤掉不兼容的，如铁镐不能附保护）。
     * 服务端与客户端都用此方法，保证按钮索引一致。
     */
    public List<Enchantment> enchantListFor(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Enchantment> filtered = new ArrayList<>();
        for (Enchantment ench : this.enchantList)
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
        if (buttonId == 5)
        {
            be.toggleAeEnabled();
            this.broadcastChanges();
            return true;
        }
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
        // 1.21.1：附魔组件化。注意 updateEnchantments 对无附魔组件的物品不写入，
        // 故保持旧逻辑 get→put→set 三步（getEnchantmentsForCrafting/setEnchantments 自动兼容附魔书组件）。
        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
        mutable.set(holderOf(ench), selectedLevel);
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
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
        // 1.21.1：置空附魔组件（setEnchantments 对附魔书写 STORED_ENCHANTMENTS，与读取对称）
        EnchantmentHelper.setEnchantments(stack, ItemEnchantments.EMPTY);
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
        Holder<Enchantment> enchHolder = holderOf(ench);
        int applied = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ench.canEnchant(stack))
            {
                continue;
            }
            // 1.21.1：isCompatibleWith 已删，用静态 Enchantment.areCompatible(Holder, Holder)（互斥集双向判定）；
            // 同附魔不算冲突（保留旧“put 覆盖等级”行为）
            ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            boolean conflict = existing.keySet().stream().anyMatch(h -> h.value() != ench && !Enchantment.areCompatible(h, enchHolder));
            if (conflict)
            {
                continue;
            }
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
            mutable.set(enchHolder, selectedLevel);
            EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
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
