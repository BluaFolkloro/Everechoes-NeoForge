package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PostalPolicies {
    public static final String OPEN = "open";
    public static final String NEARBY = "nearby";
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
        NODE_ADMISSION.put(NEARBY, NearbyNodeAdmissionPolicy.INSTANCE);
        INTERACTION.put(ALLOW_CONFIG, context -> PolicyDecision.allow());
    }

    private PostalPolicies() {
    }

    public static DomainCreationPolicy creation(String policyId) {
        return CREATION.getOrDefault(policyId, OpenDomainPolicies.CREATION);
    }

    public static DomainMembershipPolicy membership(String policyId) {
        return MEMBERSHIP.getOrDefault(policyId, OpenDomainPolicies.MEMBERSHIP);
    }

    public static DomainExitPolicy exit(String policyId) {
        return EXIT.getOrDefault(policyId, OpenDomainPolicies.EXIT);
    }

    public static DistrictNodeAdmissionPolicy nodeAdmission(String policyId) {
        return NODE_ADMISSION.getOrDefault(policyId, NearbyNodeAdmissionPolicy.INSTANCE);
    }

    public static InteractionPolicy interaction(String policyId) {
        return INTERACTION.getOrDefault(policyId, context -> PolicyDecision.allow());
    }

    public static void registerMembership(String policyId, DomainMembershipPolicy policy) {
        MEMBERSHIP.put(policyId, policy);
    }
}
