package com.godofthings.compass.natures.gui;

import java.util.Objects;

import com.godofthings.compass.natures.client.RenderUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 群系选择列表（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>自己画行底色与滚动条（原版 {@code ObjectSelectionList} 的样式跟这个半透明界面不搭），
 * 行的绘制委托给 {@link BiomeSearchEntry}。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BiomeSearchList extends ObjectSelectionList<BiomeSearchEntry>
{
    private final NaturesCompassScreen parentScreen;
    private final Player player;

    public BiomeSearchList(NaturesCompassScreen parentScreen, Minecraft mc, Player player, ResourceLocation biomeIdToSelect,
            int x, int y, int width, int height, int itemHeight)
    {
        super(mc, width, height, y, itemHeight);
        this.parentScreen = parentScreen;
        this.player = player;
        setX(x);
        refreshList(biomeIdToSelect);
    }

    @Override
    protected int getScrollbarPosition()
    {
        return getX() + getWidth();
    }

    @Override
    public int getRowWidth()
    {
        return getWidth();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        enableScissor(guiGraphics);
        // 先铺行底色
        for (int i = 0; i < getItemCount(); ++i)
        {
            if (getRowBottom(i) >= getY() && getRowTop(i) <= getBottom())
            {
                BiomeSearchEntry entry = getEntry(i);
                int fillColor = RenderUtils.getBackgroundColor(entry.isEnabled(), entry == getSelected());
                guiGraphics.fill(getRowLeft(), getRowTop(i), getRight(), getRowBottom(i), fillColor);
            }
        }
        // 再画行内容
        for (int i = 0; i < getItemCount(); ++i)
        {
            int top = getRowTop(i);
            int bottom = getRowBottom(i);
            if (bottom >= getY() && top <= getBottom())
            {
                BiomeSearchEntry entry = getEntry(i);
                boolean isHovering = isMouseOver(mouseX, mouseY)
                        && Objects.equals(getEntryAtPosition(mouseX, mouseY), entry);
                entry.render(guiGraphics, i, top, getRowLeft(), getRowWidth(), itemHeight, mouseX, mouseY, isHovering,
                        partialTicks);
            }
        }
        guiGraphics.disableScissor();
        // 滚动条
        if (getMaxScroll() > 0)
        {
            int left = getScrollbarPosition();
            int right = left + 6;
            int height = (getBottom() - getY()) * (getBottom() - getY()) / getMaxPosition();
            height = Mth.clamp(height, 32, getBottom() - getY() - 8);
            int top = (int) getScrollAmount() * (getBottom() - getY() - height) / getMaxScroll() + getY();
            if (top < getY())
            {
                top = getY();
            }
            guiGraphics.fill(left, getY(), right, getBottom(), RenderUtils.getBackgroundColor(false, false));
            guiGraphics.fill(left, top, right, top + height, RenderUtils.getBackgroundColor(true, true));
        }
    }

    @Override
    public void setSelected(BiomeSearchEntry entry)
    {
        // 经验不够的群系不许被选中
        if (entry == null || entry.isEnabled())
        {
            super.setSelected(entry);
        }
    }

    public void refreshList(ResourceLocation biomeIdToSelect)
    {
        clearEntries();
        for (Biome biome : parentScreen.sortBiomes())
        {
            BiomeSearchEntry entry = new BiomeSearchEntry(this, biome, player);
            addEntry(entry);
            ResourceLocation entryId = entry.getBiomeId();
            if (biomeIdToSelect != null && biomeIdToSelect.equals(entryId) && entry.isEnabled())
            {
                super.setSelected(entry);
            }
        }
        setScrollAmount(0);
    }

    public void refreshList(boolean maintainSelection)
    {
        ResourceLocation select = maintainSelection && hasSelection() ? getSelected().getBiomeId() : null;
        refreshList(select);
    }

    public boolean hasSelection()
    {
        return getSelected() != null;
    }

    public NaturesCompassScreen getParentScreen()
    {
        return parentScreen;
    }
}
