package com.godofthings.infinityprovider.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import appeng.client.gui.widgets.ITooltip;

import com.godofthings.Godofthings;

/**
 * 多态贴图切换按钮（移植自 AE2LT 的 TextureToggleButton）：
 * 底层模型是「状态贴图数组 + 当前索引」，每态自带贴图与提示；
 * 点击只发通知，由服务端循环状态后经 GuiSync 回传新索引。
 */
public class InfToggleButton extends Button implements ITooltip {

    /** 本模组用到的按钮类型（状态名 → 贴图路径，索引与状态枚举一一对应）。 */
    public enum ButtonType {
        /** 普通/无线模式：索引对齐 ProviderMode。 */
        MODE("wired_mode", "wireless_mode"),
        /** 回收模式三态：索引对齐 ReturnMode（OFF/AUTO/EJECT）。 */
        AUTO_RETURN("return_off", "return_auto", "return_eject"),
        /** 自适应倍增开关。 */
        ADAPTIVE_BATCH("adaptive_batch_off", "adaptive_batch_on"),
        /** 无线派发策略：索引对齐 WirelessDispatchMode。 */
        WIRELESS_STRATEGY("single_target", "even_distribution"),
        /** 无线速度：索引对齐 WirelessSpeedMode。 */
        SPEED("speed_normal", "speed_fast"),
        /** 输入过滤：被动回流只收样板产物。 */
        FILTERED_IMPORT("filtered_import_off", "filtered_import_on");

        final List<ResourceLocation> textures;

        ButtonType(String... paths) {
            this.textures = new ArrayList<>(paths.length);
            for (var path : paths) {
                this.textures.add(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID,
                        "textures/gui/buttons/" + path + ".png"));
            }
        }
    }

    private final List<ResourceLocation> textures;
    private final List<List<Component>> tooltips;
    private int stateIndex;

    public InfToggleButton(ButtonType type, OnPress onPress) {
        super(0, 0, 16, 16, Component.empty(), onPress, DEFAULT_NARRATION);
        this.textures = type.textures;
        this.tooltips = new ArrayList<>(this.textures.size());
        for (int i = 0; i < this.textures.size(); i++) {
            this.tooltips.add(Collections.emptyList());
        }
    }

    public void setStateIndex(int index) {
        this.stateIndex = Math.max(0, Math.min(index, this.textures.size() - 1));
    }

    public int getStateIndex() {
        return this.stateIndex;
    }

    /** 两态兼容：索引 0 = OFF，1 = ON。 */
    public void setState(boolean on) {
        this.setStateIndex(on ? 1 : 0);
    }

    public void setTooltipAt(int index, List<Component> lines) {
        if (index >= 0 && index < this.tooltips.size()) {
            this.tooltips.set(index, lines == null ? Collections.emptyList() : lines);
        }
    }

    public void setTooltipOn(List<Component> lines) {
        this.setTooltipAt(1, lines);
    }

    public void setTooltipOff(List<Component> lines) {
        this.setTooltipAt(0, lines);
    }

    public void setVisibility(boolean visible) {
        this.visible = visible;
        this.active = visible;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        // 照抄上游 TextureToggleButton：先画 18×20 按钮底（hover/focus 三态），
        // 图标用 Blitter（16×16 参考尺寸），非激活半透明，zOffset=3 盖在底图上
        var yOffset = this.isHovered() ? 1 : 0;
        var background = this.isHovered() ? appeng.client.gui.Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                : this.isFocused() ? appeng.client.gui.Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS
                        : appeng.client.gui.Icon.TOOLBAR_BUTTON_BACKGROUND;
        background.getBlitter()
                .dest(this.getX() - 1, this.getY() + yOffset, 18, 20)
                .zOffset(2)
                .blit(guiGraphics);

        if (this.textures.isEmpty()) {
            return;
        }
        int idx = Math.min(this.stateIndex, this.textures.size() - 1);
        var blitter = appeng.client.gui.style.Blitter.texture(this.textures.get(idx), 16, 16)
                .src(0, 0, 16, 16);
        if (!this.active) {
            blitter.opacity(0.5f);
        }
        blitter.dest(this.getX(), this.getY() + 1 + yOffset).zOffset(3).blit(guiGraphics);
    }

    @Override
    public List<Component> getTooltipMessage() {
        if (this.stateIndex >= 0 && this.stateIndex < this.tooltips.size()) {
            return this.tooltips.get(this.stateIndex);
        }
        return Collections.emptyList();
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(this.getX(), this.getY(), this.width, this.height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return this.visible && !this.getTooltipMessage().isEmpty();
    }
}
