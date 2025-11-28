package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.bluafolkloro.overdeterminism.everechoes.menu.MailBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MailBoxScreen extends AbstractContainerScreen<MailBoxMenu> {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/shulker_box.png");

    public MailBoxScreen(MailBoxMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        graphics.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}