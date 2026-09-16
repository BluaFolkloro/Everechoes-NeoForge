package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.bluafolkloro.overdeterminism.everechoes.letter.AddressInputKind;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterContents;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterData;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterLimits;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterState;
import net.bluafolkloro.overdeterminism.everechoes.menu.LetterMenu;
import net.bluafolkloro.overdeterminism.everechoes.network.LetterActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class LetterScreen extends AbstractContainerScreen<LetterMenu> {
    private static final int PANEL_COLOR = 0xFF2B2118;
    private static final int PAPER_COLOR = 0xFFF5E6C8;
    private static final int LABEL_COLOR = 0x3F2A14;
    private static final int BODY_COLOR = 0x2A1C10;

    private LetterState displayedState;
    private boolean skipSaveOnClose;
    @Nullable
    private EditBox titleBox;
    @Nullable
    private EditBox letterRecipientBox;
    @Nullable
    private EditBox addressValueBox;
    @Nullable
    private EditBox signatureBox;
    @Nullable
    private MultiLineEditBox bodyBox;
    @Nullable
    private CycleButton<AddressInputKind> addressKindButton;

    public LetterScreen(LetterMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = 220;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets(menu.letterData(minecraft.player));
    }

    @Override
    protected void containerTick() {
        LetterData data = menu.letterData(minecraft.player);
        if (data.state() != displayedState) {
            rebuildWidgets(data);
        }
    }

    private void rebuildWidgets(LetterData data) {
        this.clearWidgets();
        this.clearFocus();
        this.displayedState = data.state();
        this.titleBox = null;
        this.letterRecipientBox = null;
        this.addressValueBox = null;
        this.signatureBox = null;
        this.bodyBox = null;
        this.addressKindButton = null;

        if (data.isDraft()) {
            initDraftWidgets(data);
        } else {
            initReadOnlyButtons(data);
        }
    }

    private void initDraftWidgets(LetterData data) {
        int x = leftPos + 50;
        int fieldWidth = imageWidth - 58;

        titleBox = createLineBox(x, topPos + 8, fieldWidth, data.title(), LetterLimits.TITLE);
        letterRecipientBox = createLineBox(
                x,
                topPos + 28,
                fieldWidth,
                data.letterRecipient().orElse(""),
                LetterLimits.LETTER_RECIPIENT
        );

        AddressInputKind kind = LetterContents.addressInputKind(data.recipientAddress().orElse(null));
        addressKindButton = addRenderableWidget(CycleButton.builder(LetterScreen::addressKindLabel)
                .withValues(AddressInputKind.values())
                .withInitialValue(kind)
                .create(
                        leftPos + 8,
                        topPos + 48,
                        110,
                        18,
                        Component.translatable("gui.everechoes.letter.address_type"),
                        (button, value) -> updateAddressFieldVisibility()
                ));

        addressValueBox = createLineBox(
                leftPos + 122,
                topPos + 48,
                imageWidth - 130,
                LetterContents.addressInputValue(data.recipientAddress().orElse(null), minecraft.level),
                LetterLimits.ADDRESS_VALUE
        );
        addressValueBox.setHint(Component.translatable("gui.everechoes.letter.address.mailbox_path"));
        addressValueBox.setMaxLength(9);
        updateAddressFieldVisibility();

        bodyBox = new MultiLineEditBox(
                font,
                leftPos + 8,
                topPos + 82,
                imageWidth - 16,
                84,
                Component.translatable("gui.everechoes.letter.placeholder.body"),
                Component.translatable("gui.everechoes.letter.body")
        );
        bodyBox.setCharacterLimit(LetterLimits.BODY);
        bodyBox.setValue(data.body());
        addRenderableWidget(bodyBox);

        signatureBox = createLineBox(
                x,
                topPos + 170,
                fieldWidth,
                data.signatureSender().orElse(""),
                LetterLimits.SIGNATURE
        );

        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.letter.seal"), button -> sendAction(LetterActionPayload.Action.SEAL))
                .bounds(leftPos + 8, topPos + 192, 80, 20)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> closeAfterSaving())
                .bounds(leftPos + imageWidth - 88, topPos + 192, 80, 20)
                .build());
    }

    private void initReadOnlyButtons(LetterData data) {
        int buttonY = topPos + 192;
        if (data.isSealed()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.letter.open"), button -> sendAction(LetterActionPayload.Action.OPEN))
                    .bounds(leftPos + 8, buttonY, 80, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(leftPos + imageWidth - 88, buttonY, 80, 20)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            // Treat E as typing, not as "close inventory". GLFW still sends charTyped separately.
            // 把 E 当作输入，而不是关闭物品栏。GLFW 仍会另外派发 charTyped。
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private EditBox createLineBox(int x, int y, int width, String value, int maxLength) {
        EditBox box = new EditBox(font, x, y, width, 18, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void updateAddressFieldVisibility() {
        if (addressValueBox == null || addressKindButton == null) {
            return;
        }

        boolean visible = addressKindButton.getValue() != AddressInputKind.NONE;
        addressValueBox.visible = visible;
        addressValueBox.setEditable(visible);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_COLOR);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PAPER_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        LetterData data = menu.letterData(minecraft.player);
        if (data.isDraft()) {
            graphics.drawString(font, Component.translatable("gui.everechoes.letter.title"), 8, 12, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.everechoes.letter.letter_recipient"), 8, 32, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.everechoes.letter.body"), 8, 72, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.everechoes.letter.signature"), 8, 174, LABEL_COLOR, false);
            return;
        }

        graphics.drawString(font, this.title, 8, 8, LABEL_COLOR, false);
        graphics.drawString(font, recipientLine(data), 8, 28, BODY_COLOR, false);

        if (data.isSealed()) {
            graphics.drawWordWrap(
                    font,
                    Component.translatable("gui.everechoes.letter.sealed_hint"),
                    8,
                    52,
                    imageWidth - 16,
                    BODY_COLOR
            );
            return;
        }

        graphics.drawString(
                font,
                Component.translatable("gui.everechoes.letter.title_value", data.title().isBlank() ? "-" : data.title()),
                8,
                44,
                BODY_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.everechoes.letter.letter_recipient_value", data.letterRecipient().orElse("-")),
                8,
                58,
                BODY_COLOR,
                false
        );
        graphics.drawWordWrap(font, Component.literal(data.body().isBlank() ? " " : data.body()), 8, 78, imageWidth - 16, BODY_COLOR);
        graphics.drawString(
                font,
                Component.translatable("gui.everechoes.letter.signature_value", data.signatureSender().orElse("-")),
                8,
                174,
                BODY_COLOR,
                false
        );
    }

    private Component recipientLine(LetterData data) {
        return Component.translatable(
                "gui.everechoes.letter.recipient_value",
                data.recipientAddress()
                        .map(address -> LetterContents.formatAddress(address, minecraft.level))
                        .orElse(Component.translatable("item.everechoes.letter.tooltip.no_recipient"))
        );
    }

    @Override
    public void onClose() {
        saveDraftIfNeeded();
        super.onClose();
    }

    @Override
    public void removed() {
        saveDraftIfNeeded();
        super.removed();
    }

    private void closeAfterSaving() {
        saveDraftIfNeeded();
        onClose();
    }

    private void saveDraftIfNeeded() {
        if (skipSaveOnClose || displayedState != LetterState.DRAFT || minecraft == null || minecraft.getConnection() == null) {
            return;
        }

        skipSaveOnClose = true;
        PacketDistributor.sendToServer(createPayload(LetterActionPayload.Action.SAVE));
    }

    private void sendAction(LetterActionPayload.Action action) {
        PacketDistributor.sendToServer(createPayload(action));
        if (action == LetterActionPayload.Action.SEAL || action == LetterActionPayload.Action.OPEN) {
            skipSaveOnClose = true;
            onClose();
        }
    }

    private LetterActionPayload createPayload(LetterActionPayload.Action action) {
        LetterData data = menu.letterData(minecraft.player);
        AddressInputKind kind = addressKindButton == null ? LetterContents.addressInputKind(data.recipientAddress().orElse(null)) : addressKindButton.getValue();
        return new LetterActionPayload(
                action,
                menu.hand(),
                data.letterId(),
                valueOf(titleBox, data.title()),
                bodyBox == null ? data.body() : bodyBox.getValue(),
                valueOf(signatureBox, data.signatureSender().orElse("")),
                valueOf(letterRecipientBox, data.letterRecipient().orElse("")),
                kind,
                valueOf(addressValueBox, LetterContents.addressInputValue(data.recipientAddress().orElse(null), minecraft.level))
        );
    }

    private static String valueOf(@Nullable EditBox box, String fallback) {
        return box == null ? fallback : box.getValue();
    }

    private static Component addressKindLabel(AddressInputKind kind) {
        return switch (kind) {
            case NONE -> Component.translatable("gui.everechoes.letter.address.none");
            case MAILBOX -> Component.translatable("gui.everechoes.letter.address.mailbox");
            case PLAYER -> Component.translatable("gui.everechoes.letter.address.player");
        };
    }
}
