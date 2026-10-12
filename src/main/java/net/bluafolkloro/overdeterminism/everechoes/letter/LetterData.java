package net.bluafolkloro.overdeterminism.everechoes.letter;

import net.bluafolkloro.overdeterminism.everechoes.postal.Address;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

// Immutable letter payload and lifecycle value stored on letter items.
// 不可变的信件内容与生命周期值，作为信件物品的 Data Component 存储。
/**
 * 封装该模块的相关职责。
 */
public final class LetterData {

    // Stable letter identity; never changes after creation.
    /**
     * 保存 {@code letterId} 对应的状态或依赖。
     */
    private final UUID letterId;
    // Current lifecycle state: draft, sealed, or opened.
    /**
     * 保存 {@code state} 对应的状态或依赖。
     */
    private final LetterState state;
    // Postal return address used for delivery failures.
    /**
     * 保存 {@code returnAddress} 对应的状态或依赖。
     */
    private final Address returnAddress;
    // Optional postal destination address used by the delivery system.
    /**
     * 保存 {@code recipientAddress} 对应的状态或依赖。
     */
    private final Address recipientAddress;

    /**
     * 保存 {@code title} 对应的状态或依赖。
     */
    private final String title;
    /**
     * 保存 {@code body} 对应的状态或依赖。
     */
    private final String body;

    // Optional sender text written in the letter signature, not a postal address.
    /**
     * 保存 {@code signatureSender} 对应的状态或依赖。
     */
    private final String signatureSender;
    // Optional recipient text written in the letter, not a postal address.
    /**
     * 保存 {@code letterRecipient} 对应的状态或依赖。
     */
    private final String letterRecipient;

    // ===== Construction and reconstruction methods =====
    // ===== 构造与重建方法：集中控制信件对象的创建入口，并保证创建时完成基础校验 =====

    /**
     * 创建并初始化当前对象。
     */
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
    /**
     * 执行 {@code reconstruct} 对应的业务操作。
     */
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
    /**
     * 执行 {@code tryReconstruct} 对应的业务操作。
     */
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
    /**
     * 创建 {@code createDraft} 对应的业务操作。
     */
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

    // ===== Identity and state query methods =====
    // ===== 标识与状态查询方法：读取信件身份、生命周期状态，以及常用状态判断 =====

    /**
     * 执行 {@code letterId} 对应的业务操作。
     */
    public UUID letterId() {
        return letterId;
    }

    /**
     * 执行 {@code state} 对应的业务操作。
     */
    public LetterState state() {
        return state;
    }

    /**
     * 判断 {@code isDraft} 对应的业务操作。
     */
    public boolean isDraft() {
        return state == LetterState.DRAFT;
    }

    /**
     * 判断 {@code isSealed} 对应的业务操作。
     */
    public boolean isSealed() {
        return state == LetterState.SEALED;
    }

    /**
     * 判断 {@code isOpened} 对应的业务操作。
     */
    public boolean isOpened() {
        return state == LetterState.OPENED;
    }

    // State changes are intentionally one-way: DRAFT -> SEALED -> OPENED.
    // 状态变更有意设计为单向：草稿 -> 蜡封 -> 拆封。

    // ===== Lifecycle transition methods =====
    // ===== 生命周期转换方法：处理封蜡、拆封等状态推进逻辑；每次成功转换都返回新的不可变值 =====

    /**
     * 判断 {@code canSeal} 对应的业务操作。
     */
    public boolean canSeal() {
        return state == LetterState.DRAFT && recipientAddress != null;
    }

    /**
     * 执行 {@code trySeal} 对应的业务操作。
     */
    public Optional<LetterData> trySeal() {
        return trySeal(null);
    }

    /**
     * 执行 {@code trySeal} 对应的业务操作。
     */
    public Optional<LetterData> trySeal(Consumer<String> failureMessageKeyConsumer) {
        if (!canSeal()) {
            notifySealFailure(failureMessageKeyConsumer);
            return Optional.empty();
        }

        return Optional.of(withState(LetterState.SEALED));
    }

    /**
     * 执行 {@code seal} 对应的业务操作。
     */
    public LetterData seal() {
        return trySeal().orElseThrow(() -> new IllegalStateException("Only complete draft letters can be sealed"));
    }

    /**
     * 判断 {@code canOpen} 对应的业务操作。
     */
    public boolean canOpen() {
        return state == LetterState.SEALED;
    }

    /**
     * 执行 {@code tryOpen} 对应的业务操作。
     */
    public Optional<LetterData> tryOpen() {
        if (!canOpen()) {
            return Optional.empty();
        }

        return Optional.of(withState(LetterState.OPENED));
    }

    /**
     * 打开 {@code open} 对应的业务操作。
     */
    public LetterData open() {
        return tryOpen().orElseThrow(() -> new IllegalStateException("Only sealed letters can be opened"));
    }

    // ===== Postal address methods =====
    // ===== 邮政地址方法：读取或在草稿上替换退回地址与收件地址 =====

    /**
     * 执行 {@code returnAddress} 对应的业务操作。
     */
    public Address returnAddress() {
        return returnAddress;
    }

    /**
     * 执行 {@code withReturnAddress} 对应的业务操作。
     */
    public LetterData withReturnAddress(Address returnAddress) {
        requireDraft();
        return copy(
                this.letterId,
                this.state,
                Objects.requireNonNull(returnAddress, "returnAddress"),
                this.recipientAddress,
                this.title,
                this.body,
                this.signatureSender,
                this.letterRecipient
        );
    }

    /**
     * 执行 {@code recipientAddress} 对应的业务操作。
     */
    public Optional<Address> recipientAddress() {
        return Optional.ofNullable(recipientAddress);
    }

    /**
     * 执行 {@code requireRecipientAddress} 对应的业务操作。
     */
    public Address requireRecipientAddress() {
        return recipientAddress()
                .orElseThrow(() -> new IllegalStateException("Letter has no recipient address"));
    }

    /**
     * 执行 {@code withRecipientAddress} 对应的业务操作。
     */
    public LetterData withRecipientAddress(Address recipientAddress) {
        requireDraft();
        return copy(
                this.letterId,
                this.state,
                this.returnAddress,
                Objects.requireNonNull(recipientAddress, "recipientAddress"),
                this.title,
                this.body,
                this.signatureSender,
                this.letterRecipient
        );
    }

    /**
     * 执行 {@code withoutRecipientAddress} 对应的业务操作。
     */
    public LetterData withoutRecipientAddress() {
        requireDraft();
        return copy(
                this.letterId,
                this.state,
                this.returnAddress,
                null,
                this.title,
                this.body,
                this.signatureSender,
                this.letterRecipient
        );
    }

    // ===== Written content methods =====
    // ===== 书写内容方法：读取或在草稿上替换标题、正文、落款和信内称呼 =====

    /**
     * 执行 {@code title} 对应的业务操作。
     */
    public String title() {
        return title;
    }

    /**
     * 执行 {@code withTitle} 对应的业务操作。
     */
    public LetterData withTitle(String title) {
        requireDraft();
        return copy(
                this.letterId,
                this.state,
                this.returnAddress,
                this.recipientAddress,
                Objects.requireNonNull(title, "title"),
                this.body,
                this.signatureSender,
                this.letterRecipient
        );
    }

    /**
     * 执行 {@code body} 对应的业务操作。
     */
    public String body() {
        return body;
    }

    /**
     * 执行 {@code withBody} 对应的业务操作。
     */
    public LetterData withBody(String body) {
        requireDraft();
        return copy(
                this.letterId,
                this.state,
                this.returnAddress,
                this.recipientAddress,
                this.title,
                Objects.requireNonNull(body, "body"),
                this.signatureSender,
                this.letterRecipient
        );
    }

    /**
     * 执行 {@code signatureSender} 对应的业务操作。
     */
    public Optional<String> signatureSender() {
        return Optional.ofNullable(signatureSender);
    }

    /**
     * 执行 {@code withSignatureSender} 对应的业务操作。
     */
    public LetterData withSignatureSender(String signatureSender) {
        requireDraft();
        return copy(
                this.letterId,
                this.state,
                this.returnAddress,
                this.recipientAddress,
                this.title,
                this.body,
                signatureSender,
                this.letterRecipient
        );
    }

    /**
     * 执行 {@code letterRecipient} 对应的业务操作。
     */
    public Optional<String> letterRecipient() {
        return Optional.ofNullable(letterRecipient);
    }

    /**
     * 执行 {@code withLetterRecipient} 对应的业务操作。
     */
    public LetterData withLetterRecipient(String letterRecipient) {
        requireDraft();
        return copy(
                this.letterId,
                this.state,
                this.returnAddress,
                this.recipientAddress,
                this.title,
                this.body,
                this.signatureSender,
                letterRecipient
        );
    }

    // ===== Internal validation and helper methods =====
    // ===== 内部校验与辅助方法：集中处理状态限制、失败提示、不变量校验和文本规范化 =====

    /**
     * 执行 {@code requireDraft} 对应的业务操作。
     */
    private void requireDraft() {
        if (!isDraft()) {
            throw new IllegalStateException("Letter data can only be edited while in draft state");
        }
    }

    /**
     * 执行 {@code notifySealFailure} 对应的业务操作。
     */
    private void notifySealFailure(Consumer<String> failureMessageKeyConsumer) {
        if (failureMessageKeyConsumer == null) {
            return;
        }

        if (state == LetterState.DRAFT && recipientAddress == null) {
            failureMessageKeyConsumer.accept("message.everechoes.letter.missing_recipient_address");
        }
    }

    /**
     * 执行 {@code withState} 对应的业务操作。
     */
    private LetterData withState(LetterState state) {
        return copy(
                this.letterId,
                Objects.requireNonNull(state, "state"),
                this.returnAddress,
                this.recipientAddress,
                this.title,
                this.body,
                this.signatureSender,
                this.letterRecipient
        );
    }

    /**
     * 执行 {@code copy} 对应的业务操作。
     */
    private LetterData copy(
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

    /**
     * 校验 {@code validateStateInvariants} 对应的业务操作。
     */
    private void validateStateInvariants() {
        if ((state == LetterState.SEALED || state == LetterState.OPENED) && recipientAddress == null) {
            throw new IllegalArgumentException(state + " letter must have a recipient address");
        }
    }

    /**
     * 执行 {@code normalizeOptionalText} 对应的业务操作。
     */
    private static String normalizeOptionalText(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    /**
     * 执行 {@code equals} 对应的业务操作。
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LetterData letterData)) {
            return false;
        }

        return letterId.equals(letterData.letterId)
                && state == letterData.state
                && returnAddress.equals(letterData.returnAddress)
                && Objects.equals(recipientAddress, letterData.recipientAddress)
                && title.equals(letterData.title)
                && body.equals(letterData.body)
                && Objects.equals(signatureSender, letterData.signatureSender)
                && Objects.equals(letterRecipient, letterData.letterRecipient);
    }

    /**
     * 判断 {@code hashCode} 对应的业务操作。
     */
    @Override
    public int hashCode() {
        return Objects.hash(
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

    /**
     * 执行 {@code toString} 对应的业务操作。
     */
    @Override
    public String toString() {
        return "LetterData{"
                + "letterId=" + letterId
                + ", state=" + state
                + ", returnAddress=" + returnAddress
                + ", recipientAddress=" + recipientAddress
                + ", title='" + title + '\''
                + '}';
    }
}
