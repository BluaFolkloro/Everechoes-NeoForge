package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.bluafolkloro.overdeterminism.everechoes.menu.PostBoxConfigMenu;
import net.bluafolkloro.overdeterminism.everechoes.network.PostBoxConfigPayload;
import net.bluafolkloro.overdeterminism.everechoes.postal.MembershipState;
import net.bluafolkloro.overdeterminism.everechoes.postal.NodeRole;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalCodes;
import com.mojang.blaze3d.platform.InputConstants;
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
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PostBoxConfigScreen extends AbstractContainerScreen<PostBoxConfigMenu> {
    private static final int PANEL = 0xFF2B2118;
    private static final int PAPER = 0xFFF5E6C8;
    private static final int INK = 0x3F2A14;

    @Nullable
    private EditBox domainBox;
    @Nullable
    private CycleButton<String> domainCycle;
    @Nullable
    private CycleButton<PostBoxConfigMenu.CoveringDistrict> districtCycle;

    public PostBoxConfigScreen(PostBoxConfigMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + 40;
        if (menu.districtId() == null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.post_box.create_district"), button -> send(PostBoxConfigPayload.Action.CREATE_DISTRICT, "", null))
                    .bounds(leftPos + 8, y, 160, 20)
                    .build());
            y += 24;
            List<PostBoxConfigMenu.CoveringDistrict> joinable = menu.joinableDistricts();
            if (!joinable.isEmpty()) {
                List<PostBoxConfigMenu.CoveringDistrict> choices = new java.util.ArrayList<>();
                PostBoxConfigMenu.CoveringDistrict prompt = new PostBoxConfigMenu.CoveringDistrict(
                        null,
                        Component.translatable("gui.everechoes.post_box.choose_district").getString(),
                        null,
                        null,
                        true,
                        null
                );
                if (joinable.size() > 1) {
                    choices.add(prompt);
                }
                choices.addAll(joinable);
                districtCycle = addRenderableWidget(CycleButton.builder((PostBoxConfigMenu.CoveringDistrict value) -> Component.literal(value.label()))
                        .withValues(choices)
                        .withInitialValue(choices.getFirst())
                        .create(leftPos + 8, y, 80, 20, Component.translatable("gui.everechoes.post_box.covering_district"), (button, value) -> {
                        }));
                addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.post_box.join_district"), button -> {
                            UUID target = districtCycle == null ? null : districtCycle.getValue().districtId();
                            if (target == null) {
                                if (minecraft != null && minecraft.player != null) {
                                    minecraft.player.displayClientMessage(
                                            Component.translatable("gui.everechoes.post_box.choose_district"),
                                            true
                                    );
                                }
                                return;
                            }
                            send(PostBoxConfigPayload.Action.JOIN_DISTRICT, "", target);
                        })
                        .bounds(leftPos + 92, y, 76, 20)
                        .build());
                y += 24;
            }
        } else if (menu.membershipState() != MembershipState.ACTIVE && menu.membershipState() != MembershipState.PENDING && menu.membershipState() != MembershipState.LEAVING) {
            domainBox = new EditBox(font, leftPos + 8, y, 80, 18, Component.translatable("gui.everechoes.post_box.domain"));
            domainBox.setMaxLength(PostalCodes.DOMAIN_MAX);
            domainBox.setFilter(value -> value.chars().allMatch(Character::isLetter));
            addRenderableWidget(domainBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.post_box.establish"), button -> send(PostBoxConfigPayload.Action.ESTABLISH_DOMAIN, domainBox.getValue(), null))
                    .bounds(leftPos + 92, y, 76, 20)
                    .build());
            y += 24;
            List<String> domains = menu.domainCodes();
            if (!domains.isEmpty()) {
                domainCycle = addRenderableWidget(CycleButton.builder(Component::literal)
                        .withValues(domains)
                        .withInitialValue(domains.getFirst())
                        .create(leftPos + 8, y, 80, 20, Component.translatable("gui.everechoes.post_box.existing"), (button, value) -> {
                        }));
                addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.post_box.join"), button -> send(
                                PostBoxConfigPayload.Action.JOIN_DOMAIN,
                                domainCycle == null ? domains.getFirst() : domainCycle.getValue(),
                                null
                        ))
                        .bounds(leftPos + 92, y, 76, 20)
                        .build());
                y += 24;
            }
        }

        if (menu.membershipState() == MembershipState.ACTIVE) {
            addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.post_box.leave"), button -> send(PostBoxConfigPayload.Action.LEAVE_DOMAIN, "", null))
                    .bounds(leftPos + 8, y, 160, 20)
                    .build());
            y += 24;
        }
        if (menu.nodeRole() == NodeRole.COLLECTION && menu.districtId() != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.post_box.leave_district"), button -> send(PostBoxConfigPayload.Action.LEAVE_DISTRICT, "", null))
                    .bounds(leftPos + 8, y, 160, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(leftPos + 48, topPos + 158, 80, 20)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PAPER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, INK, false);
        graphics.drawString(font, statusLine(), 8, 22, INK, false);
        if (menu.districtId() == null && !menu.blockedDistricts().isEmpty()) {
            PostBoxConfigMenu.CoveringDistrict blocked = menu.blockedDistricts().getFirst();
            Component reason = blocked.reasonKey() == null
                    ? Component.translatable("gui.everechoes.post_box.covered_not_joinable")
                    : Component.translatable(blocked.reasonKey());
            graphics.drawString(font, reason, 8, 32, INK, false);
        }
    }

    private Component statusLine() {
        if (menu.membershipState() == MembershipState.ACTIVE && menu.domainCode() != null && menu.districtCode() != null) {
            return Component.translatable(
                    "gui.everechoes.post_box.current",
                    PostalCodes.formatOutward(menu.domainCode(), menu.districtCode())
            );
        }
        if (menu.membershipState() == MembershipState.PENDING) {
            return Component.translatable("gui.everechoes.post_box.pending");
        }
        if (menu.membershipState() == MembershipState.LEAVING) {
            return Component.translatable("gui.everechoes.post_box.leaving");
        }
        if (menu.districtId() != null) {
            return Component.translatable("gui.everechoes.post_box.unassigned");
        }
        return Component.translatable("gui.everechoes.post_box.no_district");
    }

    private void send(PostBoxConfigPayload.Action action, String domainCode, @Nullable UUID targetDistrict) {
        PacketDistributor.sendToServer(new PostBoxConfigPayload(action, menu.pos(), domainCode, targetDistrict));
    }
}
