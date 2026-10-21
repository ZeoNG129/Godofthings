package com.godofthings.compass.natures.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.godofthings.compass.common.PinyinMatch;
import com.godofthings.compass.natures.NaturesCompassData;
import com.godofthings.compass.natures.client.BiomeDisplay;
import com.godofthings.compass.natures.items.NaturesCompassItem;
import com.godofthings.compass.natures.network.SearchForNextPacket;
import com.godofthings.compass.natures.network.SearchPacket;
import com.godofthings.compass.natures.network.TeleportPacket;
import com.godofthings.compass.natures.sorting.ISorting;
import com.godofthings.compass.natures.sorting.NameSorting;
import com.godofthings.compass.natures.util.BiomeUtils;
import com.godofthings.compass.natures.util.CompassState;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 自然罗盘的群系选择界面（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>左边一列按钮（搜索 / 搜索下一个 / 排序依据 / 取消），上面一个搜索框（{@code $} 前缀搜标签、
 * {@code @} 前缀搜来源），右边是可滚动群系列表，右上角是传送按钮（有权限才显示）。</p>
 *
 * <p>服务端推下 {@code SyncPacket} 之前列表是空的，收到后（{@code NaturesCompassData.synced} 变 true）
 * 在 {@link #tick()} 里重建一次列表。</p>
 */
@OnlyIn(Dist.CLIENT)
public class NaturesCompassScreen extends Screen
{
    public final Level level;
    private final Player player;
    private final ItemStack stack;
    private final NaturesCompassItem natureCompass;
    private List<Biome> allowedBiomes;
    private List<Biome> biomesMatchingSearch;
    private final ResourceLocation foundBiomeKey;
    private Button searchForBiomeButton;
    private Button searchForNextButton;
    private Button teleportButton;
    private Button cancelButton;
    private Button sortByButton;
    private TransparentTextField searchTextField;
    private BiomeSearchList selectionList;
    private ISorting<?> sortingCategory;

    public NaturesCompassScreen(Level level, Player player, ItemStack stack, NaturesCompassItem natureCompass,
            List<ResourceLocation> allowedBiomes)
    {
        super(Component.translatable("godofthings.natures_compass.selectBiome"));
        this.level = level;
        this.player = player;
        this.stack = stack;
        this.natureCompass = natureCompass;
        this.allowedBiomes = new ArrayList<>();
        loadAllowedBiomes(allowedBiomes);

        this.sortingCategory = new NameSorting();
        this.biomesMatchingSearch = new ArrayList<>(this.allowedBiomes);

        ResourceLocation found = null;
        if (natureCompass.getState(stack) == CompassState.FOUND
                && stack.has(NaturesCompassData.BIOME_ID.get()))
        {
            found = ResourceLocation.parse(stack.get(NaturesCompassData.BIOME_ID.get()));
        }
        this.foundBiomeKey = found;
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
        ResourceLocation selectedId = selectionList != null && selectionList.hasSelection()
                ? selectionList.getSelected().getBiomeId()
                : null;
        searchForNextButton.active = teleportButton.active = selectedId != null && selectedId.equals(foundBiomeKey);
        searchForBiomeButton.active = selectionList.hasSelection();

        // 收到同步包了 → 用服务端给的群系列表重建一次
        if (NaturesCompassData.synced)
        {
            teleportButton.visible = NaturesCompassData.canTeleport;
            searchForNextButton.visible = NaturesCompassData.maxNextSearches > 0;
            if (searchForNextButton.visible)
            {
                sortByButton.setPosition(10, 100);
            }
            else
            {
                sortByButton.setPosition(10, 65);
            }
            removeWidget(selectionList);
            loadAllowedBiomes(NaturesCompassData.allowedBiomes);
            biomesMatchingSearch = new ArrayList<>(allowedBiomes);
            selectionList = new BiomeSearchList(this, minecraft, player, foundBiomeKey, 130, 40, width - 140,
                    height - 50, 50);
            addRenderableWidget(selectionList);
            NaturesCompassData.synced = false;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.drawCenteredString(font, I18n.get("godofthings.natures_compass.selectBiome"), 65, 15, 0xffffff);
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

    /** 点列表行（双击）/ 点「搜索」按钮：发包开搜并关界面 */
    public void searchForBiome(ResourceLocation biomeId)
    {
        if (biomeId != null)
        {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            PacketDistributor.sendToServer(new SearchPacket(biomeId, player.blockPosition()));
        }
        minecraft.setScreen(null);
    }

    public void searchForNextBiome()
    {
        PacketDistributor.sendToServer(new SearchForNextPacket());
        minecraft.setScreen(null);
    }

    public void teleport()
    {
        PacketDistributor.sendToServer(new TeleportPacket());
        minecraft.setScreen(null);
    }

    public ISorting<?> getSortingCategory()
    {
        return sortingCategory;
    }

    /**
     * 按搜索框内容过滤群系。
     *
     * <ul>
     *   <li>{@code $xxx}：按标签搜</li>
     *   <li>{@code @xxx}：按来源模组搜</li>
     *   <li>其它：按显示名搜</li>
     * </ul>
     */
    public void processSearchTerm()
    {
        biomesMatchingSearch = new ArrayList<>();
        // 不再预先 toLowerCase：拼音匹配需要原样的大小写与中文，大小写不敏感由 PinyinMatch 负责
        String searchTerm = searchTextField.getValue();
        for (Biome biome : allowedBiomes)
        {
            if (searchTerm.startsWith("$"))
            {
                // $标签：按类别（森林 / 海洋 …）搜，同样吃拼音
                if (PinyinMatch.contains(BiomeDisplay.getBiomeTags(level, biome), searchTerm.substring(1)))
                {
                    biomesMatchingSearch.add(biome);
                }
            }
            else if (searchTerm.startsWith("@"))
            {
                // @模组：按来源模组搜
                if (PinyinMatch.contains(BiomeDisplay.getBiomeSource(level, biome), searchTerm.substring(1)))
                {
                    biomesMatchingSearch.add(biome);
                }
            }
            else if (PinyinMatch.contains(BiomeDisplay.getBiomeNameForDisplay(level, biome), searchTerm))
            {
                biomesMatchingSearch.add(biome);
            }
        }
        selectionList.refreshList(true);
    }

    /** 当前过滤结果按「先名字、再当前排序维度」排好序 */
    public List<Biome> sortBiomes()
    {
        List<Biome> biomes = biomesMatchingSearch;
        Collections.sort(biomes, new NameSorting());
        Collections.sort(biomes, sortingCategory);
        return biomes;
    }

    private void setupWidgets()
    {
        clearWidgets();

        searchForBiomeButton = addRenderableWidget(new TransparentButton(10, 40, 110, 20,
                Component.translatable("godofthings.natures_compass.search"), onPress ->
                {
                    if (selectionList.hasSelection())
                    {
                        searchForBiome(selectionList.getSelected().getBiomeId());
                    }
                }));
        searchForBiomeButton.active = false;

        searchForNextButton = addRenderableWidget(new TransparentButton(10, 65, 110, 20,
                Component.translatable("godofthings.natures_compass.searchForNext"), onPress -> searchForNextBiome()));
        searchForNextButton.visible = NaturesCompassData.maxNextSearches > 0;
        searchForNextButton.active = false;

        sortByButton = addRenderableWidget(new TransparentButton(10, 100, 110, 20,
                Component.literal(I18n.get("godofthings.natures_compass.sortBy") + ": "
                        + sortingCategory.getLocalizedName()), onPress ->
                {
                    sortingCategory = sortingCategory.next();
                    sortByButton.setMessage(Component.literal(I18n.get("godofthings.natures_compass.sortBy") + ": "
                            + sortingCategory.getLocalizedName()));
                    selectionList.refreshList(true);
                }));
        if (!searchForNextButton.visible)
        {
            sortByButton.setPosition(10, 65);
        }

        teleportButton = addRenderableWidget(new TransparentButton(width - 120, 10, 110, 20,
                Component.translatable("godofthings.natures_compass.teleport"), onPress -> teleport()));
        teleportButton.visible = NaturesCompassData.canTeleport;
        teleportButton.active = false;

        cancelButton = addRenderableWidget(new TransparentButton(10, height - 30, 110, 20,
                CommonComponents.GUI_CANCEL, onPress -> minecraft.setScreen(null)));

        searchTextField = addRenderableWidget(new TransparentTextField(font, 130, 10, 140, 20,
                Component.translatable("godofthings.natures_compass.search")));

        selectionList = new BiomeSearchList(this, minecraft, player, foundBiomeKey, 130, 40, width - 140,
                height - 50, 50);
        addRenderableWidget(selectionList);
    }

    private void loadAllowedBiomes(List<ResourceLocation> allowedBiomeKeys)
    {
        this.allowedBiomes = new ArrayList<>();
        for (ResourceLocation biomeKey : allowedBiomeKeys)
        {
            Optional<Biome> optionalBiome = BiomeUtils.getBiomeForKey(level, biomeKey);
            optionalBiome.ifPresent(this.allowedBiomes::add);
        }
    }
}
