package com.godofthings.client.screen;

import com.godofthings.client.ClientNoteCache;
import com.godofthings.client.GodNoteHud;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteHud;
import com.godofthings.network.GodNoteMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * 「拖动摆放」悬浮窗的编辑界面：一层透明界面，便签按真实位置/大小画出来，
 * 直接用鼠标把它拖到想放的地方，滚轮改大小，Esc 保存并回到记事本。
 *
 * <p>之所以做成一个 Screen 而不是在游戏里直接抓鼠标事件：没有界面打开时鼠标事件不会
 * 送到模组手里（点一下就是攻击/挖掘），所以用一层透明界面来接管输入，拖完再退出。</p>
 */
public class GodNoteHudEditScreen extends Screen
{
    private boolean dragging;
    /** 按下时鼠标相对便签左上角的偏移（屏幕像素，已经含缩放） */
    private double dragOffsetX;
    private double dragOffsetY;

    public GodNoteHudEditScreen()
    {
        super(Component.translatable("gui.godofthings.note.drag_title"));
    }

    private NoteBook book()
    {
        return ClientNoteCache.book();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        gui.fill(0, 0, this.width, this.height, 0x40000000);

        // 便签本体（editing = true → 金色边框示意可拖动）
        GodNoteHud.render(gui, this.font, book(), this.width, this.height, true);

        Component hint = Component.translatable("gui.godofthings.note.drag_hint");
        int hintW = this.font.width(hint);
        gui.fill(this.width / 2 - hintW / 2 - 6, 8, this.width / 2 + hintW / 2 + 6, 24, 0xC0000000);
        gui.drawString(this.font, hint, this.width / 2 - hintW / 2, 12, 0xFFE8C86A, false);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 半透明遮罩自己画（见 render）
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            int[] rect = GodNoteHud.rect(this.font, book(), this.width, this.height);
            if (mouseX >= rect[0] && mouseX <= rect[0] + rect[2]
                    && mouseY >= rect[1] && mouseY <= rect[1] + rect[3])
            {
                dragging = true;
                dragOffsetX = mouseX - rect[0];
                dragOffsetY = mouseY - rect[1];
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (dragging)
        {
            NoteHud hud = book().hud();
            int px = GodNoteHud.clampPos((int) Math.round(mouseX - dragOffsetX), scaledW(), this.width);
            int py = GodNoteHud.clampPos((int) Math.round(mouseY - dragOffsetY), scaledH(), this.height);
            GodNoteHud.storePosition(hud, px, py, this.width, this.height);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (dragging)
        {
            dragging = false;
            push();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        NoteHud hud = book().hud();
        hud.scale = Mth.clamp(hud.scale + (float) Math.signum(scrollY) * NoteHud.SCALE_STEP,
                NoteHud.MIN_SCALE, NoteHud.MAX_SCALE);
        // 缩放后重新夹一次位置，别让便签被顶出屏幕
        int[] rect = GodNoteHud.rect(this.font, book(), this.width, this.height);
        GodNoteHud.storePosition(hud, rect[0], rect[1], this.width, this.height);
        push();
        return true;
    }

    private int scaledW()
    {
        return GodNoteHud.rect(this.font, book(), this.width, this.height)[2];
    }

    private int scaledH()
    {
        return GodNoteHud.rect(this.font, book(), this.width, this.height)[3];
    }

    private void push()
    {
        GodNoteMessages.sendUpdate(book().copy());
    }

    @Override
    public void onClose()
    {
        push();
        // 回到记事本，方便接着改任务或调背景
        if (this.minecraft != null)
        {
            this.minecraft.setScreen(new GodNoteScreen());
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
