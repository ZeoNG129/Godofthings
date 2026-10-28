package com.godofthings.overloadedinterface;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import appeng.helpers.InterfaceLogicHost;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;

/**
 * 过载接口菜单（重写版：不再继承 AE2 原生 InterfaceMenu）。
 *
 * <p>自己把 36 个配置槽 / 36 个存储槽建到 8 个分页语义
 * （{@link OverloadedSlotSemantics}）上，每语义一行 9 槽，
 * 翻页 = 按语义组开关槽位激活——与上游 AE2LT 的分页方案一致，
 * 但不用反射重排父类的槽位表。</p>
 *
 * <p>模式开关 / 无限槽位 / 连接数仍走 {@code GuiSync} + clientAction。</p>
 */
public class OverloadedInterfaceMenu extends UpgradeableMenu<InterfaceLogicHost>
{
    public static MenuType<OverloadedInterfaceMenu> TYPE;

    /** 每页显示的配置槽数（两行 × 9）。 */
    public static final int SLOTS_PER_PAGE = 18;

    public static void init()
    {
        OverloadedSlotSemantics.init();
        TYPE = MenuTypeBuilder
                .create(OverloadedInterfaceMenu::new, InterfaceLogicHost.class)
                .buildUnregistered(com.godofthings.Godofthings.godofthingsId("interface"));
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
    private final List<Slot> allConfigSlots = new ArrayList<>();

    public OverloadedInterfaceMenu(int id, Inventory playerInventory, InterfaceLogicHost host)
    {
        super(TYPE, id, playerInventory, host);
        this.host = host;

        var logic = host.getInterfaceLogic();
        var configInv = logic.getConfig().createMenuWrapper();
        int slots = configInv.size();

        // 36 配置槽：每 18 个一页，页内每 9 个一行
        for (int i = 0; i < slots; i++)
        {
            int page = i / SLOTS_PER_PAGE;
            int row = (i % SLOTS_PER_PAGE) / 9;
            var semantic = OverloadedSlotSemantics.CONFIG_PATTERN[2 * page + row];
            this.allConfigSlots.add(this.addSlot(new FakeSlot(configInv, i), semantic));
        }
        // 36 存储槽：同布局
        for (int i = 0; i < slots; i++)
        {
            int page = i / SLOTS_PER_PAGE;
            int row = (i % SLOTS_PER_PAGE) / 9;
            var semantic = OverloadedSlotSemantics.STORAGE_PATTERN[2 * page + row];
            this.addSlot(new AppEngSlot(logic.getStorage().createMenuWrapper(), i), semantic);
        }
        for (var slot : this.allConfigSlots)
        {
            this.addSlot(slot);
        }

        this.totalPages = Math.max(1, slots / SLOTS_PER_PAGE);

        this.registerClientAction("nextPage", this::nextPage);
        this.registerClientAction("prevPage", this::prevPage);
        this.registerClientAction("cycleInterfaceMode", this::cycleInterfaceMode);
        this.registerClientAction("cycleExportMode", this::cycleExportMode);
        this.registerClientAction("cycleImportMode", this::cycleImportMode);
        this.registerClientAction("cycleIOSpeed", this::cycleIOSpeed);
        this.registerClientAction("toggleUnlimited", Integer.class, this::toggleUnlimited);

        this.registerClientAction("setAmount", Integer.class, this::openSetAmountMenu);

        this.syncFromHost();
        this.showPage(0);
    }

    private OverloadedInterfaceBlockEntity be()
    {
        return this.host instanceof OverloadedInterfaceBlockEntity blockEntity ? blockEntity : null;
    }


    /**
     * 只同步模糊模式（照抄 InterfaceMenu.loadSettingsFromHost）。
     * 父类 UpgradeableMenu 会额外拉 REDSTONE_CONTROLLED 等设置，
     * 而 InterfaceLogic 的 ConfigManager 没注册它们，直接抛
     * {@code UnsupportedSettingException}，界面打不开、物品也放不进槽。
     */
    @Override
    protected void loadSettingsFromHost(appeng.api.util.IConfigManager manager)
    {
        this.setFuzzyMode(manager.getSetting(appeng.api.config.Settings.FUZZY_MODE));
    }

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

    // ==================== 分页 ====================

    /** 显示指定页：按语义组开关槽位激活（0/1 页对应语义组 0-1 / 2-3）。 */
    public void showPage(int page)
    {
        if (page < 0 || page >= this.totalPages)
        {
            return;
        }
        this.currentPage = page;
        for (int group = 0; group < 4; group++)
        {
            boolean active = page == group / 2;
            for (var semantic : new appeng.menu.SlotSemantic[] {
                    OverloadedSlotSemantics.CONFIG_PATTERN[group],
                    OverloadedSlotSemantics.STORAGE_PATTERN[group] })
            {
                for (var slot : this.getSlots(semantic))
                {
                    if (slot instanceof AppEngSlot appEngSlot)
                    {
                        appEngSlot.setActive(active);
                    }
                }
            }
        }
    }

    private void nextPage()
    {
        this.showPage(this.currentPage + 1 >= this.totalPages ? 0 : this.currentPage + 1);
    }

    private void prevPage()
    {
        this.showPage(this.currentPage - 1 < 0 ? this.totalPages - 1 : this.currentPage - 1);
    }

    public void clientNextPage()
    {
        // 客户端先本地切页（槽位 active 状态立刻更新），再让服务端同步权威状态
        this.nextPage();
        this.sendClientAction("nextPage");
    }

    public void clientPrevPage()
    {
        this.prevPage();
        this.sendClientAction("prevPage");
    }

    // ==================== 模式切换（服务端）====================

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

    /**
     * 打开 AE2 的「设置数量」子界面（照抄 InterfaceMenu.openSetAmountMenu）：
     * 客户端发 {@code setAmount} action，服务端用槽内当前的 key/amount 打开 SetStockAmountMenu。
     */
    public void openSetAmountMenu(int configSlot)
    {
        if (this.isClientSide())
        {
            this.sendClientAction("setAmount", configSlot);
            return;
        }
        var stack = this.host.getConfig().getStack(configSlot);
        if (stack == null)
        {
            return;
        }
        if (this.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
        {
            appeng.menu.implementations.SetStockAmountMenu.open(serverPlayer, this.getLocator(),
                    configSlot, stack.what(), (int) stack.amount());
        }
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
        if (blockEntity == null || slot == null
                || slot < 0 || slot >= OverloadedInterfaceBlockEntity.SLOT_COUNT)
        {
            return;
        }
        boolean nowUnlimited = !blockEntity.isSlotUnlimited(slot);
        blockEntity.setSlotUnlimited(slot, nowUnlimited);
        // 照抄上游：切到无限时把配置槽数量归 1，导出循环据此视为不限量补货
        if (nowUnlimited)
        {
            var config = this.host.getInterfaceLogic().getConfig();
            var key = config.getKey(slot);
            if (key != null)
            {
                config.setStack(slot, new appeng.api.stacks.GenericStack(key, 1));
            }
        }
        this.syncFromHost();
    }

    // ==================== 查询（客户端）====================

    public List<Slot> getAllConfigSlots()
    {
        return this.allConfigSlots;
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

    // ==================== 存储槽 ME 终端式交互（照抄上游 clicked）====================

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType,
            net.minecraft.world.entity.player.Player player)
    {
        if (slotId >= 0 && slotId < this.slots.size()
                && this.slots.get(slotId) instanceof AppEngSlot slot
                && java.util.Arrays.asList(OverloadedSlotSemantics.STORAGE_PATTERN)
                        .contains(this.getSlotSemantic(slot)))
        {
            if (!this.isClientSide())
            {
                this.handleStorageInteraction(slot, button, clickType, player);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private OverloadedInterfaceLogic.ProxiedStorageInv getProxy()
    {
        return this.host.getInterfaceLogic() instanceof OverloadedInterfaceLogic logic
                ? logic.getProxiedStorage()
                : null;
    }

    private void handleStorageInteraction(net.minecraft.world.inventory.Slot slot, int button,
            net.minecraft.world.inventory.ClickType clickType,
            net.minecraft.world.entity.player.Player player)
    {
        var proxy = this.getProxy();
        if (proxy == null)
        {
            return;
        }
        int idx = slot.getContainerSlot();
        switch (clickType)
        {
            case PICKUP -> this.handlePickup(proxy, idx, button);
            case QUICK_MOVE -> this.handleQuickMove(proxy, idx, player);
            case THROW -> this.handleThrow(proxy, idx, button, player);
            case SWAP -> this.handleSwap(proxy, idx, button, player);
            case CLONE -> this.handleClone(proxy, idx, player);
            default ->
            {
            }
        }
    }

    private void handlePickup(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx, int button)
    {
        var carried = this.getCarried();
        if (carried.isEmpty())
        {
            var key = proxy.getKey(idx);
            if (!(key instanceof appeng.api.stacks.AEItemKey itemKey))
            {
                return;
            }
            long maxExtract = Math.min(proxy.capForSlot(idx), itemKey.getMaxStackSize());
            if (button == 1)
            {
                maxExtract = Math.max(1, (maxExtract + 1) / 2);
            }
            long extracted = proxy.proxyExtract(key, maxExtract, appeng.api.config.Actionable.MODULATE);
            if (extracted > 0)
            {
                this.setCarried(itemKey.toStack((int) Math.min(extracted, Integer.MAX_VALUE)));
            }
        }
        else
        {
            var itemKey = appeng.api.stacks.AEItemKey.of(carried);
            if (itemKey == null)
            {
                return;
            }
            int toInsert = (button == 1) ? 1 : carried.getCount();
            long inserted = proxy.proxyInsert(itemKey, toInsert, appeng.api.config.Actionable.MODULATE);
            if (inserted > 0)
            {
                carried.shrink((int) inserted);
                if (carried.isEmpty())
                {
                    this.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
        }
    }

    private void handleQuickMove(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx,
            net.minecraft.world.entity.player.Player player)
    {
        var key = proxy.getKey(idx);
        if (!(key instanceof appeng.api.stacks.AEItemKey itemKey))
        {
            return;
        }
        long maxExtract = Math.min(proxy.capForSlot(idx), itemKey.getMaxStackSize());
        long extracted = proxy.proxyExtract(key, maxExtract, appeng.api.config.Actionable.MODULATE);
        if (extracted > 0)
        {
            var stack = itemKey.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
            if (!player.getInventory().add(stack))
            {
                player.drop(stack, false);
            }
        }
    }

    private void handleThrow(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx, int button,
            net.minecraft.world.entity.player.Player player)
    {
        var key = proxy.getKey(idx);
        if (!(key instanceof appeng.api.stacks.AEItemKey itemKey))
        {
            return;
        }
        long maxExtract = (button == 1)
                ? Math.min(proxy.capForSlot(idx), itemKey.getMaxStackSize())
                : 1;
        long extracted = proxy.proxyExtract(key, maxExtract, appeng.api.config.Actionable.MODULATE);
        if (extracted > 0)
        {
            player.drop(itemKey.toStack((int) Math.min(extracted, Integer.MAX_VALUE)), true);
        }
    }

    private void handleSwap(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx, int button,
            net.minecraft.world.entity.player.Player player)
    {
        if (button < 0 || button > 8)
        {
            return;
        }
        var hotbarStack = player.getInventory().getItem(button);
        var key = proxy.getKey(idx);
        appeng.api.stacks.AEItemKey hbKey = hotbarStack.isEmpty() ? null
                : appeng.api.stacks.AEItemKey.of(hotbarStack);
        // 先模拟两侧再实际交换（照抄上游）
        long simInserted = hbKey != null
                ? proxy.proxyInsert(hbKey, hotbarStack.getCount(), appeng.api.config.Actionable.SIMULATE)
                : 0;
        long simExtracted = key instanceof appeng.api.stacks.AEItemKey itemKey
                ? proxy.proxyExtract(key,
                        Math.min(proxy.capForSlot(idx), itemKey.getMaxStackSize()),
                        appeng.api.config.Actionable.SIMULATE)
                : 0;
        if (simInserted <= 0 && simExtracted <= 0)
        {
            return;
        }
        var extractedStack = net.minecraft.world.item.ItemStack.EMPTY;
        if (simExtracted > 0 && key instanceof appeng.api.stacks.AEItemKey itemKey)
        {
            long extracted = proxy.proxyExtract(key, simExtracted,
                    appeng.api.config.Actionable.MODULATE);
            if (extracted > 0)
            {
                extractedStack = itemKey.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
            }
        }
        if (simInserted > 0 && hbKey != null)
        {
            long inserted = proxy.proxyInsert(hbKey, hotbarStack.getCount(),
                    appeng.api.config.Actionable.MODULATE);
            if (inserted > 0)
            {
                hotbarStack.shrink((int) inserted);
            }
        }
        if (!extractedStack.isEmpty())
        {
            if (hotbarStack.isEmpty())
            {
                player.getInventory().setItem(button, extractedStack);
            }
            else if (!player.getInventory().add(extractedStack))
            {
                player.drop(extractedStack, false);
            }
        }
    }

    private void handleClone(OverloadedInterfaceLogic.ProxiedStorageInv proxy, int idx,
            net.minecraft.world.entity.player.Player player)
    {
        if (!player.isCreative())
        {
            return;
        }
        var key = proxy.getKey(idx);
        if (key instanceof appeng.api.stacks.AEItemKey itemKey)
        {
            this.setCarried(itemKey.toStack(itemKey.getMaxStackSize()));
        }
    }
    public boolean isSlotUnlimited(int slot)
    {
        return slot >= 0 && slot < 64 && ((this.unlimitedBits >> slot) & 1L) != 0L;
    }
}
