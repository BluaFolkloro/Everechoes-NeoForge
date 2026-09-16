package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.bluafolkloro.overdeterminism.everechoes.menu.PostBoxConfigMenu;
import net.bluafolkloro.overdeterminism.everechoes.network.PostBoxConfigPayload;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalCodes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PostBoxConfigScreen extends AbstractContainerScreen<PostBoxConfigMenu> {
    private static final int PANEL = 0xFF2B2118;
    private static final int PAPER = 0xFFF5E6C8;
    private static final int INK = 0x3F2A14;

    @Nullable
    private EditBox domainBox;
    @Nullable
    private CycleButton<String> domainCycle;

    public PostBoxConfigScreen(PostBoxConfigMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 140;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        domainBox = new EditBox(font, leftPos + 8, topPos + 48, 80, 18, Component.translatable("gui.everechoes.post_box.domain"));
        domainBox.setMaxLength(PostalCodes.DOMAIN_MAX);
        domainBox.setFilter(value -> value.chars().allMatch(Character::isLetter));
        addRenderableWidget(domainBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.post_box.create"), button -> send(PostBoxConfigPayload.Action.CREATE))
                .bounds(leftPos + 92, topPos + 47, 76, 20)
                .build());

        List<String> domains = menu.domainIds();
        if (!domains.isEmpty()) {
            String initial = menu.domainId() != null && domains.contains(menu.domainId()) ? menu.domainId() : domains.getFirst();
            domainCycle = addRenderableWidget(CycleButton.builder(Component::literal)
                    .withValues(domains)
                    .withInitialValue(initial)
                    .create(
                            leftPos + 8,
                            topPos + 76,
                            80,
                            20,
                            Component.translatable("gui.everechoes.post_box.existing"),
                            (button, value) -> {
                            }
                    ));
            addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.post_box.select"), button -> send(PostBoxConfigPayload.Action.SELECT))
                    .bounds(leftPos + 92, topPos + 76, 76, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(leftPos + 48, topPos + 108, 80, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PAPER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, INK, false);
        Component current = menu.domainId() != null && menu.districtId() != null
                ? Component.translatable(
                        "gui.everechoes.post_box.current",
                        PostalCodes.formatDistrict(menu.domainId(), menu.districtId())
                )
                : Component.translatable("gui.everechoes.post_box.unassigned");
        graphics.drawString(font, current, 8, 22, INK, false);
        graphics.drawString(font, Component.translatable("gui.everechoes.post_box.domain"), 8, 38, INK, false);
    }

    private void send(PostBoxConfigPayload.Action action) {
        String domain = action == PostBoxConfigPayload.Action.SELECT && domainCycle != null
                ? domainCycle.getValue()
                : domainBox == null ? "" : domainBox.getValue();
        PacketDistributor.sendToServer(new PostBoxConfigPayload(action, menu.pos(), domain));
    }
}
