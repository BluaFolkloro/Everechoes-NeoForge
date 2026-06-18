# 邮政系统架构

本文档记录 Everechoes 邮政系统的设计边界和核心概念。

## 核心目标

邮政系统负责把一封已封蜡的信从一个地址投递到另一个地址。信件内容本身由 `LetterData` 表达，邮政系统只关心路由、投递、失败和退回。

## 地址模型

当前命名边界：

- `post_box`：发信端，负责玩家投递或提交待投递信件。
- `mail_box`：未来的收信端，负责接收投递到具体邮政编码的信件。

地址基类：

```java
public sealed interface Address permits MailBoxAddress, PlayerAddress {
}
```

当前地址类型：

- `MailBoxAddress(String postalCode)`：收信邮箱地址，指向未来的 `mail_box`。
- `PlayerAddress(UUID playerId)`：玩家地址。

`Address` 使用 sealed interface，而不是枚举类型字段。这样 Java 类型本身就能表达地址分支，后续在序列化时再决定是否需要额外类型标签。

## 信件模型

`LetterData` 维护信件本体数据：

- `letterId`：信件唯一标识。
- `state`：草稿、蜡封、拆封。
- `returnAddress`：寄件地址，用于退回。
- `recipientAddress`：收件地址，用于投递。
- `title`：标题。
- `body`：正文。
- `signatureSender`：落款中的寄件人文本。
- `letterRecipient`：信件中写给谁的文本。

注意：`signatureSender` 和 `letterRecipient` 是信件内容元素，不是邮政地址。

## 状态流转

信件状态是单向的：

```text
DRAFT -> SEALED -> OPENED
```

- `DRAFT`：可编辑，可暂时没有收件地址。
- `SEALED`：不可编辑，必须有收件地址，可用于投递。
- `OPENED`：不可编辑，表示已拆封阅读。

## 发信邮筒职责

当前 `post_box` 仍是占位容器。目标设计中，`post_box` 应负责：

- 发起投递请求。
- 暂存或提交可投递信件。
- 将信件交给邮政系统路由。

`post_box` 不应直接负责信件正文编辑，也不负责接收投递完成的信件。收信职责留给未来的 `mail_box`。

## 投递流程草案

```text
玩家编辑草稿信件
-> 设置收件地址
-> 封蜡
-> 投入 post_box
-> 邮政系统解析 Address
-> 投递成功或退回 returnAddress
```

## 待明确问题

- 收信邮箱 `postalCode` 如何生成。
- 一个玩家是否可以拥有多个收信邮箱地址。
- 玩家地址是否代表玩家背包、玩家邮箱，还是系统收件箱。
- 离线玩家收件如何存储。
- 收信邮箱被破坏后，绑定地址是否保留。
- 投递是否即时完成，还是需要时间/队列。
