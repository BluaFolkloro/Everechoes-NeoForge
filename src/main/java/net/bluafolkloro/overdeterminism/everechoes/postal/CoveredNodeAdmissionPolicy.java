package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装邮政业务的权限与准入策略。
 */
public final class CoveredNodeAdmissionPolicy implements DistrictNodeAdmissionPolicy {
    /**
     * 定义 {@code INSTANCE} 常量。
     */
    public static final CoveredNodeAdmissionPolicy INSTANCE = new CoveredNodeAdmissionPolicy();

    /**
     * 创建并初始化当前对象。
     */
    private CoveredNodeAdmissionPolicy() {
    }

    /**
     * 执行 {@code policyId} 对应的业务操作。
     */
    @Override
    public String policyId() {
        return PostalPolicies.COVERED;
    }

    /**
     * 判断 {@code canAdmit} 对应的业务操作。
     */
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
