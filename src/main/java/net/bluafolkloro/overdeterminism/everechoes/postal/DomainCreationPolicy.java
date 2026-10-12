package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装邮政业务的权限与准入策略。
 */
public interface DomainCreationPolicy {
    /**
     * 执行 {@code policyId} 对应的业务操作。
     */
    String policyId();

    /**
     * 判断 {@code canCreate} 对应的业务操作。
     */
    PolicyDecision canCreate(PostalNetwork network, DomainCreationRequest request);
}
