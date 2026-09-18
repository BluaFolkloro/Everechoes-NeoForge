package net.bluafolkloro.overdeterminism.everechoes.postal;

public interface DomainExitPolicy {
    String policyId();

    PolicyDecision canRequestExit(PostalNetwork network, DomainMembership membership);

    PolicyDecision canCompleteExit(PostalNetwork network, DomainMembership membership);
}
