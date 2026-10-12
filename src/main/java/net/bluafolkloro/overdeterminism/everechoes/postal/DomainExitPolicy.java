package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装邮政业务的权限与准入策略。
 */
public interface DomainExitPolicy {
    /**
     * 执行 {@code policyId} 对应的业务操作。
     */
    String policyId();

    /**
     * 判断 {@code canRequestExit} 对应的业务操作。
     */
    PolicyDecision canRequestExit(PostalNetwork network, DomainMembership membership);

    /**
     * 判断 {@code canCompleteExit} 对应的业务操作。
     */
    PolicyDecision canCompleteExit(PostalNetwork network, DomainMembership membership);
}
