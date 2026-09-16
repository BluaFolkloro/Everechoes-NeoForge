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

层级：

```text
PostalDomain
└─ PostalDistrict
   ├─ PostBoxNode（HUB 或 COLLECTION）
   └─ MailBoxEndpoint（尚未实现）
```

- `MailBoxAddress` 同时保存内部 UUID（`domainId` / `districtId` / `mailboxId`）和显示快照（`domainCode` / `districtCode` / `deliveryCode`）。
- `PlayerAddress(UUID playerId)`：当面交给玩家本人。
- 当前显示格式为暂定英式外码空格内码，例如 `EV12 7QF`。尚未最终定案。

显示规则（暂定）：

- `domainCode`：1 到 2 个字母。
- `districtCode`：1 到 2 位数字，可选一个末尾字母；自动分配暂时只用 1–99。
- `deliveryCode`：一位数字加两位英式内码字母（排除 C I K M O V）。
- 规范化输出带一个空格：`EV12 7QF`。
- `post_box` 只显示外码 `EV12`；完整地址属于未来的 `mail_box`。

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

## 邮域模型

邮域是自治的邮政路由集合，不属于玩家，也不等于维度、坐标区域或某个方块。

- 内部 `domainId` 是不可变 UUID，是归属关系的权威身份。
- `domainCode` 是地址里显示的 1 到 3 个字母，可以以后改名而不破坏内部引用。
- 数据存在服务器 SavedData，不依赖区块加载。
- 没有 owner UUID，也不使用印章、许可证或邀请物品作为加入凭证。
- GUI 只能替邮区发起建立/加入/退出请求；是否成功由服务端策略决定。

邮域生命周期：

- `ACTIVE`：至少有一个有效邮区。
- `DORMANT`：暂时没有有效邮区，且不保存虚假邮箱、库存或投递队列。
- `HISTORICAL`：只用于解析旧地址和邮戳。

邮区通过 `DomainMembership` 加入邮域：

- 状态：`PENDING` → `ACTIVE` → `LEAVING` → `DETACHED`。
- 加入和退出针对邮区，不针对玩家。
- 当前默认策略是开放加入；退出不能被永久禁止。
- 清算接口已预留。尚未实现 `mail_box`，因此不伪造自动投递队列。

当前信件地址仍使用 `MailBoxAddress.domainId` 作为显示用邮域代码，现有 `AAAXX-YYY` 解析成本很低，因此继续允许读取。该显示格式尚未最终定案。

开发版存档策略：`PostalNetwork` 只运行带 `schemaVersion` 的新模型。更早的邮域表在读取时一次性丢弃并改写为新格式，不保留旧代码到内部 ID 的别名。旧邮筒库存保留，但旧邮域绑定作废，加载后回到未加入状态。不扫描、不强制加载未加载区块。

## 发信邮筒职责

`post_box` 是承运交接口，不是自动传送机：

- 只接受带有效 `LetterData` 的 `sealed_letter`。
- 仅在邮区 `ACTIVE` 成员资格下接收新邮件。
- 暂存待领取信件，并把运单标为“待承运人领取”。
- 玩家从邮筒取出信件后，运单改为“玩家承运中”。
- 潜行右键打开入网界面：为该邮区建立邮域、申请加入或申请退出。
- 未加入有效邮域的邮筒只打开入网界面。

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
