package com.godofthings.compass.natures.gui;

import com.godofthings.compass.natures.NaturesCompassData;
import com.godofthings.compass.natures.client.BiomeDisplay;
import com.godofthings.compass.natures.sorting.DimensionSorting;
import com.godofthings.compass.natures.sorting.NameSorting;
import com.godofthings.compass.natures.sorting.SourceSorting;
import com.godofthings.compass.natures.sorting.TagsSorting;
import com.godofthings.compass.natures.util.BiomeUtils;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 群系列表的一行（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>每行四行文字：群系名 / 当前排序维度的值 / 标签 / 来源。经验不足的群系变灰不可选，
 * 右上角还会画出「要扣几级经验」的附魔台等级贴图。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BiomeSearchEntry extends ObjectSelectionList.Entry<BiomeSearchEntry>
{
    private static final ResourceLocation[] ENABLED_LEVEL_SPRITES = new ResourceLocation[] {
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1"),
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2"),
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3")
    };

    private static final ResourceLocation[] DISABLED_LEVEL_SPRITES = new ResourceLocation[] {
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1_disabled"),
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2_disabled"),
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3_disabled")
    };

    private final Minecraft mc;
    private final NaturesCompassScreen parentScreen;
    private final Biome biome;
    private final ResourceLocation biomeId;
    private final Player player;
    private final BiomeSearchList biomesList;
    private final String tags;
    private final int xpLevels;
    private long lastClickTime;

    public BiomeSearchEntry(BiomeSearchList biomesList, Biome biome, Player player)
    {
        this.biomesList = biomesList;
        this.biome = biome;
        this.player = player;
        this.parentScreen = biomesList.getParentScreen();
        this.mc = Minecraft.getInstance();
        this.tags = BiomeDisplay.getBiomeTags(parentScreen.level, biome);
        this.biomeId = BiomeUtils.getKeyForBiome(parentScreen.level, biome).orElse(null);

        // 这个群系搜索一次要扣几级经验（上限 3）
        int levels = 0;
        if (biomeId != null && NaturesCompassData.xpLevelsForAllowedBiomes.containsKey(biomeId))
        {
            levels = NaturesCompassData.xpLevelsForAllowedBiomes.get(biomeId);
            if (levels > 3)
            {
                levels = 3;
            }
        }
        this.xpLevels = levels;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX,
            int mouseY, boolean isHovering, float partialTick)
    {
        String title = parentScreen.getSortingCategory().getLocalizedName();
        Object value = parentScreen.getSortingCategory().getValue(biome);
        if (parentScreen.getSortingCategory() instanceof NameSorting
                || parentScreen.getSortingCategory() instanceof SourceSorting
                || parentScreen.getSortingCategory() instanceof TagsSorting
                || parentScreen.getSortingCategory() instanceof DimensionSorting)
        {
            title = I18n.get("godofthings.natures.dimension");
            if (biomeId != null)
            {
                value = BiomeDisplay.dimensionKeysToString(
                        NaturesCompassData.dimensionKeysForAllowedBiomeKeys.get(biomeId));
            }
            else
            {
                value = "";
            }
        }

        int maxTextWidth = width - 10;

        if (xpLevels > 0)
        {
            int itemHeight = height + 4;
            int spriteSize = (int) (itemHeight * 0.4F);
            int spriteBorder = (itemHeight - spriteSize) / 2;
            int spriteIndex = xpLevels - 1;
            ResourceLocation spriteId = isEnabled() ? ENABLED_LEVEL_SPRITES[spriteIndex] : DISABLED_LEVEL_SPRITES[spriteIndex];
            guiGraphics.blitSprite(spriteId, left + width - spriteSize - spriteBorder, top + spriteBorder - 2,
                    spriteSize, spriteSize);

            // 画了经验贴图，右侧要留出位置
            maxTextWidth = width - itemHeight - 5;
        }

        String tagsLine = I18n.get("godofthings.natures.tags") + ": " + tags;
        if (mc.font.width(tagsLine) > maxTextWidth)
        {
            tagsLine = mc.font.plainSubstrByWidth(tagsLine + "...", maxTextWidth) + "...";
        }

        int nameColor = isEnabled() ? 0xffffffff : 0xff808080;
        int infoColor = isEnabled() ? 0xff808080 : 0xff555555;
        guiGraphics.drawString(mc.font, Component.literal(BiomeDisplay.getBiomeNameForDisplay(parentScreen.level, biome)),
                left + 5, top + (height / 2) - ((mc.font.lineHeight + 2) * 2), nameColor);
        guiGraphics.drawString(mc.font, Component.literal(title + ": " + value), left + 5,
                top + (height / 2) - ((mc.font.lineHeight + 2)), infoColor);
        guiGraphics.drawString(mc.font, Component.literal(tagsLine), left + 5,
                top + (height / 2) + ((mc.font.lineHeight + 2) * 0), infoColor);
        guiGraphics.drawString(mc.font,
                Component.translatable("godofthings.natures.source")
                        .append(Component.literal(": " + BiomeDisplay.getBiomeSource(parentScreen.level, biome))),
                left + 5, top + (height / 2) + ((mc.font.lineHeight + 2)), infoColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && isEnabled())
        {
            biomesList.setSelected(this);
            // 250ms 内点两下 = 直接开搜（上游的手感）
            if (Util.getMillis() - lastClickTime < 250L)
            {
                parentScreen.searchForBiome(biomeId);
            }
            lastClickTime = Util.getMillis();
            return true;
        }
        return false;
    }

    /** 经验够不够搜这个群系（创造模式 / 有无限材料时永远够） */
    public boolean isEnabled()
    {
        return NaturesCompassData.infiniteXp || player.experienceLevel >= xpLevels;
    }

    public ResourceLocation getBiomeId()
    {
        return biomeId;
    }

    @Override
    public Component getNarration()
    {
        return Component.literal(BiomeDisplay.getBiomeNameForDisplay(parentScreen.level, biome));
    }
}
