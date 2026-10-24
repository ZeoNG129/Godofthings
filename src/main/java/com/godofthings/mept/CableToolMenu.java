package com.godofthings.mept;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import appeng.api.util.AEColor;

/**
 * 线缆放置工具的配置菜单（照抄上游 CableToolMenu）。
 * 提供线缆类型 / 颜色 / 放置模式选择。
 * [本地改动] 升级槽已删除——光谱钥匙功能按需求内置进工具（hasUpgrade 恒真）。
 */
public class CableToolMenu extends AEBaseMenu
{
    private static final String ACTION_SET_MODE = "setMode";
    private static final String ACTION_SET_CABLE_TYPE = "setCableType";
    private static final String ACTION_SET_COLOR = "setColor";
    private static final String ACTION_SET_COLOR_SHORTCUT = "setColorShortcut";

    // 同步字段
    @GuiSync(0)
    public int currentMode;
    @GuiSync(1)
    public int currentCableType;
    @GuiSync(2)
    public int currentColor;
    @GuiSync(3)
    public boolean hasUpgrade;

    // 颜色快捷槽（1-5，槽 0 恒为当前颜色）
    @GuiSync(4)
    public int colorShortcut1 = -1;
    @GuiSync(5)
    public int colorShortcut2 = -1;
    @GuiSync(6)
    public int colorShortcut3 = -1;
    @GuiSync(7)
    public int colorShortcut4 = -1;
    @GuiSync(8)
    public int colorShortcut5 = -1;

    private final ItemStack toolStack;
    private final Player player;

    public CableToolMenu(int id, Inventory playerInventory, FriendlyByteBuf buf)
    {
        this(id, playerInventory, getToolStackFromSlot(playerInventory, buf.readInt()), playerInventory.selected);
    }

    private static ItemStack getToolStackFromSlot(Inventory inv, int slot)
    {
        if (slot == 40)
        {
            return inv.player.getOffhandItem();
        }
        return inv.getItem(slot);
    }

    public CableToolMenu(int id, Inventory playerInventory, ItemStack toolStack)
    {
        this(id, playerInventory, toolStack, playerInventory.selected);
    }

    public CableToolMenu(int id, Inventory playerInventory, ItemStack toolStack, int slot)
    {
        super(MeptMenus.CABLE_TOOL_MENU.get(), id, playerInventory, null);

        this.player = playerInventory.player;
        this.toolStack = toolStack;

        // 玩家背包槽位（自定义坐标，与贴图对齐）
        createCustomPlayerInventorySlots(playerInventory);

        // 从工具装载当前设置
        loadSettings();

        // 注册客户端动作
        registerClientAction(ACTION_SET_MODE, Integer.class, this::setMode);
        registerClientAction(ACTION_SET_CABLE_TYPE, Integer.class, this::setCableType);
        registerClientAction(ACTION_SET_COLOR, Integer.class, this::setColor);
        registerClientAction(ACTION_SET_COLOR_SHORTCUT, int[].class, this::setColorShortcutFromClient);
    }

    private void loadSettings()
    {
        this.currentMode = ItemMECablePlacementTool.getMode(toolStack).ordinal();
        this.currentCableType = ItemMECablePlacementTool.getCableType(toolStack).ordinal();
        this.currentColor = ItemMECablePlacementTool.getColor(toolStack).ordinal();
        // 内置升级：恒为已安装
        this.hasUpgrade = true;

        // 装载颜色快捷槽
        int[] shortcuts = ItemMECablePlacementTool.getColorShortcuts(toolStack);
        if (shortcuts.length >= 5)
        {
            this.colorShortcut1 = shortcuts[0];
            this.colorShortcut2 = shortcuts[1];
            this.colorShortcut3 = shortcuts[2];
            this.colorShortcut4 = shortcuts[3];
            this.colorShortcut5 = shortcuts[4];
        }
    }

    /**
     * 按贴图位置摆玩家背包槽位。
     * 背包区 (8,111)-(167,162)，16x16 槽 + 2px 间距（共 18px）；
     * 快捷栏 (8,169)-(167,184)。
     */
    private void createCustomPlayerInventorySlots(Inventory playerInventory)
    {
        final int SLOT_SIZE = 18; // 16px 槽 + 2px 间距
        final int INV_X = 8;
        final int INV_Y = 111;
        final int HOTBAR_Y = 169;

        // 主背包（槽 9-35）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                int index = col + row * 9 + 9;
                int x = INV_X + col * SLOT_SIZE;
                int y = INV_Y + row * SLOT_SIZE;
                this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, index, x, y), SlotSemantics.PLAYER_INVENTORY);
            }
        }

        // 快捷栏（槽 0-8）
        for (int col = 0; col < 9; col++)
        {
            int x = INV_X + col * SLOT_SIZE;
            this.addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, x, HOTBAR_Y), SlotSemantics.PLAYER_HOTBAR);
        }
    }

    public void setMode(int mode)
    {
        if (mode >= 0 && mode < ItemMECablePlacementTool.PlacementMode.values().length)
        {
            this.currentMode = mode;
            ItemMECablePlacementTool.setMode(toolStack, ItemMECablePlacementTool.PlacementMode.values()[mode]);
            if (isClientSide())
            {
                sendClientAction(ACTION_SET_MODE, mode);
            }
        }
    }

    public void setCableType(int type)
    {
        if (type >= 0 && type < ItemMECablePlacementTool.CableType.values().length)
        {
            this.currentCableType = type;
            ItemMECablePlacementTool.setCableType(toolStack, ItemMECablePlacementTool.CableType.values()[type]);
            if (isClientSide())
            {
                sendClientAction(ACTION_SET_CABLE_TYPE, type);
            }
        }
    }

    public void setColor(int color)
    {
        if (color >= 0 && color < AEColor.values().length)
        {
            this.currentColor = color;
            ItemMECablePlacementTool.setColor(toolStack, AEColor.values()[color]);
            if (isClientSide())
            {
                sendClientAction(ACTION_SET_COLOR, color);
            }
        }
    }

    public ItemMECablePlacementTool.PlacementMode getPlacementMode()
    {
        return ItemMECablePlacementTool.PlacementMode.values()[currentMode];
    }

    public ItemMECablePlacementTool.CableType getCableType()
    {
        return ItemMECablePlacementTool.CableType.values()[currentCableType];
    }

    public AEColor getSelectedColor()
    {
        return AEColor.values()[currentColor];
    }

    public boolean hasUpgradeInstalled()
    {
        return this.hasUpgrade;
    }

    /**
     * 取颜色快捷槽数组。
     * @return 5 个颜色下标（-1 = 空）
     */
    public int[] getColorShortcuts()
    {
        return new int[]{colorShortcut1, colorShortcut2, colorShortcut3, colorShortcut4, colorShortcut5};
    }

    /**
     * 设置某个颜色快捷槽。
     * @param slot 槽下标（0-4）
     * @param colorIndex 颜色下标，-1 表示清空
     */
    public void setColorShortcut(int slot, int colorIndex)
    {
        switch (slot)
        {
            case 0 -> this.colorShortcut1 = colorIndex;
            case 1 -> this.colorShortcut2 = colorIndex;
            case 2 -> this.colorShortcut3 = colorIndex;
            case 3 -> this.colorShortcut4 = colorIndex;
            case 4 -> this.colorShortcut5 = colorIndex;
        }

        // 写回工具
        int[] shortcuts = getColorShortcuts();
        ItemMECablePlacementTool.setColorShortcuts(toolStack, shortcuts);

        if (isClientSide())
        {
            sendClientAction(ACTION_SET_COLOR_SHORTCUT, new int[]{slot, colorIndex});
        }
    }

    /** 处理客户端 setColorShortcut 动作。 */
    private void setColorShortcutFromClient(int[] data)
    {
        if (data != null && data.length >= 2)
        {
            int colorIndex = data[1];
            // 校验：只允许 -1（空）或合法 AEColor 序号，否则恶意同步值会让客户端渲染快捷栏时崩溃
            if (colorIndex != -1 && (colorIndex < 0 || colorIndex >= AEColor.values().length))
            {
                return;
            }
            setColorShortcut(data[0], colorIndex);
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        // 按物品类型匹配而非栈引用（引用会被数据组件同步换掉导致菜单意外关闭）
        return player.getMainHandItem().getItem() instanceof ItemMECablePlacementTool
                || player.getOffhandItem().getItem() instanceof ItemMECablePlacementTool;
    }
}
