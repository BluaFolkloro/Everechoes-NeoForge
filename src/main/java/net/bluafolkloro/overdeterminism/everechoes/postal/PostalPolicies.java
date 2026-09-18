package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PostalPolicies {
    public static final String OPEN = "open";
    public static final String COVERED = "covered";
    public static final String ALLOW_CONFIG = "allow_config";

    private static final Map<String, DomainCreationPolicy> CREATION = new ConcurrentHashMap<>();
    private static final Map<String, DomainMembershipPolicy> MEMBERSHIP = new ConcurrentHashMap<>();
    private static final Map<String, DomainExitPolicy> EXIT = new ConcurrentHashMap<>();
    private static final Map<String, DistrictNodeAdmissionPolicy> NODE_ADMISSION = new ConcurrentHashMap<>();
    private static final Map<String, InteractionPolicy> INTERACTION = new ConcurrentHashMap<>();

    static {
        CREATION.put(OPEN, OpenDomainPolicies.CREATION);
        MEMBERSHIP.put(OPEN, OpenDomainPolicies.MEMBERSHIP);
        EXIT.put(OPEN, OpenDomainPolicies.EXIT);
        NODE_ADMISSION.put(COVERED, CoveredNodeAdmissionPolicy.INSTANCE);
        INTERACTION.put(ALLOW_CONFIG, context -> PolicyDecision.allow());
    }

    private PostalPolicies() {
    }

    public static DomainCreationPolicy creation(String policyId) {
        return findCreation(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown creation policy: " + policyId));
    }

    public static DomainMembershipPolicy membership(String policyId) {
        return findMembership(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown membership policy: " + policyId));
    }

    public static DomainExitPolicy exit(String policyId) {
        return findExit(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown exit policy: " + policyId));
    }

    public static DistrictNodeAdmissionPolicy nodeAdmission(String policyId) {
        return findNodeAdmission(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown node admission policy: " + policyId));
    }

    public static InteractionPolicy interaction(String policyId) {
        return findInteraction(policyId).orElseThrow(() -> new IllegalArgumentException("Unknown interaction policy: " + policyId));
    }

    public static Optional<DomainCreationPolicy> findCreation(String policyId) {
        return Optional.ofNullable(CREATION.get(policyId));
    }

    public static Optional<DomainMembershipPolicy> findMembership(String policyId) {
        return Optional.ofNullable(MEMBERSHIP.get(policyId));
    }

    public static Optional<DomainExitPolicy> findExit(String policyId) {
        return Optional.ofNullable(EXIT.get(policyId));
    }

    public static Optional<DistrictNodeAdmissionPolicy> findNodeAdmission(String policyId) {
        return Optional.ofNullable(NODE_ADMISSION.get(policyId));
    }

    public static Optional<InteractionPolicy> findInteraction(String policyId) {
        return Optional.ofNullable(INTERACTION.get(policyId));
    }

    public static void registerCreation(String policyId, DomainCreationPolicy policy) {
        CREATION.put(policyId, policy);
    }

    public static void registerMembership(String policyId, DomainMembershipPolicy policy) {
        MEMBERSHIP.put(policyId, policy);
    }

    public static void registerExit(String policyId, DomainExitPolicy policy) {
        EXIT.put(policyId, policy);
    }

    public static void registerNodeAdmission(String policyId, DistrictNodeAdmissionPolicy policy) {
        NODE_ADMISSION.put(policyId, policy);
    }

    public static void registerInteraction(String policyId, InteractionPolicy policy) {
        INTERACTION.put(policyId, policy);
    }
}
