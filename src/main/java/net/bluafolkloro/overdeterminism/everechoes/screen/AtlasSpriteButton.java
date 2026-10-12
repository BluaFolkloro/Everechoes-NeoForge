package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * 封装该模块的相关职责。
 */
@OnlyIn(Dist.CLIENT)
final class AtlasSpriteButton extends AbstractButton {
    /**
     * 保存 {@code normal} 对应的状态或依赖。
     */
    private final ResourceLocation normal;
    /**
     * 保存 {@code hover} 对应的状态或依赖。
     */
    private final ResourceLocation hover;
    /**
     * 保存 {@code pressed} 对应的状态或依赖。
     */
    private final ResourceLocation pressed;
    /**
     * 保存 {@code disabled} 对应的状态或依赖。
     */
    private final ResourceLocation disabled;
    /**
     * 保存 {@code textureSize} 对应的状态或依赖。
     */
    private final int textureSize;
    /**
     * 保存 {@code sliceCorner} 对应的状态或依赖。
     */
    private final int sliceCorner;
    /**
     * 保存 {@code onPress} 对应的状态或依赖。
     */
    private final OnPress onPress;
    /**
     * 保存 {@code drawLabel} 对应的状态或依赖。
     */
    private final boolean drawLabel;

    /**
     * 创建并初始化当前对象。
     */
    AtlasSpriteButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            ResourceLocation normal,
            ResourceLocation hover,
            ResourceLocation pressed,
            ResourceLocation disabled,
            int textureSize,
            int sliceCorner,
            boolean drawLabel,
            OnPress onPress
    ) {
        super(x, y, width, height, message);
        this.normal = normal;
        this.hover = hover;
        this.pressed = pressed;
        this.disabled = disabled;
        this.textureSize = textureSize;
        this.sliceCorner = sliceCorner;
        this.drawLabel = drawLabel;
        this.onPress = onPress;
    }

    /**
     * 执行 {@code nine} 对应的业务操作。
     */
    static AtlasSpriteButton nine(
            int x,
            int y,
            int width,
            int height,
            Component label,
            String face,
            OnPress onPress
    ) {
        return new AtlasSpriteButton(
                x, y, width, height, label,
                tex(face + "_normal"),
                tex(face + "_hover"),
                tex(face + "_pressed"),
                tex(face + "_disabled"),
                16,
                4,
                true,
                onPress
        );
    }

    /**
     * 执行 {@code compass} 对应的业务操作。
     */
    static AtlasSpriteButton compass(
            int x,
            int y,
            String dir,
            Component tooltip,
            OnPress onPress
    ) {
        AtlasSpriteButton button = new AtlasSpriteButton(
                x, y, AtlasLayout.COMPASS_BTN, AtlasLayout.COMPASS_BTN, Component.empty(),
                tex("compass_" + dir + "_normal"),
                tex("compass_" + dir + "_hover"),
                tex("compass_" + dir + "_pressed"),
                tex("compass_" + dir + "_disabled"),
                56,
                0,
                false,
                onPress
        );
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }

    /**
     * 执行 {@code icon} 对应的业务操作。
     */
    static AtlasSpriteButton icon(
            int x,
            int y,
            ResourceLocation texture,
            Component tooltip,
            OnPress onPress
    ) {
        AtlasSpriteButton button = new AtlasSpriteButton(
                x, y, AtlasLayout.COMPASS_BTN, AtlasLayout.COMPASS_BTN, Component.empty(),
                texture, texture, texture, texture,
                56,
                0,
                false,
                onPress
        );
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }

    /**
     * 执行 {@code tex} 对应的业务操作。
     */
    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("everechoes", "textures/gui/atlas/" + name + ".png");
    }

    /**
     * 处理 {@code onPress} 对应的业务操作。
     */
    @Override
    public void onPress() {
        onPress.onPress(this);
    }

    /**
     * 绘制 {@code renderWidget} 对应的业务操作。
     */
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation texture = sprite();
        if (sliceCorner > 0) {
            AtlasNine.nine(graphics, texture, getX(), getY(), getWidth(), getHeight(), textureSize, sliceCorner);
        } else {
            AtlasNine.blit(graphics, texture, getX(), getY(), getWidth(), getHeight(), textureSize);
        }
        if (drawLabel) {
            int color = active ? 0xFFF5E6C8 : 0xFFC3A69A;
            if ("btn_secondary".equals(faceName())) {
                color = active ? 0xFF2A1C10 : 0xFF76654E;
            } else if ("btn_ghost".equals(faceName())) {
                color = active ? 0xFF2A1C10 : 0xFF76654E;
            }
            Font font = Minecraft.getInstance().font;
            int textX = getX() + (getWidth() - font.width(getMessage())) / 2;
            int textY = getY() + (getHeight() - font.lineHeight) / 2;
            if (isHovered() && mouseDown()) {
                textY++;
            }
            graphics.enableScissor(getX() + 2, getY() + 1, getRight() - 2, getBottom() - 1);
            graphics.drawString(font, getMessage(), textX, textY, color | Mth.ceil(alpha * 255.0F) << 24, false);
            graphics.disableScissor();
        }
    }

    /**
     * 执行 {@code faceName} 对应的业务操作。
     */
    private String faceName() {
        String path = normal.getPath();
        int slash = path.lastIndexOf('/');
        String file = path.substring(slash + 1);
        int under = file.lastIndexOf('_');
        return file.substring(0, under);
    }

    /**
     * 执行 {@code sprite} 对应的业务操作。
     */
    private ResourceLocation sprite() {
        if (!active) {
            return disabled;
        }
        if (isHovered() && mouseDown()) {
            return pressed;
        }
        if (isHoveredOrFocused()) {
            return hover;
        }
        return normal;
    }

    /**
     * 执行 {@code mouseDown} 对应的业务操作。
     */
    private static boolean mouseDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
    }

    /**
     * 更新 {@code updateWidgetNarration} 对应的业务操作。
     */
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    /**
     * 封装可替换实现遵循的行为契约。
     */
    @FunctionalInterface
    interface OnPress {
        /**
         * 处理 {@code onPress} 对应的业务操作。
         */
        void onPress(AtlasSpriteButton button);
    }
}
