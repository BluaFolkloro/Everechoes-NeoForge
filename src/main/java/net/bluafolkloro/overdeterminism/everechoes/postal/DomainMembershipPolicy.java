package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装邮政业务的权限与准入策略。
 */
public interface DomainMembershipPolicy {
    /**
     * 执行 {@code policyId} 对应的业务操作。
     */
    String policyId();

    /**
     * 判断 {@code canJoin} 对应的业务操作。
     */
    PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request);

    /**
     * 执行 {@code autoActivate} 对应的业务操作。
     */
    default boolean autoActivate() {
        return false;
    }
}
