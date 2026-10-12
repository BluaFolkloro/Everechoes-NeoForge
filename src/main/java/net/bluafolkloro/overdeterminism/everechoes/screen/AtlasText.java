package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 封装该模块的相关职责。
 */
@OnlyIn(Dist.CLIENT)
final class AtlasText {
    /**
     * 封装一组受限且稳定的业务状态。
     */
    enum Align {
        /**
         * 定义 {@code LEFT} 常量。
         */
        LEFT,
        /**
         * 定义 {@code CENTER} 常量。
         */
        CENTER,
        /**
         * 定义 {@code RIGHT} 常量。
         */
        RIGHT
    }

    /**
     * 创建并初始化当前对象。
     */
    private AtlasText() {
    }

    /**
     * 绘制 {@code draw} 对应的业务操作。
     */
    static void draw(
            GuiGraphics graphics,
            Font font,
            Component text,
            AtlasLayout.Rect area,
            int originX,
            int originY,
            Align align,
            int color
    ) {
        draw(graphics, font, text, area, originX, originY, align, color, false);
    }

    /**
     * 绘制 {@code draw} 对应的业务操作。
     */
    static void draw(
            GuiGraphics graphics,
            Font font,
            Component text,
            AtlasLayout.Rect area,
            int originX,
            int originY,
            Align align,
            int color,
            boolean ellipsize
    ) {
        if (area.w() <= 0 || area.h() <= 0) {
            return;
        }
        int lineH = font.lineHeight;
        int y = originY + area.textY(lineH);
        Component shown = ellipsize ? fit(font, text, area.w()) : text;
        int tw = font.width(shown);
        int localX = switch (align) {
            case LEFT -> area.x();
            case CENTER -> area.x() + Math.max(0, (area.w() - tw) / 2);
            case RIGHT -> area.x() + Math.max(0, area.w() - tw);
        };
        int x = originX + localX;
        int x0 = originX + area.x();
        int y0 = originY + area.y();
        graphics.enableScissor(x0, y0, x0 + area.w(), y0 + area.h());
        graphics.drawString(font, shown, x, y, color, false);
        graphics.disableScissor();
    }

    /**
     * 执行 {@code truncated} 对应的业务操作。
     */
    static boolean truncated(Font font, Component text, AtlasLayout.Rect area) {
        return font.width(text) > area.w();
    }

    /**
     * 执行 {@code fit} 对应的业务操作。
     */
    static Component fit(Font font, Component text, int maxWidth) {
        if (maxWidth <= 0) {
            return Component.empty();
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int ellipsisW = font.width(ellipsis);
        String raw = text.getString();
        int cut = raw.length();
        while (cut > 0 && font.width(raw.substring(0, cut)) + ellipsisW > maxWidth) {
            cut--;
        }
        if (cut <= 0) {
            return Component.literal(ellipsis);
        }
        return Component.literal(raw.substring(0, cut) + ellipsis);
    }
}
