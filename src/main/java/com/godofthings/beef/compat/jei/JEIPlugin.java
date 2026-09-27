package com.godofthings.beef.compat.jei;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.client.gui.ChainGroupScreen;
import com.godofthings.beef.client.gui.DimensionConfigScreen;
import com.godofthings.beef.client.gui.StaffLinkScreen;
import com.godofthings.beef.content.menus.DimensionConfigMenu;
import com.godofthings.beef.content.stafflink.LinkFilterSlot;
import com.godofthings.beef.content.stafflink.StaffLinkFilters;
import com.godofthings.beef.content.stafflink.StaffLinkRoute;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 接线：运行时持有器 + 三套「从 JEI 把原料拖进界面」的处理器。
 *
 * <p>对应上游 {@code com.sorrowmist.useless.compat.jei.JEIPlugin} 里与本模组已移植子系统相关的部分。
 * 上游该类的其余内容（万象合金炉配方类别与配方页、催化剂信息页、全能样板转移、
 * {@code GenericStackJeiIngredientProviders} 等）属于机器子系统，未随照抄带入。</p>
 *
 * <p><b>v5.1.1 补记</b>：v3.0.0 首次照抄时把本类裁剪成「只有运行时持有器」，
 * 判断依据是「上游该类里与造化杖有关的只有运行时持有器」——那个判断<b>漏掉了
 * {@code registerGuiHandlers}</b>：它注册的三个拖拽处理器分别服务于连锁等价组界面、
 * 无线物流界面与无用维度配置界面，都属于已移植的子系统。现按上游逐字补回三者。</p>
 *
 * <p>本类自带 {@link JeiPlugin} 注解，是独立于 {@code com.godofthings.jei.GodJeiPlugin}
 * 的另一个 JEI 插件：JEI 允许同一模组注册多个插件（按 pluginUid 区分）。</p>
 */
@JeiPlugin
public final class JEIPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(UselessMod.MODID, "beef_tool_runtime");

    @Nullable
    private static IJeiRuntime runtime;

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** 供连锁等价组界面查询 JEI 物品列表；JEI 缺席时为 {@code null}。 */
    @Nullable
    public static IJeiRuntime getRuntime() {
        return runtime;
    }

    /* ==================== JEI 拖拽（照抄上游 registerGuiHandlers） ==================== */

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(DimensionConfigScreen.class,
                new DimensionConfigGhostHandler());
        registration.addGhostIngredientHandler(StaffLinkScreen.class,
                new StaffLinkGhostHandler());
        registration.addGhostIngredientHandler(ChainGroupScreen.class,
                new ChainGroupGhostHandler());
        // 等价组界面继承了 AbstractContainerScreen，JEI 内置的容器屏 handler 会自动接管，
        // 不需要（也不该）再注册 IScreenHandler——两套 handler 会互相抢。
    }

    /**
     * 连锁等价组界面接受 JEI 拖拽。
     *
     * <p>每个组行右端有一个拖拽槽，把方块拖进去就作为一条精确方块 ID 追加到该组。
     * 非方块物品直接不接受——等价组判定的是方块，收一个没有方块的物品没有意义。</p>
     */
    private static final class ChainGroupGhostHandler
            implements IGhostIngredientHandler<ChainGroupScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(ChainGroupScreen screen,
                                                   ITypedIngredient<I> ingredient,
                                                   boolean doStart) {
            ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
            if (!(stack.getItem() instanceof BlockItem blockItem)) return List.of();
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());

            List<Target<I>> targets = new ArrayList<>(screen.chainGroupCount());
            for (int index = 0; index < screen.chainGroupCount(); index++) {
                if (!screen.isGroupRowVisible(index)) continue;
                final int groupIndex = index;
                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return new Rect2i(screen.groupDropZoneScreenX(groupIndex),
                                screen.groupDropZoneScreenY(groupIndex),
                                screen.dropZoneSize(), screen.dropZoneSize());
                    }

                    @Override
                    public void accept(I value) {
                        screen.addEntryFromBlock(groupIndex, blockId);
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }

    /**
     * 无线物流的过滤槽接受 JEI 拖拽。
     *
     * <p>标记按线路的资源类型分流：物品线路收物品，流体线路收<b>流体本身</b>（不是装它的桶），
     * 化学品线路收一只装满它的储罐。类型对不上的原料直接不接受——不接，比悄悄塞进一个
     * 语义不对的东西好。</p>
     */
    private static final class StaffLinkGhostHandler
            implements IGhostIngredientHandler<StaffLinkScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(StaffLinkScreen screen,
                                                   ITypedIngredient<I> ingredient,
                                                   boolean doStart) {
            StaffLinkRoute config = screen.getMenu().getSelectedConfig();
            if (config == null || !config.filterApplies()) return List.of();
            LinkFilterSlot marker = StaffLinkFilters.fromIngredient(
                    config.medium(), ingredient.getIngredient());
            if (marker == null) return List.of();

            List<Target<I>> targets = new ArrayList<>(StaffLinkScreen.filterSlotCount());
            for (int index = 0; index < StaffLinkScreen.filterSlotCount(); index++) {
                final int slotIndex = index;
                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return new Rect2i(screen.filterSlotScreenX(slotIndex),
                                screen.filterSlotScreenY(slotIndex),
                                StaffLinkScreen.filterSlotSize(), StaffLinkScreen.filterSlotSize());
                    }

                    @Override
                    public void accept(I value) {
                        screen.getMenu().setFilterSlot(slotIndex, marker);
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }

    /**
     * 无用维度配置界面的方块槽接受 JEI 拖拽（把方块从 JEI 拖到填充/边框/中心等空槽里）。
     */
    private static final class DimensionConfigGhostHandler
            implements IGhostIngredientHandler<DimensionConfigScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(DimensionConfigScreen screen,
                                                   ITypedIngredient<I> ingredient,
                                                   boolean doStart) {
            ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
            if (!(stack.getItem() instanceof BlockItem)) return List.of();

            List<Target<I>> targets = new ArrayList<>(DimensionConfigMenu.GHOST_SLOT_COUNT);
            for (int index = 0; index < DimensionConfigMenu.GHOST_SLOT_COUNT; index++) {
                if (!screen.getMenu().isGhostSlotActive(index)) continue;
                int slotIndex = index;
                DimensionConfigMenu.GhostSlot slot = screen.getMenu().getGhostSlot(slotIndex);
                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return new Rect2i(screen.getGuiLeft() + slot.x,
                                screen.getGuiTop() + slot.y, 16, 16);
                    }

                    @Override
                    public void accept(I value) {
                        if (value instanceof ItemStack itemStack) {
                            screen.getMenu().setGhostSlotFromClient(slotIndex, itemStack);
                        }
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }
}
