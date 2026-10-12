package net.bluafolkloro.overdeterminism.everechoes.postal;

// Config permission is separate from domain ownership. Players are never domain owners.
// 配置权限与邮域所有权分离。玩家永远不是邮域所有者。
/**
 * 封装邮政业务的权限与准入策略。
 */
public interface InteractionPolicy {
    /**
     * 执行 {@code policyId} 对应的业务操作。
     */
    default String policyId() {
        return PostalPolicies.ALLOW_CONFIG;
    }

    /**
     * 判断 {@code canSubmit} 对应的业务操作。
     */
    PolicyDecision canSubmit(PostalActionContext context);
}
