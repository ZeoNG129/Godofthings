package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.backpack.GodBackpackMenu;
import com.godofthings.backpack.GodBackpackSettings;
import com.godofthings.backpack.SortBy;
import com.godofthings.network.GodBackpackActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 神之背包界面（120 格，9 列 × 6 行可见 + 滚动）。
 *
 * <p>功能（对标精妙背包下界合金档的界面，但代码与贴图都是本项目原创）：</p>
 * <ul>
 *   <li><b>整理</b>：按当前排序方式重排（按名称 / 按模组 / 按数量 / 按标签）</li>
 *   <li><b>记忆格</b>：标记模式点格子 → 记住该格物品；记忆格不参与整理、存放时优先填、空着显示半透明影子</li>
 *   <li><b>忽略整理格</b>：标记模式点格子 → 整理时该格原地不动</li>
 *   <li><b>搜索</b>：名字过滤（`@` 前缀按模组），不匹配的格子显示为空</li>
 *   <li><b>存入背包 / 取到身上</b>、<b>忽略耐久 / 忽略 NBT</b>、<b>保留搜索词</b>、滚轮翻页</li>
 * </ul>
 *
 * <p><b>坐标纪律</b>：所有坐标都是「面板内相对坐标」，绘制时统一加 {@code leftPos/topPos}。</p>
 */
public class GodBackpackScreen extends AbstractContainerScreen<GodBackpackMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_backpack.png");

    // ---- 布局（与 textures/gui/god_backpack.png 一致）----
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 6;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 37;
    private static final int SB_X = 176;
    private static final int SB_Y = 37;
    private static final int SB_W = 12;
    private static final int SB_H = 108;

    private static final int SEARCH_X = 8;
    private static final int SEARCH_Y = 5;
    private static final int SEARCH_W = 184;
    private static final int SEARCH_H = 14;

    private static final int BTN_Y = 22;
    private static final int BTN_H = 14;
    private static final int[] BTN_X = {8, 40, 82, 122, 164};
    private static final int[] BTN_W = {30, 40, 38, 40, 26};

    // 设置浮层里的 5 个开关
    private static final int SET_W = 150;
    private static final int SET_ROW_H = 20;

    // ---- 状态 ----
    /** 标记模式：0 = 无，1 = 记忆，2 = 忽略整理 */
    private int markMode;
    private boolean settingsOpen;
    private EditBox searchBox;
    /** 上次发给服务端的搜索词（避免每次重绘都发包） */
    private String sentSearch = "";

    public GodBackpackScreen(GodBackpackMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 240;
        this.inventoryLabelY = 149;
    }

    @Override
    protected void init()
    {
        super.init();
        this.markMode = 0;
        this.settingsOpen = false;

        this.searchBox = new EditBox(this.font, this.leftPos + SEARCH_X, this.topPos + SEARCH_Y,
                SEARCH_W, SEARCH_H, Component.translatable("gui.godofthings.backpack.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Component.translatable("gui.godofthings.backpack.search"));
        this.searchBox.setValue(this.menu.searchPhrase() == null ? "" : this.menu.searchPhrase());
        this.sentSearch = this.searchBox.getValue();
        this.searchBox.setResponder(text ->
        {
            // 搜索词即时同步给服务端（服务端权威，供过滤与「匹配转移」使用）
            if (!text.equals(this.sentSearch))
            {
                this.sentSearch = text;
                PacketDistributor.sendToServer(new GodBackpackActionPayload(
                        GodBackpackActionPayload.ACTION_SET_SEARCH, 0, text));
            }
        });
        addRenderableWidget(this.searchBox);
    }

    // ------------------------------------------------------------------ 绘制

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        gui.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        drawButtons(gui, mouseX, mouseY);
        drawScrollbar(gui);
        drawGhostsAndMarks(gui);
        if (this.settingsOpen)
        {
            drawSettings(gui, mouseX, mouseY);
        }
    }

    /**
     * 搜索遮罩：当前搜索词不匹配的格子**画成空**（物品仍在背包里，只是不显示）。
     * 物品本身不做任何搬动，所以不会丢东西；配合下面的 slotClicked 让它也点不动。
     */
    @Override
    protected void renderSlot(GuiGraphics gui, Slot slot)
    {
        if (!this.menu.matchesSearch(slot.getItem()))
        {
            return;
        }
        super.renderSlot(gui, slot);
    }

    /** 被搜索遮住的格子不接受点击（避免「看不见却点得到」） */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, net.minecraft.world.inventory.ClickType type)
    {
        if (slot != null && !this.menu.matchesSearch(slot.getItem()))
        {
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    /** 顶部 5 个按钮：整理 / 排序方式 / 存入背包 / 取到身上 / 设置 */
    private void drawButtons(GuiGraphics gui, int mouseX, int mouseY)
    {
        Component[] labels = {
                Component.translatable("gui.godofthings.backpack.sort"),
                sortByLabel(),
                Component.translatable("gui.godofthings.backpack.to_backpack"),
                Component.translatable("gui.godofthings.backpack.to_inventory"),
                Component.literal("⚙"),
        };
        for (int i = 0; i < labels.length; i++)
        {
            boolean hovered = isHovering(BTN_X[i], BTN_Y, BTN_W[i], BTN_H, mouseX, mouseY);
            boolean active = i != 4 || this.settingsOpen;
            int bg = active ? (hovered ? 0xFF6E6E6E : 0xFF4A4A4A) : (hovered ? 0xFF5A5A5A : 0xFF3A3A3A);
            int bx = this.leftPos + BTN_X[i];
            int by = this.topPos + BTN_Y;
            gui.fill(bx, by, bx + BTN_W[i], by + BTN_H, bg);
            gui.fill(bx, by, bx + BTN_W[i], by + 1, 0xFF8A8A8A);
            gui.fill(bx, by + BTN_H - 1, bx + BTN_W[i], by + BTN_H, 0xFF2A2A2A);
            String label = this.font.plainSubstrByWidth(labels[i].getString(), BTN_W[i] - 4);
            gui.drawString(this.font, label, bx + (BTN_W[i] - this.font.width(label)) / 2, by + 3, 0xFFFFFF, false);
        }
    }

    private Component sortByLabel()
    {
        SortBy by = this.menu.sortBy();
        return Component.translatable("gui.godofthings.backpack.sort." + (by == null ? "name" : by.getSerializedName()));
    }

    /** 最大滚动行数（120 格 = 14 行，可见 6 行 → 最多滚 8） */
    private int maxScroll()
    {
        int totalRows = (com.godofthings.backpack.GodBackpackItem.SIZE + COLS - 1) / COLS;
        return Math.max(0, totalRows - VISIBLE_ROWS);
    }

    /** 滚动条：轨道 + 滑块（鼠标在轨道上时高亮） */
    private void drawScrollbar(GuiGraphics gui)
    {
        int maxScroll = maxScroll();
        if (maxScroll <= 0)
        {
            return;
        }
        int trackX = this.leftPos + SB_X;
        int trackY = this.topPos + SB_Y;
        int handleH = Math.max(14, SB_H * VISIBLE_ROWS / (VISIBLE_ROWS + maxScroll));
        int handleY = trackY + (SB_H - handleH) * this.menu.scroll() / maxScroll;
        gui.fill(trackX + 2, handleY, trackX + SB_W - 2, handleY + handleH, 0xFFC6C6C6);
        gui.fill(trackX + 2, handleY, trackX + SB_W - 2, handleY + 1, 0xFFFFFFFF);
        gui.fill(trackX + 2, handleY + handleH - 1, trackX + SB_W - 2, handleY + handleH, 0xFF555555);
    }

    /** 记忆格的空影子 + 记忆 / 忽略整理的角标 */
    private void drawGhostsAndMarks(GuiGraphics gui)
    {
        GodBackpackSettings settings = this.menu.settings();
        List<Slot> slots = this.menu.slots;
        for (int i = 0; i < COLS * VISIBLE_ROWS && i < slots.size(); i++)
        {
            Slot slot = slots.get(i);
            int storageIndex = this.menu.visibleSlotToStorageIndex(i);
            int x = this.leftPos + GRID_X + (i % COLS) * 18;
            int y = this.topPos + GRID_Y + (i / COLS) * 18;

            // 记忆格：空着时画一个半透明的影子
            if (settings != null && settings.isMemory(storageIndex) && !slot.hasItem())
            {
                ItemStack remembered = settings.memory().get(storageIndex);
                if (remembered != null && !remembered.isEmpty())
                {
                    gui.setColor(1.0F, 1.0F, 1.0F, 0.35F);
                    gui.renderItem(remembered, x + 1, y + 1);
                    gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
            // 角标：记忆 = 右下紫点，忽略整理 = 右上灰条
            if (settings != null && settings.isMemory(storageIndex))
            {
                gui.fill(x + 12, y + 12, x + 16, y + 16, 0xFF8A5AC8);
            }
            if (settings != null && settings.isNoSort(storageIndex))
            {
                gui.fill(x + 11, y + 1, x + 16, y + 3, 0xFF6E6E6E);
            }
        }
    }

    /** 设置浮层：记忆标记 / 忽略整理标记 / 忽略耐久 / 忽略 NBT / 保留搜索词 */
    private void drawSettings(GuiGraphics gui, int mouseX, int mouseY)
    {
        int x = this.leftPos + (this.imageWidth - SET_W) / 2;
        int y = this.topPos + 52;
        int h = 5 * SET_ROW_H + 8;
        gui.fill(x - 2, y - 2, x + SET_W + 2, y + h + 2, 0xFF202020);
        gui.fill(x, y, x + SET_W, y + h, 0xFF3A3A3A);

        GodBackpackSettings s = this.menu.settings();
        for (int i = 0; i < 5; i++)
        {
            int by = y + 4 + i * SET_ROW_H;
            boolean hovered = mouseX >= x + 2 && mouseX < x + SET_W - 2 && mouseY >= by && mouseY < by + SET_ROW_H - 2;
            boolean on = switch (i)
            {
                case 0 -> this.markMode == 1;
                case 1 -> this.markMode == 2;
                case 2 -> this.menu.ignoreDurability();
                case 3 -> this.menu.ignoreNbt();
                default -> this.menu.keepSearch();
            };
            gui.fill(x + 2, by, x + SET_W - 2, by + SET_ROW_H - 2,
                    on ? (hovered ? 0xFF6F9E6F : 0xFF4E7A4E) : (hovered ? 0xFF5A5A5A : 0xFF474747));
            gui.drawString(this.font, settingsLabel(i), x + 6, by + 3, 0xFFFFFF, false);
            gui.drawString(this.font, Component.translatable(on ? "gui.godofthings.armor.on" : "gui.godofthings.armor.off"),
                    x + SET_W - 22, by + 3, on ? 0x9CFF9C : 0xC0C0C0, false);
        }

    }

    private Component settingsLabel(int i)
    {
        return switch (i)
        {
            case 0 -> Component.translatable("gui.godofthings.backpack.memory");
            case 1 -> Component.translatable("gui.godofthings.backpack.no_sort");
            case 2 -> Component.translatable("gui.godofthings.backpack.ignore_durability");
            case 3 -> Component.translatable("gui.godofthings.backpack.ignore_nbt");
            default -> Component.translatable("gui.godofthings.backpack.keep_search");
        };
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        gui.drawString(this.font, this.title, 8, 6 - 1, 0x404040, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"),
                8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);

        // 标记模式提示
        if (this.markMode != 0)
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.backpack.marking"),
                    8, this.topPos + 40 - 12, 0xFFD060, false);
        }
        // 按钮与滚动条提示
        if (isHovering(BTN_X[0], BTN_Y, BTN_W[0], BTN_H, mouseX, mouseY))
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.backpack.sort"), mouseX, mouseY);
        }
        else if (isHovering(BTN_X[1], BTN_Y, BTN_W[1], BTN_H, mouseX, mouseY))
        {
            gui.renderTooltip(this.font, sortByLabel(), mouseX, mouseY);
        }
        else if (isHovering(BTN_X[2], BTN_Y, BTN_W[2], BTN_H, mouseX, mouseY))
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.backpack.to_backpack"), mouseX, mouseY);
        }
        else if (isHovering(BTN_X[3], BTN_Y, BTN_W[3], BTN_H, mouseX, mouseY))
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.backpack.to_inventory"), mouseX, mouseY);
        }
        else if (isHovering(SEARCH_X, SEARCH_Y, SEARCH_W, SEARCH_H, mouseX, mouseY))
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.backpack.search.hint"), mouseX, mouseY);
        }
        else if (!this.settingsOpen && isHovering(SB_X, SB_Y, SB_W, SB_H, mouseX, mouseY))
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.backpack.scroll"), mouseX, mouseY);
        }
        else if (this.hoveredSlot != null && this.hoveredSlot.hasItem())
        {
            gui.renderTooltip(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        // ① 设置浮层打开时，点击只作用于浮层
        if (this.settingsOpen)
        {
            int x = (this.imageWidth - SET_W) / 2;
            int y = 52;
            for (int i = 0; i < 5; i++)
            {
                int by = y + 4 + i * SET_ROW_H;
                if (relX >= x + 2 && relX < x + SET_W - 2 && relY >= by && relY < by + SET_ROW_H - 2)
                {
                    onSettingsRow(i);
                    return true;
                }
            }
            if (relX < x - 2 || relX > x + SET_W + 2 || relY < y - 2 || relY > y + 5 * SET_ROW_H + 10)
            {
                this.settingsOpen = false;
                return true;
            }
            return true;
        }

        // ② 顶部按钮
        for (int i = 0; i < BTN_X.length; i++)
        {
            if (isHovering(BTN_X[i], BTN_Y, BTN_W[i], BTN_H, mouseX, mouseY))
            {
                if (i == 4)
                {
                    this.settingsOpen = true;
                    return true;
                }
                int btnId = switch (i)
                {
                    case 0 -> GodBackpackMenu.BTN_SORT;
                    case 1 -> GodBackpackMenu.BTN_SORT_BY;
                    case 2 -> GodBackpackMenu.BTN_TO_BACKPACK;
                    default -> GodBackpackMenu.BTN_TO_INVENTORY;
                };
                sendButton(btnId);
                return true;
            }
        }

        // ③ 滚动条：点轨道跳转
        if (isHovering(SB_X, SB_Y, SB_W, SB_H, mouseX, mouseY))
        {
            int maxScroll = maxScroll();
            if (maxScroll > 0)
            {
                int rel = (int) mouseY - (this.topPos + SB_Y);
                setScroll((int) Math.round((double) rel / SB_H * maxScroll));
            }
            return true;
        }

        // ④ 标记模式下点背包格子 = 切换标记（不移动物品）
        int cell = gridCellAt(mouseX, mouseY);
        if (this.markMode != 0 && cell >= 0)
        {
            int storageIndex = this.menu.visibleSlotToStorageIndex(cell);
            int action = this.markMode == 1
                    ? GodBackpackActionPayload.ACTION_TOGGLE_MEMORY
                    : GodBackpackActionPayload.ACTION_TOGGLE_NO_SORT;
            PacketDistributor.sendToServer(new GodBackpackActionPayload(action, storageIndex, ""));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onSettingsRow(int row)
    {
        switch (row)
        {
            case 0 -> this.markMode = this.markMode == 1 ? 0 : 1;
            case 1 -> this.markMode = this.markMode == 2 ? 0 : 2;
            case 2 -> sendButton(GodBackpackMenu.BTN_TOGGLE_IGNORE_DURABILITY);
            case 3 -> sendButton(GodBackpackMenu.BTN_TOGGLE_IGNORE_NBT);
            default -> sendButton(GodBackpackMenu.BTN_TOGGLE_KEEP_SEARCH);
        }
    }

    /** 鼠标所在的背包格（0..53），不在网格上返回 -1 */
    private int gridCellAt(double mouseX, double mouseY)
    {
        int relX = (int) mouseX - this.leftPos - GRID_X;
        int relY = (int) mouseY - this.topPos - GRID_Y;
        if (relX < 0 || relY < 0)
        {
            return -1;
        }
        int col = relX / 18;
        int row = relY / 18;
        if (col >= COLS || row >= VISIBLE_ROWS || relX % 18 > 16 || relY % 18 > 16)
        {
            return -1;
        }
        return row * COLS + col;
    }

    private void sendButton(int id)
    {
        if (this.minecraft != null && this.minecraft.getConnection() != null)
        {
            this.minecraft.getConnection().send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
        }
    }

    private void setScroll(int scroll)
    {
        PacketDistributor.sendToServer(new GodBackpackActionPayload(
                GodBackpackActionPayload.ACTION_SET_SCROLL, scroll, ""));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        int maxScroll = maxScroll();
        if (maxScroll > 0 && !this.settingsOpen)
        {
            int next = this.menu.scroll() - (int) Math.signum(scrollY);
            setScroll(Math.max(0, Math.min(maxScroll, next)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        // 标记模式下 Esc 先退出标记模式
        if (keyCode == 256 && this.markMode != 0)
        {
            this.markMode = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
