/*
 * 照抄自 AE2 Lightning Tech Reborn（作者 MOAKIEE、CystrySU、gjmhmm8、_leng、TedXenon、MHanHanBing）
 * 的 fumo 玩偶系统，仅改动包名与命名空间，并裁掉本模组未移植的「超维猪咪」分支。
 *
 * 许可：本文件源码沿用上游的 GNU LGPL 3.0 —— 见仓库 LICENSES/AE2LT-LGPL-3.0.txt。
 * 相关素材（models/block/hoyoog_fumo.json 那份玩家模型）沿用上游的 CC BY-NC-SA 3.0 ——
 * 见 LICENSES/AE2LT-ASSETS-CC-BY-NC-SA-3.0.md（署名 / 禁止商用 / 相同方式共享）。
 * 本模组的贴图 textures/block/hoyoog_fumo.png 不是上游素材，是作者自己的皮肤。
 */
package com.godofthings.fumo.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class FumoBlockItem extends BlockItem implements Equipable {
    @Nullable
    private final String tooltipKey;

    public FumoBlockItem(Block block, Item.Properties properties) {
        this(block, properties, null);
    }

    public FumoBlockItem(Block block, Item.Properties properties, String tooltipKey) {
        super(block, properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (tooltipKey == null) {
            return;
        }
        tooltipComponents.add(Component.translatable(tooltipKey + ".1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(tooltipKey + ".2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(tooltipKey + ".3").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(tooltipKey + ".4").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
