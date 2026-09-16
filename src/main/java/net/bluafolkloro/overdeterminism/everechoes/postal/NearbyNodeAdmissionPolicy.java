package net.bluafolkloro.overdeterminism.everechoes.postal;

public final class NearbyNodeAdmissionPolicy implements DistrictNodeAdmissionPolicy {
    public static final NearbyNodeAdmissionPolicy INSTANCE = new NearbyNodeAdmissionPolicy();

    private NearbyNodeAdmissionPolicy() {
    }

    @Override
    public String policyId() {
        return PostalPolicies.NEARBY;
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
        PostBoxNode anchor = network.hubOf(district.districtId()).or(() -> network.nodesOf(district.districtId()).stream().findFirst())
                .orElse(null);
        if (anchor == null) {
            return PolicyDecision.deny("message.everechoes.post_box.unknown_district");
        }
        if (!anchor.dimension().equals(node.dimension())) {
            return PolicyDecision.deny("message.everechoes.post_box.too_far");
        }
        if (anchor.position().distSqr(node.position()) > (long) PostalCodes.NEARBY_NODE_RANGE * PostalCodes.NEARBY_NODE_RANGE) {
            return PolicyDecision.deny("message.everechoes.post_box.too_far");
        }
        return PolicyDecision.allow();
    }
}
