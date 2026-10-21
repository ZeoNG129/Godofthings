package com.godofthings.compass.explorers.gui;

import com.godofthings.compass.explorers.ExplorersCompassData;
import com.godofthings.compass.explorers.client.StructureDisplay;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 结构列表的一行（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>每行四行文字：结构名 / 来源 / 组 / 维度。250ms 内点两下 = 直接开搜（上游的手感）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class StructureSearchEntry extends ObjectSelectionList.Entry<StructureSearchEntry>
{
    private final Minecraft mc;
    private final ExplorersCompassScreen parentScreen;
    private final ResourceLocation structureKey;
    private final StructureSearchList structuresList;
    private long lastClickTime;

    public StructureSearchEntry(StructureSearchList structuresList, ResourceLocation structureKey)
    {
        this.structuresList = structuresList;
        this.structureKey = structureKey;
        this.parentScreen = structuresList.getParentScreen();
        this.mc = Minecraft.getInstance();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX,
            int mouseY, boolean isHovering, float partialTick)
    {
        guiGraphics.drawString(mc.font, Component.literal(StructureDisplay.getPrettyStructureName(structureKey)),
                left + 1, top + 1, 0xffffff);
        guiGraphics.drawString(mc.font,
                Component.translatable("godofthings.explorers_compass.source")
                        .append(Component.literal(": " + StructureDisplay.getPrettyStructureSource(structureKey))),
                left + 1, top + mc.font.lineHeight + 3, 0x808080);
        ResourceLocation groupKey = ExplorersCompassData.structureKeysToTypeKeys.get(structureKey);
        guiGraphics.drawString(mc.font,
                Component.translatable("godofthings.explorers_compass.group")
                        .append(Component.literal(": "))
                        .append(Component.translatable(StructureDisplay.getPrettyStructureName(groupKey))),
                left + 1, top + mc.font.lineHeight + 14, 0x808080);
        guiGraphics.drawString(mc.font,
                Component.translatable("godofthings.explorers_compass.dimension")
                        .append(Component.literal(": " + StructureDisplay.dimensionKeysToString(
                                ExplorersCompassData.dimensionKeysForAllowedStructureKeys.get(structureKey)))),
                left + 1, top + mc.font.lineHeight + 25, 0x808080);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            structuresList.selectStructure(this);
            if (Util.getMillis() - lastClickTime < 250L)
            {
                searchForStructure();
                return true;
            }
            lastClickTime = Util.getMillis();
            return true;
        }
        return false;
    }

    @Override
    public Component getNarration()
    {
        return Component.literal(StructureDisplay.getPrettyStructureName(structureKey));
    }

    public void searchForStructure()
    {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        parentScreen.searchForStructure(structureKey);
    }

    public void searchForGroup()
    {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        parentScreen.searchForGroup(ExplorersCompassData.structureKeysToTypeKeys.get(structureKey));
    }

    public ResourceLocation getStructureKey()
    {
        return structureKey;
    }
}
