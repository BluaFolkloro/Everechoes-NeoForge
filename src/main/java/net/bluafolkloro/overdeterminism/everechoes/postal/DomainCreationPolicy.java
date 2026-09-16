package net.bluafolkloro.overdeterminism.everechoes.postal;

public interface DomainCreationPolicy {
    PolicyDecision canCreate(PostalNetwork network, DomainCreationRequest request);
}
