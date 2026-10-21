package com.godofthings.compass.explorers.client;

import com.godofthings.compass.explorers.config.ConfigHandler;
import com.godofthings.compass.explorers.items.ExplorersCompassItem;
import com.godofthings.compass.explorers.util.CompassState;
import com.godofthings.compass.explorers.util.ItemUtils;
import com.godofthings.compass.explorers.util.StructureUtils;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 手持探险者指南针时显示的搜索状态 HUD（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，
 * CC BY-NC-SA 4.0）。
 *
 * <p>只画在「没开界面 / 开着聊天栏且配置允许」且「F3 调试屏没开」的时候。三种状态各一套内容：
 * 搜索中（半径）/ 已找到（坐标 + 距离）/ 未找到（半径），另外右侧还可以显示最近两次搜索的历史。</p>
 *
 * <p>上游用的是 1.20 Forge 的 {@code RenderGuiOverlayEvent.Post}；1.21.1 换成了
 * {@link LayeredDraw.Layer}（在 {@link ExplorersCompassClient} 里注册成独立 GUI 层）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ExplorersCompassOverlay implements LayeredDraw.Layer
{
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int BASE_X = 5;
    private static final int HISTORY_X = 64;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int CONTENT_COLOR = 0xAAAAAA;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker)
    {
        if (!shouldRenderHUD())
        {
            return;
        }
        final Player player = MC.player;
        final ItemStack stack = ItemUtils.getHeldExplorersCompass(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof ExplorersCompassItem compass))
        {
            return;
        }
        final CompassState state = compass.getState(stack);
        if (state == null || state == CompassState.INACTIVE)
        {
            return;
        }
        renderMainState(guiGraphics, compass, stack, player, state);
        if (ConfigHandler.CLIENT.showHaveFound.get())
        {
            renderSearchHistory(guiGraphics, compass, stack);
        }
    }

    private boolean shouldRenderHUD()
    {
        return MC.player != null
                && MC.level != null
                && !MC.options.hideGui
                && !MC.getDebugOverlay().showDebugScreen()
                && (MC.screen == null
                        || (ConfigHandler.CLIENT.displayWithChatOpen.get() && MC.screen instanceof ChatScreen));
    }

    private void renderMainState(GuiGraphics guiGraphics, ExplorersCompassItem compass, ItemStack stack,
            Player player, CompassState state)
    {
        // 公共状态信息
        renderLinePair(guiGraphics, "godofthings.explorers_compass.status", I18n.get(getStatusKey(state)), 0);
        renderLinePair(guiGraphics, "godofthings.explorers_compass.structure",
                StructureDisplay.getPrettyStructureName(compass.getStructureKey(stack)), 3);

        // 状态特定信息
        switch (state)
        {
            case SEARCHING, NOT_FOUND ->
                    renderLinePair(guiGraphics, "godofthings.explorers_compass.radius",
                            String.valueOf(compass.getSearchRadius(stack)), 6);
            case FOUND ->
            {
                if (compass.shouldDisplayCoordinates(stack))
                {
                    String coords = compass.getFoundStructureX(stack) + ", " + compass.getFoundStructureZ(stack);
                    renderLinePair(guiGraphics, "godofthings.explorers_compass.coordinates", coords, 6);
                    int distance = StructureUtils.getHorizontalDistanceToLocation(player,
                            compass.getFoundStructureX(stack), compass.getFoundStructureZ(stack));
                    renderLinePair(guiGraphics, "godofthings.explorers_compass.distance",
                            String.valueOf(distance), 9);
                }
            }
            default ->
            {
            }
        }
    }

    /** 右上角「最近两次搜索」两行一块（最多两条） */
    private void renderSearchHistory(GuiGraphics guiGraphics, ExplorersCompassItem compass, ItemStack stack)
    {
        int searchedTimes = compass.getSearchedTimes(stack);
        if (searchedTimes < 1)
        {
            return;
        }

        renderLine(guiGraphics, I18n.get("godofthings.explorers_compass.have_searched"), HISTORY_X, 0, TITLE_COLOR);
        renderSearchRecord(guiGraphics, compass, stack, 1, 1);

        if (searchedTimes >= 2)
        {
            renderSearchRecord(guiGraphics, compass, stack, 2, 4);
        }
    }

    private void renderSearchRecord(GuiGraphics guiGraphics, ExplorersCompassItem compass, ItemStack stack,
            int recordId, int lineOffset)
    {
        ResourceLocation structureKey = (recordId == 1) ? compass.getStructureKey1(stack)
                : compass.getStructureKey2(stack);
        boolean found = (recordId == 1) ? compass.getSearchResult1(stack) : compass.getSearchResult2(stack);
        int xCoord = (recordId == 1) ? compass.getFoundStructureX1(stack) : compass.getFoundStructureX2(stack);
        int zCoord = (recordId == 1) ? compass.getFoundStructureZ1(stack) : compass.getFoundStructureZ2(stack);

        renderLine(guiGraphics, StructureDisplay.getPrettyStructureName(structureKey), HISTORY_X, lineOffset,
                CONTENT_COLOR);
        String result = found ? (xCoord + ", " + zCoord) : I18n.get("godofthings.explorers_compass.notFound");
        renderLine(guiGraphics, result, HISTORY_X, lineOffset + 1, CONTENT_COLOR);
    }

    private void renderLinePair(GuiGraphics guiGraphics, String titleKey, String content, int startLine)
    {
        renderLine(guiGraphics, I18n.get(titleKey), BASE_X, startLine, TITLE_COLOR);
        renderLine(guiGraphics, content, BASE_X, startLine + 1, CONTENT_COLOR);
    }

    private void renderLine(GuiGraphics guiGraphics, String text, int x, int line, int color)
    {
        RenderUtils.drawConfiguredStringOnHUD(guiGraphics, text, x, BASE_X, color, line);
    }

    private String getStatusKey(CompassState state)
    {
        return switch (state)
        {
            case SEARCHING -> "godofthings.explorers_compass.searching";
            case FOUND -> "godofthings.explorers_compass.found";
            case NOT_FOUND -> "godofthings.explorers_compass.notFound";
            default -> "godofthings.explorers_compass.inactive";
        };
    }
}
