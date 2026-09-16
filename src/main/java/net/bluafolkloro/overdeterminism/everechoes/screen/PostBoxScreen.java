package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.bluafolkloro.overdeterminism.everechoes.menu.PostBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PostBoxScreen extends AbstractContainerScreen<PostBoxMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/hopper.png");

    public PostBoxScreen(PostBoxMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        graphics.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
