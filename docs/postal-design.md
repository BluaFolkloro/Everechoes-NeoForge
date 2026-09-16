# 邮政系统架构

本文档记录 Everechoes 邮政系统的设计边界和核心概念。

## 核心目标

邮政系统负责寻址、权限、路由和责任记录。邮件不会在容器之间自动传送；每封在途信件都必须由玩家、信鸽或未来的邮递 NPC 实际携带。

信件内容由 `LetterData` 表达。运输责任由独立的 `Waybill` 表达。

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

- `MailBoxAddress(domainId, districtId, deliveryId)`：三级邮箱地址，指向未来的递送点。
- `PlayerAddress(UUID playerId)`：当面交给玩家本人。
- `postalCode` 由上述三段组成，格式为 `AAAXX-YYY`。

字段规则：

- `domainId`：1 到 3 个字母，储存为大写。
- `districtId`：1 到 99 的十进制，储存时省略先导 0。
- `deliveryId`：1 到 `FFF` 的十六进制，储存为大写且省略先导 0。

写信时允许小写；保存时规范化为大写邮编，例如 `abc1-00a` 存成 `ABC1-A`。

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

`post_box` 是承运交接口，不是自动传送机：

- 只接受带有效 `LetterData` 的 `sealed_letter`。
- 暂存待领取信件，并把运单标为“待承运人领取”。
- 玩家从邮筒取出信件后，运单改为“玩家承运中”。
- 潜行右键打开邮域配置：创建 1 到 3 个字母的邮域，或选择已有邮域。
- 绑定后按该邮域单独递增邮区号（1 到 99），例如 `ABC1`、`ABC2`。
- 未绑定邮域的邮筒只打开配置界面。

`post_box` 不编辑信件正文，也不签收。收信职责留给未来的 `mail_box` 和当面签收。

## 人工邮路流程

```text
玩家编辑草稿信件
-> 填写三级地址或玩家名并封蜡
-> 投入 post_box
-> 玩家从邮筒领取并实际携带
-> 交给收件人或未来的 mail_box 签收
```

## 待明确问题

- 收信邮箱 `postalCode` 如何申领，以及与 `mail_box` 方块如何解绑。
- 一个玩家是否可以拥有多个收信邮箱地址。
- 钥匙复制、重新配锁和旧钥匙失效的规则。
- 邮箱被破坏后，邮区/邮编是否保留。
- 当面签收的交互方式。
