package com.godofthings.compass.explorers.gui;

import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 结构选择列表（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>自己画行底色与滚动条（原版 {@code ObjectSelectionList} 的样式跟这个半透明界面不搭），
 * 行的绘制委托给 {@link StructureSearchEntry}。</p>
 *
 * <p>1.21 的 {@code AbstractSelectionList} 把 {@code x0/x1/y0/y1} 私有字段换成了
 * {@code getX()/getRight()/getY()/getBottom()} 与 {@code width/height} 字段，所以原来的
 * {@code renderList(...)} 覆写改成覆写 {@code renderWidget(...)}（父类 {@code renderWidget} 里
 * 那套背景 / 分隔线会被整段替换掉，正好符合「自己画」的意图）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class StructureSearchList extends ObjectSelectionList<StructureSearchEntry>
{
    private final ExplorersCompassScreen parentScreen;

    public StructureSearchList(ExplorersCompassScreen parentScreen, Minecraft mc, int x, int y, int width, int height,
            int itemHeight)
    {
        super(mc, width, height, y, itemHeight);
        this.parentScreen = parentScreen;
        setX(x);
        refreshList();
    }

    @Override
    protected int getScrollbarPosition()
    {
        return super.getScrollbarPosition() + 20;
    }

    @Override
    public int getRowWidth()
    {
        return super.getRowWidth() + 50;
    }

    @Override
    protected boolean isSelectedItem(int slotIndex)
    {
        return slotIndex >= 0 && slotIndex < children().size() ? children().get(slotIndex).equals(getSelected()) : false;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        for (int i = 0; i < getItemCount(); ++i)
        {
            int rowTop = getRowTop(i);
            int rowBottom = getRowBottom(i);
            if (rowBottom >= getY() && rowTop <= getBottom())
            {
                int innerHeight = itemHeight - 4;
                StructureSearchEntry entry = getEntry(i);
                if (isSelectedItem(i))
                {
                    final int insideLeft = getX() + width / 2 - getRowWidth() / 2 + 2;
                    guiGraphics.fill(insideLeft - 4, rowTop - 4, insideLeft + getRowWidth(),
                            rowTop + itemHeight - 4, 0xFF000000);
                    drawBorder(guiGraphics, insideLeft - 4, rowTop - 4, getRowWidth() + 4, itemHeight + 4);
                }
                boolean isHovering = isMouseOver(mouseX, mouseY)
                        && Objects.equals(getEntryAtPosition(mouseX, mouseY), entry);
                entry.render(guiGraphics, i, rowTop, getRowLeft(), getRowWidth(), innerHeight, mouseX, mouseY,
                        isHovering, partialTicks);
            }
        }

        if (getMaxScroll() > 0)
        {
            int left = getScrollbarPosition();
            int right = left + 6;
            int barHeight = (getBottom() - getY()) * (getBottom() - getY()) / getMaxPosition();
            barHeight = Mth.clamp(barHeight, 32, getBottom() - getY() - 8);
            int top = (int) getScrollAmount() * (getBottom() - getY() - barHeight) / getMaxScroll() + getY();
            if (top < getY())
            {
                top = getY();
            }

            guiGraphics.fill(left, getY(), right, getBottom(), (int) (2.35F * 255.0F) / 2 << 24);
            guiGraphics.fill(left, top, right, top + barHeight, (int) (1.9F * 255.0F) / 2 << 24);
        }
    }

    public void refreshList()
    {
        clearEntries();
        for (ResourceLocation key : parentScreen.sortStructures())
        {
            addEntry(new StructureSearchEntry(this, key));
        }
        selectStructure(null);
        setScrollAmount(0);
    }

    public void selectStructure(StructureSearchEntry entry)
    {
        setSelected(entry);
        parentScreen.selectStructure(entry);
    }

    public boolean hasSelection()
    {
        return getSelected() != null;
    }

    public ExplorersCompassScreen getParentScreen()
    {
        return parentScreen;
    }

    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height)
    {
        // 上边框
        guiGraphics.fill(x, y, x + width, y + 1, 0xDDDDDDDD);
        // 下边框
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xDDDDDDDD);
        // 左边框
        guiGraphics.fill(x, y, x + 1, y + height, 0xDDDDDDDD);
        // 右边框
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xDDDDDDDD);
    }
}
