package net.bluafolkloro.overdeterminism.everechoes.postal;

public interface DomainMembershipPolicy {
    String policyId();

    PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request);

    default boolean autoActivate() {
        return false;
    }
}
