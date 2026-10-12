package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 封装该模块的相关职责。
 */
@OnlyIn(Dist.CLIENT)
final class AtlasNine {
    /**
     * 创建并初始化当前对象。
     */
    private AtlasNine() {
    }

    /**
     * 绘制 {@code blit} 对应的业务操作。
     */
    static void blit(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int textureSize
    ) {
        blit(graphics, texture, x, y, width, height, textureSize, textureSize);
    }

    /**
     * 绘制 {@code blit} 对应的业务操作。
     */
    static void blit(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight
    ) {
        graphics.blit(texture, x, y, width, height, 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    /**
     * 执行 {@code nine} 对应的业务操作。
     */
    static void nine(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int textureSize,
            int corner
    ) {
        int midSrc = textureSize - corner * 2;
        int midDstW = Math.max(0, width - corner * 2);
        int midDstH = Math.max(0, height - corner * 2);
        patch(graphics, texture, x, y, corner, corner, 0, 0, corner, corner, textureSize);
        patch(graphics, texture, x + width - corner, y, corner, corner, textureSize - corner, 0, corner, corner, textureSize);
        patch(graphics, texture, x, y + height - corner, corner, corner, 0, textureSize - corner, corner, corner, textureSize);
        patch(graphics, texture, x + width - corner, y + height - corner, corner, corner, textureSize - corner, textureSize - corner, corner, corner, textureSize);
        if (midDstW > 0) {
            patch(graphics, texture, x + corner, y, midDstW, corner, corner, 0, midSrc, corner, textureSize);
            patch(graphics, texture, x + corner, y + height - corner, midDstW, corner, corner, textureSize - corner, midSrc, corner, textureSize);
        }
        if (midDstH > 0) {
            patch(graphics, texture, x, y + corner, corner, midDstH, 0, corner, corner, midSrc, textureSize);
            patch(graphics, texture, x + width - corner, y + corner, corner, midDstH, textureSize - corner, corner, corner, midSrc, textureSize);
        }
        if (midDstW > 0 && midDstH > 0) {
            patch(graphics, texture, x + corner, y + corner, midDstW, midDstH, corner, corner, midSrc, midSrc, textureSize);
        }
    }

    /**
     * 执行 {@code patch} 对应的业务操作。
     */
    private static void patch(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int w,
            int h,
            int u,
            int v,
            int uw,
            int vh,
            int textureSize
    ) {
        graphics.blit(texture, x, y, w, h, (float) u, (float) v, uw, vh, textureSize, textureSize);
    }
}
