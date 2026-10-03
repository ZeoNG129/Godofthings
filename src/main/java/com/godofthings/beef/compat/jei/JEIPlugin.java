package com.godofthings.beef.compat.jei;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.client.gui.DimensionConfigScreen;
import com.godofthings.beef.content.menus.DimensionConfigMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 接线：运行时持有器 + 「从 JEI 把方块拖进无用维度配置界面」的处理器。
 *
 * <p>对应上游 {@code com.sorrowmist.useless.compat.jei.JEIPlugin} 里与本模组已移植子系统相关的部分。
 * 上游该类的其余内容（万象合金炉配方类别与配方页、催化剂信息页、全能样板转移、
 * {@code GenericStackJeiIngredientProviders} 等）属于机器子系统，未随照抄带入。</p>
 *
 * <p><b>本次死代码清理</b>：连锁等价组界面已随牛排工具框架删除，它的 JEI 拖拽处理器
 * （{@code ChainGroupGhostHandler}）与 {@code ChainGroupScreen} 相关代码一并移除；
 * 上游「无线物流界面」的处理器此前已随该子系统移除。本类现在只剩无用维度配置界面一个处理器。</p>
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

    /** 供其它界面查询 JEI 物品列表；JEI 缺席时为 {@code null}。 */
    @Nullable
    public static IJeiRuntime getRuntime() {
        return runtime;
    }

    /* ==================== JEI 拖拽（照抄上游 registerGuiHandlers） ==================== */

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(DimensionConfigScreen.class,
                new DimensionConfigGhostHandler());
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
