package net.bluafolkloro.overdeterminism.everechoes.postal;

public interface DomainMembershipPolicy {
    PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request);

    default boolean autoActivate() {
        return false;
    }
}
