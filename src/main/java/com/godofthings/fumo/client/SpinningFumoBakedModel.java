/*
 * 照抄自 AE2 Lightning Tech Reborn（作者 MOAKIEE、CystrySU、gjmhmm8、_leng、TedXenon、MHanHanBing）
 * 的 fumo 玩偶系统，仅改动包名与命名空间，并裁掉本模组未移植的「超维猪咪」分支。
 *
 * 许可：本文件源码沿用上游的 GNU LGPL 3.0 —— 见仓库 LICENSES/AE2LT-LGPL-3.0.txt。
 * 相关素材（models/block/hoyoog_fumo.json 那份玩家模型）沿用上游的 CC BY-NC-SA 3.0 ——
 * 见 LICENSES/AE2LT-ASSETS-CC-BY-NC-SA-3.0.md（署名 / 禁止商用 / 相同方式共享）。
 * 本模组的贴图 textures/block/hoyoog_fumo.png 不是上游素材，是作者自己的皮肤。
 */
package com.godofthings.fumo.client;

import com.godofthings.fumo.blockentity.FumoBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

class SpinningFumoBakedModel extends BakedModelWrapper<BakedModel> {

    SpinningFumoBakedModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext displayContext, PoseStack poseStack,
                                     boolean leftHand) {
        originalModel.applyTransform(displayContext, poseStack, leftHand);
        if (displayContext == ItemDisplayContext.HEAD) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
                float angle = (minecraft.level.getGameTime() % 60L + partialTick)
                        * FumoBlockEntity.SPIN_DEGREES_PER_TICK;
                poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            }
        }
        return this;
    }
}
