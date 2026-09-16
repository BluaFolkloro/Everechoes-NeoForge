package net.bluafolkloro.overdeterminism.everechoes.postal;

public interface DomainExitPolicy {
    PolicyDecision canRequestExit(PostalNetwork network, DomainMembership membership);

    PolicyDecision canCompleteExit(PostalNetwork network, DomainMembership membership);
}
