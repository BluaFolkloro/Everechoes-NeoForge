package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装不可变业务数据。
 */
public record DomainCreationRequest(/** 保存 {@code domainCode} 对应的状态或依赖。 */ String domainCode, /** 保存 {@code context} 对应的状态或依赖。 */ PostalActionContext context) {
}
