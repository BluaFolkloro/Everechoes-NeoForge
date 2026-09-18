package net.bluafolkloro.overdeterminism.everechoes.postal;

public final class CoveredNodeAdmissionPolicy implements DistrictNodeAdmissionPolicy {
    public static final CoveredNodeAdmissionPolicy INSTANCE = new CoveredNodeAdmissionPolicy();

    private CoveredNodeAdmissionPolicy() {
    }

    @Override
    public String policyId() {
        return PostalPolicies.COVERED;
    }

    @Override
    public PolicyDecision canAdmit(PostalNetwork network, NodeJoinRequest request) {
        if (!request.explicit()) {
            return PolicyDecision.deny("message.everechoes.post_box.join_not_explicit");
        }
        PostBoxNode node = network.node(request.nodeId()).orElse(null);
        PostalDistrict district = network.district(request.districtId()).orElse(null);
        if (node == null || district == null || district.isEmpty()) {
            return PolicyDecision.deny("message.everechoes.post_box.unknown_district");
        }
        if (node.districtId() != null) {
            return PolicyDecision.deny("message.everechoes.post_box.node_already_assigned");
        }
        if (!network.covers(district.districtId(), node.dimension(), node.position())) {
            return PolicyDecision.deny("message.everechoes.post_box.outside_district");
        }
        return PolicyDecision.allow();
    }
}
