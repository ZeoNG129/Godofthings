package com.godofthings.doll.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 玩偶尾巴 —— <b>本模组裁剪版：空实现</b>。
 *
 * <p>上游的尾巴是一套 7 骨骼的蒙皮网格（约 500 行 + 13KB 几何 JSON + 专用贴图），
 * 几何顶点带毛皮 UV。按需求把尾巴连同几何/贴图一起移除后，{@link TwoIDollRenderer}
 * 里对 {@code tail.pose(...)} / {@code tail.render(...)} / {@code tail.tip*()} 的十多处
 * 调用点全部保留并指向本空实现 —— 调用是 no-op，几何查询返回 0 / 零向量，
 * 这样渲染器主体一行不用改，也不会再引用任何已删除的资源。</p>
 */
public final class TwoIDollTail {
    public TwoIDollTail() {
    }

    /** 每帧姿态更新：无尾巴，空操作。 */
    public void pose(boolean seated, float bodyY, float bodyZRot, float time,
                     float stride, float moving, TwoIDollCarryMotion.Pose carryMotion) {
    }

    /** 渲染：无尾巴，空操作。 */
    public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
    }

    public float tipYaw() {
        return 0.0F;
    }

    public float tipPitch() {
        return 0.0F;
    }

    public float bottom() {
        return 0.0F;
    }

    public Vec3 tip() {
        return Vec3.ZERO;
    }

    public Vec3 attachment() {
        return Vec3.ZERO;
    }
}