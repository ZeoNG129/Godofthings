package com.godofthings.compass.explorers.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.godofthings.compass.common.PinyinMatch;
import com.godofthings.compass.explorers.ExplorersCompassData;
import com.godofthings.compass.explorers.items.ExplorersCompassItem;
import com.godofthings.compass.explorers.network.CleanFoundPacket;
import com.godofthings.compass.explorers.network.CompassSearchPacket;
import com.godofthings.compass.explorers.network.TeleportPacket;
import com.godofthings.compass.explorers.sorting.ISorting;
import com.godofthings.compass.explorers.sorting.NameSorting;
import com.godofthings.compass.explorers.util.CompassState;
import com.godofthings.compass.explorers.client.StructureDisplay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 探险者指南针的结构选择界面（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>左边一列按钮（搜索 / 搜索组 / 排序方式 / 清除地图 / 取消），中间一个搜索框
 * （{@code @} 前缀搜来源），右边是可滚动结构列表，右上角是传送按钮（有权限才显示）。</p>
 *
 * <p>服务端推下 {@code SyncPacket} 之前列表是空的，收到后（{@code ExplorersCompassData} 里的
 * 静态列表长度变了）在 {@link #tick()} 里重建一次列表。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ExplorersCompassScreen extends Screen
{
    private final Level level;
    private final Player player;
    private final ItemStack stack;
    private final ExplorersCompassItem explorersCompass;
    private List<ResourceLocation> allowedStructureKeys;
    private List<ResourceLocation> structureKeysMatchingSearch;
    private Button searchButton;
    private Button searchGroupButton;
    private Button sortByButton;
    private Button teleportButton;
    private Button cancelButton;
    private Button cleanMapButton;
    private TransparentTextField searchTextField;
    private StructureSearchList selectionList;
    private ISorting sortingCategory;

    public ExplorersCompassScreen(Level level, Player player, ItemStack stack, ExplorersCompassItem explorersCompass,
            List<ResourceLocation> allowedStructureKeys)
    {
        super(Component.translatable("godofthings.explorers.selectStructure"));
        this.level = level;
        this.player = player;
        this.stack = stack;
        this.explorersCompass = explorersCompass;
        this.allowedStructureKeys = new ArrayList<>(allowedStructureKeys);
        this.structureKeysMatchingSearch = new ArrayList<>(this.allowedStructureKeys);
        this.sortingCategory = new NameSorting();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        return selectionList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void init()
    {
        setupWidgets();
    }

    @Override
    public void tick()
    {
        teleportButton.active = explorersCompass.getState(stack) == CompassState.FOUND;

        // 服务端推下来的结构表长度和本地不一致 → 用服务端那份重建列表
        if (allowedStructureKeys.size() != ExplorersCompassData.allowedStructureKeys.size())
        {
            removeWidget(selectionList);
            allowedStructureKeys = new ArrayList<>(ExplorersCompassData.allowedStructureKeys);
            structureKeysMatchingSearch = new ArrayList<>(allowedStructureKeys);
            selectionList = new StructureSearchList(this, minecraft, 0, 40, width + 110, height - 40, 45);
            addRenderableWidget(selectionList);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.drawCenteredString(font, title, 65, 15, 0xffffff);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        boolean ret = super.keyPressed(keyCode, scanCode, modifiers);
        if (searchTextField.isFocused())
        {
            processSearchTerm();
            return true;
        }
        return ret;
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode)
    {
        boolean ret = super.charTyped(typedChar, keyCode);
        if (searchTextField.isFocused())
        {
            processSearchTerm();
            return true;
        }
        return ret;
    }

    /** 选中 / 取消选中一行：只有选中了才让「搜索」「搜索组」可点 */
    public void selectStructure(StructureSearchEntry entry)
    {
        boolean enable = entry != null;
        searchButton.active = enable;
        searchGroupButton.active = enable;
    }

    public void searchForStructure(ResourceLocation key)
    {
        if (key != null)
        {
            PacketDistributor.sendToServer(new CompassSearchPacket(key, List.of(key), player.blockPosition()));
        }
        minecraft.setScreen(null);
    }

    public void searchForGroup(ResourceLocation key)
    {
        if (key != null)
        {
            PacketDistributor.sendToServer(new CompassSearchPacket(key,
                    ExplorersCompassData.typeKeysToStructureKeys.get(key), player.blockPosition()));
        }
        minecraft.setScreen(null);
    }

    public void teleport()
    {
        PacketDistributor.sendToServer(new TeleportPacket());
        minecraft.setScreen(null);
    }

    /** 按搜索框内容过滤结构（{@code @xxx} 按来源模组搜，其它按显示名搜；两者都吃拼音） */
    public void processSearchTerm()
    {
        structureKeysMatchingSearch = new ArrayList<>();
        // 不预先 toLowerCase：拼音匹配需要原样中文，大小写不敏感由 PinyinMatch 负责
        String searchTerm = searchTextField.getValue();
        for (ResourceLocation key : allowedStructureKeys)
        {
            if (searchTerm.startsWith("@"))
            {
                if (PinyinMatch.contains(StructureDisplay.getPrettyStructureSource(key), searchTerm.substring(1)))
                {
                    structureKeysMatchingSearch.add(key);
                }
            }
            else if (PinyinMatch.contains(StructureDisplay.getPrettyStructureName(key), searchTerm))
            {
                structureKeysMatchingSearch.add(key);
            }
        }
        selectionList.refreshList();
    }

    /** 当前过滤结果按「先名字、再当前排序维度」排好序 */
    public List<ResourceLocation> sortStructures()
    {
        final List<ResourceLocation> structures = structureKeysMatchingSearch;
        Collections.sort(structures, new NameSorting());
        Collections.sort(structures, sortingCategory);
        return structures;
    }

    private void setupWidgets()
    {
        clearWidgets();
        searchButton = addRenderableWidget(new TransparentButton(10, 40, 110, 20,
                Component.translatable("godofthings.explorers.search"), onPress ->
                {
                    if (selectionList.hasSelection())
                    {
                        selectionList.getSelected().searchForStructure();
                    }
                }));
        cleanMapButton = addRenderableWidget(new TransparentButton(10, 115, 110, 20,
                Component.translatable("godofthings.explorers.cleanMap"), onPress ->
                {
                    PacketDistributor.sendToServer(new CleanFoundPacket());
                    minecraft.setScreen(null);
                }));
        searchGroupButton = addRenderableWidget(new TransparentButton(10, 65, 110, 20,
                Component.translatable("godofthings.explorers.searchForGroup"), onPress ->
                {
                    if (selectionList.hasSelection())
                    {
                        selectionList.getSelected().searchForGroup();
                    }
                }));
        sortByButton = addRenderableWidget(new TransparentButton(10, 90, 110, 20,
                sortByLabel(), onPress ->
                {
                    sortingCategory = sortingCategory.next();
                    sortByButton.setMessage(sortByLabel());
                    selectionList.refreshList();
                }));
        cancelButton = addRenderableWidget(new TransparentButton(10, height - 30, 110, 20,
                CommonComponents.GUI_CANCEL, onPress -> minecraft.setScreen(null)));
        teleportButton = addRenderableWidget(new TransparentButton(width - 120, 10, 110, 20,
                Component.translatable("godofthings.explorers.teleport"), onPress -> teleport()));

        searchButton.active = false;
        searchGroupButton.active = false;

        teleportButton.visible = ExplorersCompassData.canTeleport;

        searchTextField = addRenderableWidget(new TransparentTextField(font, width / 2 - 82, 10, 140, 20,
                Component.translatable("godofthings.explorers.search")));

        if (selectionList == null)
        {
            selectionList = new StructureSearchList(this, minecraft, 0, 40, width + 110, height - 40, 45);
        }
        addRenderableWidget(selectionList);
    }

    /** 「排序方式: 名称」按钮文字 */
    private Component sortByLabel()
    {
        return Component.translatable("godofthings.explorers.sortBy")
                .append(Component.literal(": " + sortingCategory.getLocalizedName()));
    }
}
