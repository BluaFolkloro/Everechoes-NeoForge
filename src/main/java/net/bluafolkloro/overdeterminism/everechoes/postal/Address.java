package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装邮政地址值对象。
 */
public sealed interface Address permits MailBoxAddress, PlayerAddress {
}
