package com.direwolf20.justdirethings.client.renderers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 精简版渲染类型：仅保留区域预览需要的 SolidBoxArea（半透明方块面）与线条类型。
 * 线条复用原版 {@link RenderType#lines()}。
 */
public class OurRenderTypes extends RenderType {
	public static final RenderType SolidBoxArea = create("SolidBoxArea", DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS, 256, false, false,
			RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY).setTextureState(NO_TEXTURE)
					.setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(NO_LIGHTMAP)
					.setWriteMaskState(COLOR_WRITE).createCompositeState(false));

	public OurRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
			boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
