package com.godofthings.client.screen;

import com.godofthings.godgrid.AbstractGodGridScreen;
import com.godofthings.menu.GodResourceMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之资源三机（矿物 / 作物 / 复制）共用界面：网格面板（17 输入 + 17 输出 + 加速槽、箭头、
 * 三个页签）全部由 {@link AbstractGodGridScreen} 绘制，本类只绑定菜单类型。
 *
 * <p>旧 176×166 的按变体贴图（textures/gui/ore_machine|crop_machine|copy_machine.png）
 * 随面板一起停用 —— 共用框架纯程序化绘制、宽 362 塞不进 256 贴图空间；三台机器的区分仍在
 * {@link com.godofthings.menu.GodResourceMenu#gridBlock()}（按变体取方块）与按变体生成的界面标题上。</p>
 */
public class GodResourceScreen extends AbstractGodGridScreen<GodResourceMenu>
{
    public GodResourceScreen(GodResourceMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }
}
