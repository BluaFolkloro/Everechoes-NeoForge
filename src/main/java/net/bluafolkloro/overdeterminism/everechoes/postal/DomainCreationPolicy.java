package net.bluafolkloro.overdeterminism.everechoes.postal;

public interface DomainCreationPolicy {
    String policyId();

    PolicyDecision canCreate(PostalNetwork network, DomainCreationRequest request);
}
