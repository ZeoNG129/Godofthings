package com.godofthings.note;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;

/**
 * 神之便签的「悬浮窗」显示设置（每个玩家一份，跟着便签数据一起存/同步）。
 *
 * <p>位置用<b>屏幕比例</b>存（0~1，指便签左上角），所以换分辨率、换 GUI 缩放都不会跑偏；
 * 大小是一个缩放系数，直接乘在绘制矩阵上（HUD 与编辑界面共用同一套算法）。</p>
 */
public final class NoteHud
{
    /** 背景样式：无 / 半透明黑 / 羊皮纸 / 深空蓝 / 自定义 */
    public static final int BG_NONE = 0;
    public static final int BG_DARK = 1;
    public static final int BG_PARCHMENT = 2;
    public static final int BG_SPACE = 3;
    public static final int BG_CUSTOM = 4;
    public static final int BG_COUNT = 5;

    public static final float MIN_SCALE = 0.5F;
    public static final float MAX_SCALE = 2.5F;
    public static final float SCALE_STEP = 0.25F;
    public static final float MIN_OPACITY = 0.1F;
    public static final float MAX_OPACITY = 1.0F;

    /** 位置步进（编辑界面里 ± 按钮一次挪多少屏宽/屏高） */
    public static final float POS_STEP = 0.02F;

    public boolean enabled;
    public float x = 0.02F;
    public float y = 0.08F;
    public float scale = 1.0F;
    public int background = BG_DARK;
    public int color = 0x101418;
    public float opacity = 0.6F;
    /** 悬浮窗里是否连「已完成」的条目一起显示 */
    public boolean showDone = true;

    public NoteHud copy()
    {
        NoteHud h = new NoteHud();
        h.enabled = enabled;
        h.x = x;
        h.y = y;
        h.scale = scale;
        h.background = background;
        h.color = color;
        h.opacity = opacity;
        h.showDone = showDone;
        return h;
    }

    /** 服务端落库前的兜底：所有字段夹到合法范围（客户端传什么都不会写坏存档） */
    public void clamp()
    {
        x = Mth.clamp(x, -0.5F, 1.5F);
        y = Mth.clamp(y, -0.5F, 1.5F);
        scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
        opacity = Mth.clamp(opacity, MIN_OPACITY, MAX_OPACITY);
        background = Mth.clamp(background, 0, BG_COUNT - 1);
        color &= 0xFFFFFF;
    }

    /** 背景样式的语言键（界面按钮上显示的名字） */
    public static String backgroundKey(int background)
    {
        return switch (background)
        {
            case BG_NONE -> "gui.godofthings.note.bg.none";
            case BG_PARCHMENT -> "gui.godofthings.note.bg.parchment";
            case BG_SPACE -> "gui.godofthings.note.bg.space";
            case BG_CUSTOM -> "gui.godofthings.note.bg.custom";
            default -> "gui.godofthings.note.bg.dark";
        };
    }

    public void save(CompoundTag tag)
    {
        tag.putBoolean("Enabled", enabled);
        tag.putFloat("X", x);
        tag.putFloat("Y", y);
        tag.putFloat("Scale", scale);
        tag.putInt("Background", background);
        tag.putInt("Color", color);
        tag.putFloat("Opacity", opacity);
        tag.putBoolean("ShowDone", showDone);
    }

    public void load(CompoundTag tag)
    {
        enabled = tag.getBoolean("Enabled");
        x = tag.contains("X") ? tag.getFloat("X") : x;
        y = tag.contains("Y") ? tag.getFloat("Y") : y;
        scale = tag.contains("Scale") ? tag.getFloat("Scale") : scale;
        background = tag.contains("Background") ? tag.getInt("Background") : background;
        color = tag.contains("Color") ? tag.getInt("Color") : color;
        opacity = tag.contains("Opacity") ? tag.getFloat("Opacity") : opacity;
        showDone = !tag.contains("ShowDone") || tag.getBoolean("ShowDone");
        clamp();
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeBoolean(enabled);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(scale);
        buf.writeVarInt(background);
        buf.writeInt(color);
        buf.writeFloat(opacity);
        buf.writeBoolean(showDone);
    }

    public static NoteHud read(RegistryFriendlyByteBuf buf)
    {
        NoteHud h = new NoteHud();
        h.enabled = buf.readBoolean();
        h.x = buf.readFloat();
        h.y = buf.readFloat();
        h.scale = buf.readFloat();
        h.background = buf.readVarInt();
        h.color = buf.readInt();
        h.opacity = buf.readFloat();
        h.showDone = buf.readBoolean();
        h.clamp();
        return h;
    }
}
