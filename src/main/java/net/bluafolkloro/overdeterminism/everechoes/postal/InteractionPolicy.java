package net.bluafolkloro.overdeterminism.everechoes.postal;

// Config permission is separate from domain ownership. Players are never domain owners.
// 配置权限与邮域所有权分离。玩家永远不是邮域所有者。
public interface InteractionPolicy {
    default String policyId() {
        return PostalPolicies.ALLOW_CONFIG;
    }

    PolicyDecision canSubmit(PostalActionContext context);
}
