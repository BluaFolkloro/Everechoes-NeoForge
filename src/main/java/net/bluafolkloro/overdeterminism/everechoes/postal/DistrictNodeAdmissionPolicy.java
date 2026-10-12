package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装邮政业务的权限与准入策略。
 */
public interface DistrictNodeAdmissionPolicy {
    /**
     * 执行 {@code policyId} 对应的业务操作。
     */
    String policyId();

    /**
     * 判断 {@code canAdmit} 对应的业务操作。
     */
    PolicyDecision canAdmit(PostalNetwork network, NodeJoinRequest request);
}
