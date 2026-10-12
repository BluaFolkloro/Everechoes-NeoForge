package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 信箱端点在线状态：在线可投递绑定，离线仍保留区内投递码占用。
 */
public enum MailBoxEndpointState {
    /**
     * 端点在线。
     */
    ONLINE,
    /**
     * 端点离线；已绑定的投递码继续占用。
     */
    OFFLINE
}
