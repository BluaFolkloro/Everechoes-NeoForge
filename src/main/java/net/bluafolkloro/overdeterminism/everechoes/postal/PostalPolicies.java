package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 封装邮政业务的权限与准入策略。
 */
public final class PostalPolicies {
    /**
     * 定义 {@code OPEN} 常量。
     */
    public static final String OPEN = "open";
    /**
     * 定义 {@code COVERED} 常量。
     */
    public static final String COVERED = "covered";
    /**
     * 定义 {@code ALLOW_CONFIG} 常量。
     */
    public static final String ALLOW_CONFIG = "allow_config";

    /**
     * 定义 {@code CREATION} 常量。
     */
    private static final Map<String, DomainCreationPolicy> CREATION = new ConcurrentHashMap<>();
    /**
     * 定义 {@code MEMBERSHIP} 常量。
     */
    private static final Map<String, DomainMembershipPolicy> MEMBERSHIP = new ConcurrentHashMap<>();
    /**
     * 定义 {@code EXIT} 常量。
     */
    private static final Map<String, DomainExitPolicy> EXIT = new ConcurrentHashMap<>();
    /**
     * 定义 {@code NODE_ADMISSION} 常量。
     */
    private static final Map<String, DistrictNodeAdmissionPolicy> NODE_ADMISSION = new ConcurrentHashMap<>();
    /**
     * 定义 {@code INTERACTION} 常量。
     */
    private static final Map<String, InteractionPolicy> INTERACTION = new ConcurrentHashMap<>();

    static {
        CREATION.put(OPEN, OpenDomainPolicies.CREATION);
        MEMBERSHIP.put(OPEN, OpenDomainPolicies.MEMBERSHIP);
        EXIT.put(OPEN, OpenDomainPolicies.EXIT);
        NODE_ADMISSION.put(COVERED, CoveredNodeAdmissionPolicy.INSTANCE);
        INTERACTION.put(ALLOW_CONFIG, /** 保存 {@code context} 对应的状态或依赖。 */ context -> PolicyDecision.allow());
    }

    /**
     * 创建并初始化当前对象。
     */
    private PostalPolicies() {
    }

    /**
     * 执行 {@code creation} 对应的业务操作。
     */
    public static DomainCreationPolicy creation(String policyId) {
        return findCreation(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown creation policy: " + policyId));
    }

    /**
     * 执行 {@code membership} 对应的业务操作。
     */
    public static DomainMembershipPolicy membership(String policyId) {
        return findMembership(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown membership policy: " + policyId));
    }

    /**
     * 执行 {@code exit} 对应的业务操作。
     */
    public static DomainExitPolicy exit(String policyId) {
        return findExit(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown exit policy: " + policyId));
    }

    /**
     * 执行 {@code nodeAdmission} 对应的业务操作。
     */
    public static DistrictNodeAdmissionPolicy nodeAdmission(String policyId) {
        return findNodeAdmission(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown node admission policy: " + policyId));
    }

    /**
     * 执行 {@code interaction} 对应的业务操作。
     */
    public static InteractionPolicy interaction(String policyId) {
        return findInteraction(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown interaction policy: " + policyId));
    }

    /**
     * 获取或解析 {@code findCreation} 对应的业务操作。
     */
    public static Optional<DomainCreationPolicy> findCreation(String policyId) {
        return Optional.ofNullable(CREATION.get(policyId));
    }

    /**
     * 获取或解析 {@code findMembership} 对应的业务操作。
     */
    public static Optional<DomainMembershipPolicy> findMembership(String policyId) {
        return Optional.ofNullable(MEMBERSHIP.get(policyId));
    }

    /**
     * 获取或解析 {@code findExit} 对应的业务操作。
     */
    public static Optional<DomainExitPolicy> findExit(String policyId) {
        return Optional.ofNullable(EXIT.get(policyId));
    }

    /**
     * 获取或解析 {@code findNodeAdmission} 对应的业务操作。
     */
    public static Optional<DistrictNodeAdmissionPolicy> findNodeAdmission(String policyId) {
        return Optional.ofNullable(NODE_ADMISSION.get(policyId));
    }

    /**
     * 获取或解析 {@code findInteraction} 对应的业务操作。
     */
    public static Optional<InteractionPolicy> findInteraction(String policyId) {
        return Optional.ofNullable(INTERACTION.get(policyId));
    }

    /**
     * 注册 {@code registerCreation} 对应的业务操作。
     */
    public static void registerCreation(String policyId, DomainCreationPolicy policy) {
        CREATION.put(policyId, policy);
    }

    /**
     * 注册 {@code registerMembership} 对应的业务操作。
     */
    public static void registerMembership(String policyId, DomainMembershipPolicy policy) {
        MEMBERSHIP.put(policyId, policy);
    }

    /**
     * 注册 {@code registerExit} 对应的业务操作。
     */
    public static void registerExit(String policyId, DomainExitPolicy policy) {
        EXIT.put(policyId, policy);
    }

    /**
     * 注册 {@code registerNodeAdmission} 对应的业务操作。
     */
    public static void registerNodeAdmission(String policyId, DistrictNodeAdmissionPolicy policy) {
        NODE_ADMISSION.put(policyId, policy);
    }

    /**
     * 注册 {@code registerInteraction} 对应的业务操作。
     */
    public static void registerInteraction(String policyId, InteractionPolicy policy) {
        INTERACTION.put(policyId, policy);
    }
}
