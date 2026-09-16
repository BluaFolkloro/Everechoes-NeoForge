package net.bluafolkloro.overdeterminism.everechoes.postal;

public interface DistrictNodeAdmissionPolicy {
    String policyId();

    PolicyDecision canAdmit(PostalNetwork network, NodeJoinRequest request);
}
