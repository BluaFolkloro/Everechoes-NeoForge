package net.bluafolkloro.overdeterminism.everechoes.letter;

import net.bluafolkloro.overdeterminism.everechoes.postal.Address;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

// Mutable letter payload and lifecycle data.
// 可变的信件内容与生命周期数据。
public class LetterData {

    // Stable letter identity; never changes after creation.
    private final UUID letterId;
    // Current lifecycle state: draft, sealed, or opened.
    private LetterState state;
    // Postal return address used for delivery failures.
    private Address returnAddress;
    // Optional postal destination address used by the delivery system.
    private Address recipientAddress;

    private String title;
    private String body;

    // Optional sender text written in the letter signature, not a postal address.
    private String signatureSender;
    // Optional recipient text written in the letter, not a postal address.
    private String letterRecipient;

    // Reconstructs letter data from persisted fields and validates state invariants.
    // 从持久化字段重建信件数据，并校验状态不变量。
    private LetterData(
            UUID letterId,
            LetterState state,
            Address returnAddress,
            Address recipientAddress,
            String title,
            String body,
            String signatureSender,
            String letterRecipient
    ) {
        this.letterId = Objects.requireNonNull(letterId, "letterId");
        this.state = Objects.requireNonNull(state, "state");
        this.returnAddress = Objects.requireNonNull(returnAddress, "returnAddress");
        this.recipientAddress = recipientAddress;
        this.title = Objects.requireNonNull(title, "title");
        this.body = Objects.requireNonNull(body, "body");
        this.signatureSender = normalizeOptionalText(signatureSender);
        this.letterRecipient = normalizeOptionalText(letterRecipient);
        validateStateInvariants();
    }

    // Reconstructs letter data from persisted fields and validates state invariants.
    // 从持久化字段重建信件数据，并校验状态不变量。
    public static LetterData reconstruct(
            UUID letterId,
            LetterState state,
            Address returnAddress,
            Address recipientAddress,
            String title,
            String body,
            String signatureSender,
            String letterRecipient
    ) {
        return new LetterData(
                letterId,
                state,
                returnAddress,
                recipientAddress,
                title,
                body,
                signatureSender,
                letterRecipient
        );
    }

    // Attempts to reconstruct letter data from persisted fields without throwing on invalid data.
    // 尝试从持久化字段重建信件数据；数据非法时不抛出异常，而是返回空结果。
    public static Optional<LetterData> tryReconstruct(
            UUID letterId,
            LetterState state,
            Address returnAddress,
            Address recipientAddress,
            String title,
            String body,
            String signatureSender,
            String letterRecipient
    ) {
        try {
            return Optional.of(reconstruct(
                    letterId,
                    state,
                    returnAddress,
                    recipientAddress,
                    title,
                    body,
                    signatureSender,
                    letterRecipient
            ));
        } catch (NullPointerException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    // Creates an editable draft with generated identity, no destination address, and empty written content.
    // 创建一封可编辑草稿，自动生成标识，不预设收件地址，并使用空白书写内容。
    public static LetterData createDraft(Address returnAddress) {
        return new LetterData(
                UUID.randomUUID(),
                LetterState.DRAFT,
                returnAddress,
                null,
                "",
                "",
                null,
                null
        );
    }

    // Returns the stable identity of this letter.
    // 返回这封信的稳定标识。
    public UUID letterId() {
        return letterId;
    }

    // Returns the current lifecycle state.
    // 返回当前生命周期状态。
    public LetterState state() {
        return state;
    }

    // Returns whether the letter is still editable.
    // 返回信件是否仍处于可编辑状态。
    public boolean isDraft() {
        return state == LetterState.DRAFT;
    }

    // State changes are intentionally one-way: DRAFT -> SEALED -> OPENED.
    // 状态变更有意设计为单向：草稿 -> 蜡封 -> 拆封。

    // Returns whether the letter can be sealed.
    // 返回信件当前是否可以封蜡。
    public boolean canSeal() {
        return state == LetterState.DRAFT && recipientAddress != null;
    }

    // Attempts to seal the letter without throwing when the state is invalid.
    // 尝试封蜡；状态不满足时不抛异常，而是返回 false。
    public boolean trySeal() {
        return trySeal(null);
    }

    // Attempts to seal the letter and reports a localized failure message key when user input is incomplete.
    // 尝试封蜡；当玩家输入不完整时，通过回调提供本地化失败提示键。
    public boolean trySeal(Consumer<String> failureMessageKeyConsumer) {
        if (!canSeal()) {
            notifySealFailure(failureMessageKeyConsumer);
            return false;
        }

        applyState(LetterState.SEALED);
        return true;
    }

    // Seals the letter or throws if it is not a sealable draft.
    // 封蜡信件；如果不是完整草稿则抛出异常。
    public void seal() {
        if (!canSeal()) {
            throw new IllegalStateException("Only complete draft letters can be sealed");
        }

        applyState(LetterState.SEALED);
    }

    // Returns whether the letter can be opened.
    // 返回蜡封信件当前是否可以拆封。
    public boolean canOpen() {
        return state == LetterState.SEALED;
    }

    // Attempts to open the letter without throwing when the state is invalid.
    // 尝试拆封；状态不满足时不抛异常，而是返回 false。
    public boolean tryOpen() {
        if (!canOpen()) {
            return false;
        }

        applyState(LetterState.OPENED);
        return true;
    }

    // Opens the sealed letter or throws if it is not sealed.
    // 拆封信件；如果信件并非蜡封状态则抛出异常。
    public void open() {
        if (!canOpen()) {
            throw new IllegalStateException("Only sealed letters can be opened");
        }

        applyState(LetterState.OPENED);
    }

    // Returns the postal return address.
    // 返回邮政退回地址。
    public Address returnAddress() {
        return returnAddress;
    }

    // Address and written-content fields can only be edited while the letter is a draft.
    // 地址与书写内容字段只能在草稿状态下编辑。
    public void setReturnAddress(Address returnAddress) {
        requireDraft();
        this.returnAddress = Objects.requireNonNull(returnAddress, "returnAddress");
    }

    // Returns the optional postal destination address.
    // 返回可选的邮政目标地址。
    public Optional<Address> recipientAddress() {
        return Optional.ofNullable(recipientAddress);
    }

    // Sets the postal destination address while the letter is a draft.
    // 在草稿状态下设置信件的邮政目标地址。
    public void setRecipientAddress(Address recipientAddress) {
        requireDraft();
        this.recipientAddress = Objects.requireNonNull(recipientAddress, "recipientAddress");
    }

    // Returns the written title.
    // 返回书写标题。
    public String title() {
        return title;
    }

    // Sets the written title while the letter is a draft.
    // 在草稿状态下设置信件标题。
    public void setTitle(String title) {
        requireDraft();
        this.title = Objects.requireNonNull(title, "title");
    }

    // Returns the written body.
    // 返回书写正文。
    public String body() {
        return body;
    }

    // Sets the written body while the letter is a draft.
    // 在草稿状态下设置信件正文。
    public void setBody(String body) {
        requireDraft();
        this.body = Objects.requireNonNull(body, "body");
    }

    // Returns the optional signature sender text.
    // 返回可选的落款寄件人文本。
    public Optional<String> signatureSender() {
        return Optional.ofNullable(signatureSender);
    }

    // Sets the optional signature sender text; blank text is treated as absent.
    // 设置可选的落款寄件人文本；空白文本会视为未填写。
    public void setSignatureSender(String signatureSender) {
        requireDraft();
        this.signatureSender = normalizeOptionalText(signatureSender);
    }

    // Returns the optional written recipient text.
    // 返回可选的信件收件人文本。
    public Optional<String> letterRecipient() {
        return Optional.ofNullable(letterRecipient);
    }

    // Sets the optional written recipient text; blank text is treated as absent.
    // 设置可选的信件收件人文本；空白文本会视为未填写。
    public void setLetterRecipient(String letterRecipient) {
        requireDraft();
        this.letterRecipient = normalizeOptionalText(letterRecipient);
    }

    private void requireDraft() {
        if (!isDraft()) {
            throw new IllegalStateException("Letter data can only be edited while in draft state");
        }
    }

    // Sends the most specific sealing failure message key to the caller when one is available.
    // 在存在明确封蜡失败原因时，将对应的提示翻译键交给调用方处理。
    private void notifySealFailure(Consumer<String> failureMessageKeyConsumer) {
        if (failureMessageKeyConsumer == null) {
            return;
        }

        if (state == LetterState.DRAFT && recipientAddress == null) {
            failureMessageKeyConsumer.accept("message.everechoes.letter.missing_recipient_address");
        }
    }

    private void applyState(LetterState state) {
        this.state = Objects.requireNonNull(state, "state");
        validateStateInvariants();
    }

    private void validateStateInvariants() {
        if ((state == LetterState.SEALED || state == LetterState.OPENED) && recipientAddress == null) {
            throw new IllegalArgumentException(state + " letter must have a recipient address");
        }
    }

    private static String normalizeOptionalText(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
