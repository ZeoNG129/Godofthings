package com.godofthings.overloadedinterface;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.GenericStack;
import appeng.helpers.InterfaceLogicHost;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.InterfaceMenu;
import appeng.menu.implementations.MenuTypeBuilder;

/**
 * 过载接口菜单（移植自 AE2LT 的 OverloadedInterfaceMenu，裁掉反射改槽位语义那套）。
 *
 * <p>AE2 的 {@code InterfaceMenu} 固定按 9 槽布局注册 CONFIG / STORAGE 槽；
 * 上游用反射把槽位语义重映射成 36 槽 + 每页 18 槽。这里改用更朴素的办法：
 * 直接继承原生菜单（9 槽可见），额外的 27 个槽通过翻页切换
 * —— 与 {@code InfProviderPagedScreen} 的做法一致。</p>
 *
 * <p>模式切换（无线 / 导出 / 导入 / 速度）通过 {@code GuiSync} + clientAction
 * 完整同步，这与上游一致；无限槽位用位图同步。</p>
 */
public class OverloadedInterfaceMenu extends InterfaceMenu
{
    public static MenuType<OverloadedInterfaceMenu> TYPE;

    /** 每页显示的配置槽数（原生 9 槽一页）。 */
    public static final int SLOTS_PER_PAGE = 9;

    public static void init()
    {
        TYPE = MenuTypeBuilder
                .create(OverloadedInterfaceMenu::new, InterfaceLogicHost.class)
                .buildUnregistered(com.godofthings.Godofthings.godofthingsId("overloaded_interface"));
    }

    @GuiSync(20)
    public int currentPage;

    @GuiSync(21)
    public int totalPages;

    @GuiSync(22)
    public int interfaceMode;

    @GuiSync(23)
    public int exportMode;

    @GuiSync(24)
    public int importMode;

    @GuiSync(26)
    public int ioSpeedMode;

    @GuiSync(27)
    public long unlimitedBits;

    @GuiSync(28)
    public int connectionCount;

    private final InterfaceLogicHost host;

    public OverloadedInterfaceMenu(int id, Inventory playerInventory, InterfaceLogicHost host)
    {
        super(TYPE, id, playerInventory, host);
        this.host = host;
        this.totalPages = Math.max(1, OverloadedInterfaceBlockEntity.SLOT_COUNT / SLOTS_PER_PAGE);

        this.registerClientAction("nextPage", this::nextPage);
        this.registerClientAction("prevPage", this::prevPage);
        this.registerClientAction("cycleInterfaceMode", this::cycleInterfaceMode);
        this.registerClientAction("cycleExportMode", this::cycleExportMode);
        this.registerClientAction("cycleImportMode", this::cycleImportMode);
        this.registerClientAction("cycleIOSpeed", this::cycleIOSpeed);
        this.registerClientAction("toggleUnlimited", Integer.class, this::toggleUnlimited);

        this.syncFromHost();
    }

    private OverloadedInterfaceBlockEntity be()
    {
        return this.host instanceof OverloadedInterfaceBlockEntity blockEntity ? blockEntity : null;
    }

    // ==================== 同步 ====================

    @Override
    public void broadcastChanges()
    {
        this.syncFromHost();
        super.broadcastChanges();
    }

    private void syncFromHost()
    {
        var blockEntity = this.be();
        if (blockEntity == null)
        {
            return;
        }
        this.interfaceMode = blockEntity.getInterfaceMode().ordinal();
        this.exportMode = blockEntity.getExportMode().ordinal();
        this.importMode = blockEntity.getImportMode().ordinal();
        this.ioSpeedMode = blockEntity.getIOSpeedMode().ordinal();
        this.connectionCount = blockEntity.getConnections().size();
        long bits = 0L;
        for (int i = 0; i < OverloadedInterfaceBlockEntity.SLOT_COUNT && i < 64; i++)
        {
            if (blockEntity.isSlotUnlimited(i))
            {
                bits |= (1L << i);
            }
        }
        this.unlimitedBits = bits;
    }

    // ==================== 客户端动作入口 ====================

    public void clientNextPage()
    {
        this.sendClientAction("nextPage");
    }

    public void clientPrevPage()
    {
        this.sendClientAction("prevPage");
    }

    public void clientCycleInterfaceMode()
    {
        this.sendClientAction("cycleInterfaceMode");
    }

    public void clientCycleExportMode()
    {
        this.sendClientAction("cycleExportMode");
    }

    public void clientCycleImportMode()
    {
        this.sendClientAction("cycleImportMode");
    }

    public void clientCycleIOSpeed()
    {
        this.sendClientAction("cycleIOSpeed");
    }

    public void clientToggleUnlimited(int slot)
    {
        this.sendClientAction("toggleUnlimited", slot);
    }

    // ==================== 服务端处理 ====================

    private void nextPage()
    {
        if (this.isServerSide())
        {
            this.currentPage = this.currentPage + 1 >= this.totalPages ? 0 : this.currentPage + 1;
        }
    }

    private void prevPage()
    {
        if (this.isServerSide())
        {
            this.currentPage = this.currentPage - 1 < 0 ? this.totalPages - 1 : this.currentPage - 1;
        }
    }

    private void cycleInterfaceMode()
    {
        if (this.isClientSide())
        {
            this.sendClientAction("cycleInterfaceMode");
            return;
        }
        var blockEntity = this.be();
        if (blockEntity == null)
        {
            return;
        }
        var values = OverloadedInterfaceBlockEntity.InterfaceMode.values();
        blockEntity.setInterfaceMode(values[(blockEntity.getInterfaceMode().ordinal() + 1)
                % values.length]);
        this.syncFromHost();
    }

    private void cycleExportMode()
    {
        if (this.isClientSide())
        {
            this.sendClientAction("cycleExportMode");
            return;
        }
        var blockEntity = this.be();
        if (blockEntity == null)
        {
            return;
        }
        var values = OverloadedInterfaceBlockEntity.ExportMode.values();
        blockEntity.setExportMode(values[(blockEntity.getExportMode().ordinal() + 1)
                % values.length]);
        this.syncFromHost();
    }

    private void cycleImportMode()
    {
        if (this.isClientSide())
        {
            this.sendClientAction("cycleImportMode");
            return;
        }
        var blockEntity = this.be();
        if (blockEntity == null)
        {
            return;
        }
        var values = OverloadedInterfaceBlockEntity.ImportMode.values();
        blockEntity.setImportMode(values[(blockEntity.getImportMode().ordinal() + 1)
                % values.length]);
        this.syncFromHost();
    }

    private void cycleIOSpeed()
    {
        if (this.isClientSide())
        {
            this.sendClientAction("cycleIOSpeed");
            return;
        }
        var blockEntity = this.be();
        if (blockEntity == null)
        {
            return;
        }
        var values = OverloadedInterfaceBlockEntity.IOSpeedMode.values();
        blockEntity.setIOSpeedMode(values[(blockEntity.getIOSpeedMode().ordinal() + 1)
                % values.length]);
        this.syncFromHost();
    }

    private void toggleUnlimited(Integer slot)
    {
        if (this.isClientSide())
        {
            this.sendClientAction("toggleUnlimited", slot);
            return;
        }
        var blockEntity = this.be();
        if (blockEntity == null || slot == null)
        {
            return;
        }
        blockEntity.setSlotUnlimited(slot, !blockEntity.isSlotUnlimited(slot));
        this.syncFromHost();
    }

    // ==================== 查询 ====================

    public int getCurrentPage()
    {
        return this.currentPage;
    }

    public int getTotalPages()
    {
        return this.totalPages;
    }

    public boolean isWirelessMode()
    {
        return this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS.ordinal();
    }

    public int getExportModeOrdinal()
    {
        return this.exportMode;
    }

    public int getImportModeOrdinal()
    {
        return this.importMode;
    }

    public int getIOSpeedOrdinal()
    {
        return this.ioSpeedMode;
    }

    public int getConnectionCount()
    {
        return this.connectionCount;
    }

    public boolean isSlotUnlimited(int slot)
    {
        return slot >= 0 && slot < 64 && ((this.unlimitedBits >> slot) & 1L) != 0L;
    }

    /** 由菜单构造器调用（上游同款签名）。 */
    public static OverloadedInterfaceMenu create(int id, Inventory playerInventory,
            InterfaceLogicHost host)
    {
        return new OverloadedInterfaceMenu(id, playerInventory, host);
    }

    static OverloadedInterfaceMenu fromBuffer(int id, Inventory playerInventory,
            RegistryFriendlyByteBuf buffer)
    {
        return new OverloadedInterfaceMenu(id, playerInventory,
                (InterfaceLogicHost) playerInventory.player.level()
                        .getBlockEntity(buffer.readBlockPos()));
    }
}
