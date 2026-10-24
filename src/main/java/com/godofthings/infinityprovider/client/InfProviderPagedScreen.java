package com.godofthings.infinityprovider.client;

import java.util.List;

import com.godofthings.infinityprovider.InfProviderMenu;

import appeng.menu.slot.AppEngSlot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import appeng.client.gui.style.ScreenStyle;

/**
 * 大容量供应器的翻页屏幕（移植自上游 InfinityPatternProviderScreen）：
 * 每页显示 32 个样板槽（8×4），上/下箭头与滚轮翻页，页码显示在标题下方。
 * 增强版：同时继承 {@link InfProviderEnhancedScreen}，左侧工具栏带无线/回收/自适应/高级设置。
 *
 * <p>槽位坐标的重排依赖 accesstransformer.cfg 把 {@code Slot.x/y} 的 final 去掉
 * （AE2 自己也做了同样的 AT，上游 mod 的翻页屏正是这么改坐标的）。</p>
 */
public abstract class InfProviderPagedScreen<M extends InfProviderMenu.EnhancedActions & InfProviderMenu.PagedMenu>
        extends InfProviderEnhancedScreen<M> {

    private static final int PAGE_TEXT_X = 116;
    private static final int PAGE_TEXT_Y = 28;
    private static final int SLOTS_PER_PAGE = 32;

    private final int totalSlots;
    private final int totalPages;
    private int page = 0;
    private List<AppEngSlot> patternSlots;

    public InfProviderPagedScreen(M menu, Inventory playerInventory,
            Component title, ScreenStyle style, int totalSlots) {
        super(menu, playerInventory, title, style);
        this.patternSlots = menu.getPatternSlots();
        this.totalSlots = totalSlots;
        this.totalPages = Math.max(1, (totalSlots + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
        // 只激活第一页的槽位，其余全部隐藏
        for (int i = SLOTS_PER_PAGE; i < this.patternSlots.size(); i++) {
            this.patternSlots.get(i).setActive(false);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new InfArrowButton(this.leftPos + 154, this.topPos + 41,
                InfArrowButton.Direction.UP, button -> this.prevPage()));
        this.addRenderableWidget(new InfArrowButton(this.leftPos + 154, this.topPos + 91,
                InfArrowButton.Direction.DOWN, button -> this.nextPage()));
        this.updateAllSlotPositions();
    }

    private void updateAllSlotPositions() {
        if (this.patternSlots != null && !this.patternSlots.isEmpty()) {
            for (int i = 0; i < this.patternSlots.size(); i++) {
                this.setSlotPosition(this.patternSlots.get(i), i);
            }
        }
    }

    private void setSlotPosition(AppEngSlot slot, int slotIndex) {
        slot.x = 8 + slotIndex % SLOTS_PER_PAGE % 8 * 18;
        slot.y = 42 + slotIndex % SLOTS_PER_PAGE / 8 * 18;
        slot.setChanged();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= this.leftPos && mouseX < this.leftPos + this.width
                && mouseY >= this.topPos && mouseY < this.topPos + this.height) {
            if (scrollY > 0.0) {
                this.prevPage();
            } else if (scrollY < 0.0) {
                this.nextPage();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void prevPage() {
        this.setPageVisible(this.page, false);
        this.page = this.page > 0 ? this.page - 1 : this.totalPages - 1;
        this.setPageVisible(this.page, true);
    }

    public void nextPage() {
        this.setPageVisible(this.page, false);
        this.page = this.page < this.totalPages - 1 ? this.page + 1 : 0;
        this.setPageVisible(this.page, true);
    }

    private void setPageVisible(int page, boolean visible) {
        for (int i = SLOTS_PER_PAGE * page; i < SLOTS_PER_PAGE * page + SLOTS_PER_PAGE; i++) {
            if (i < this.patternSlots.size()) {
                this.patternSlots.get(i).setActive(visible);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.drawString(this.font,
                String.format("%04d", this.page + 1) + "/" + String.format("%04d", this.totalPages),
                this.leftPos + PAGE_TEXT_X, this.topPos + PAGE_TEXT_Y, 4210752, false);
    }
}
