# Everechoes

Everechoes 是一个基于 NeoForge 的 Minecraft 模组项目，目前面向 Minecraft 1.21.1 开发。

模组当前处于早期开发阶段，核心方向是围绕信件、邮筒与装饰手办构建一套带有叙事感的交互内容。现阶段已有基础物品、方块、资源与占位 GUI，完整的邮件系统仍在设计和实现中。

## 当前内容

### 信件物品

- `everechoes:sealed_letter`：封蜡信件
- `everechoes:letter`：信件
- `everechoes:opened_letter`：拆封的信件

这些物品目前已完成注册、模型、纹理、语言资源和信件数据组件。玩家可以写信、封蜡和拆封；数据会随物品保存并同步。邮筒投递尚未实现。

### 邮筒

- `everechoes:post_box`

邮筒是一个双格方块，下半部分持有方块实体和 27 格占位容器。玩家右键邮筒可打开当前占位 GUI。

当前已实现：

- 双格放置与联动移除
- 放置方向
- 下半部分持有储存数据
- 上半部分点击时打开下半部分菜单
- 破坏时掉落容器内容
- 邮筒本体由下半部分统一负责掉落
- 中文和英文语言资源

当前邮筒 GUI 和容器逻辑仍是占位实现，后续会替换为实际邮件系统。

### 夜鹭手办

- `everechoes:night_heron_figure`
- `everechoes:night_heron_cockroach_figure`
- `everechoes:night_heron_thoughtful_figure`

当前已实现：

- 方块、方块物品和创造模式标签页注册
- 方块模型、物品模型、纹理和语言资源
- 放置方向
- 适配模型尺寸的碰撞箱/选中框

## 邮件系统设计方向

Everechoes 的邮件不会在容器之间自动传送。每封邮件都是必须由玩家、信鸽或未来可能加入的邮递 NPC 实际携带、交接并签收的物品。邮政系统负责寻址、权限、路由和责任记录，不凭空移动邮件。

### 三级地址

邮箱地址采用“邮域—邮区—邮编”三级结构：

```text
Postal Domain / Postal District / Postal Code
邮域          / 邮区            / 邮编
```

- 邮域是邮政网络的最高级命名空间，不依赖某个必须加载的方块存在。
- `post_box` 定义邮区，是投信、分拣和承运交接的节点。
- `mail_box` 定义邮编，是具体收信端点。

玩家不直接拥有 `mail_box`，而是通过钥匙获得访问权限。邮编申领权与邮箱方块绑定关系分离，使玩家可以保留喜欢的邮编，而不必在邮政网络中留下虚假邮箱、占位库存或幽灵投递端点。

### 玩家地址与默认邮箱

`PlayerAddress` 表示当面交给玩家本人：邮件不会自动进入背包，玩家必须与承运者交互并签收。

玩家可以声明一个邮箱为默认收件地址。信鸽处理 `PlayerAddress` 时优先改投默认邮箱；只有没有默认邮箱或信件要求本人签收时，才尝试寻找玩家的位置。

### 实体承运与签收

预期流程为：

```text
编辑草稿
-> 填写地址并封蜡
-> 投入 post_box
-> 玩家或信鸽领取
-> 人工携带或逻辑旅行
-> 玩家或 mail_box 签收
-> 失败时退回 returnAddress，或滞留在实际中转站
```

投递既不是即时完成，也不是脱离承运者的自动队列。邮件在任何时刻都应有明确保管人或实际存放节点。

- 玩家承运时不限制距离和维度。
- 信鸽承运能力取决于自身属性和升级。
- 末影升级可以提供跨维度能力，也可以允许玩家为信鸽登记传送门路线。
- 邮箱被破坏后的邮编迁移和保留规则，将与邮编申领机制一起确定。

### 信鸽性能原则

信鸽不应为了飞行而持续加载沿途区块。建议采用实体表现与逻辑旅行结合的方式：

- 在玩家附近、起飞、抵达和签收时表现为实体。
- 离开活动区域后转为持久化旅程记录，根据路线、距离和信鸽属性计算下一事件时间。
- 目的地区块未加载时保持逻辑抵达状态，不主动加载区块；区块加载后再实体化完成交付。
- 大量旅程按到期时间调度，不逐 tick 扫描或模拟路径。

### 邮票

邮政系统不计划加入强制邮费或货币消耗。邮票仍可作为可选收藏内容，用于邮域发行物、节日系列、路线纪念、邮戳和实寄封收藏，而不是投递门槛。

## 开发环境

- Minecraft：`1.21.1`
- NeoForge：`21.1.214`
- Java：`21`
- Gradle：使用项目自带 Gradle Wrapper

## 文档

更详细的设计和开发记录位于 `docs/`：

- [开发路线和计划](docs/roadmap.md)
- [邮政系统架构](docs/postal-design.md)
- [代码机制笔记](docs/implementation-notes.md)

## 项目结构

主要源码位于：

```text
src/main/java/net/bluafolkloro/overdeterminism/everechoes
```

当前 Java 包结构：

```text
everechoes
├─ block
│  ├─ entity
│  │  ├─ PostBoxBlockEntity.java
│  │  └─ ModBlockEntities.java
│  ├─ BirdFigureBlock.java
│  ├─ BirdFigureBlocks.java
│  ├─ ContainerBlocks.java
│  └─ PostBoxBlock.java
├─ component
│  └─ ModDataComponents.java
├─ item
│  ├─ BirdFigureBlockItems.java
│  ├─ ContainerBlockItems.java
│  ├─ LetterItem.java
│  ├─ LetterItems.java
│  └─ ModCreativeModeTabs.java
├─ letter
│  ├─ LetterData.java
│  ├─ LetterDataSerializer.java
│  └─ LetterState.java
├─ menu
│  ├─ LetterMenu.java
│  ├─ PostBoxMenu.java
│  └─ ModMenuTypes.java
├─ network
│  ├─ LetterActionPayload.java
│  └─ ModNetworking.java
├─ postal
│  ├─ Address.java
│  ├─ MailBoxAddress.java
│  └─ PlayerAddress.java
├─ screen
│  ├─ LetterScreen.java
│  └─ PostBoxScreen.java
├─ Everechoes.java
└─ EverechoesClient.java
```

各层职责：

- `Everechoes`：模组入口，负责注册物品、方块、方块实体、菜单、数据组件、网络包和创造模式标签页。
- `EverechoesClient`：客户端事件入口，当前用于注册邮筒界面和信件界面。
- `block`：方块定义与方块注册，包括邮筒和夜鹭手办。
- `block.entity`：方块实体定义与注册，目前用于邮筒储存。
- `component`：信件 Data Component 注册。
- `item`：物品注册、方块物品注册和创造模式标签页。
- `letter`：信件数据、状态机、序列化和服务端写信逻辑。
- `menu`：服务端容器菜单定义与菜单类型注册。
- `network`：自定义网络包。
- `screen`：客户端 GUI 界面。
- `postal`：邮件系统的基础地址模型，目前包含邮箱地址和玩家地址。

主要资源位于：

```text
src/main/resources
├─ assets/everechoes
│  ├─ blockstates
│  ├─ lang
│  ├─ models
│  │  ├─ block
│  │  └─ item
│  └─ textures
│     ├─ block
│     └─ item
├─ data/everechoes
│  └─ loot_table
│     └─ blocks
└─ ...

src/main/templates
└─ META-INF
   └─ neoforge.mods.toml
```

资源层级说明：

- `assets/everechoes/blockstates`：方块状态与模型旋转配置。
- `assets/everechoes/models/block`：方块模型。
- `assets/everechoes/models/item`：物品模型。
- `assets/everechoes/textures`：方块和物品贴图。
- `assets/everechoes/lang`：中英文语言文件。
- `data/everechoes/loot_table/blocks`：方块掉落表。
- `src/main/templates/META-INF/neoforge.mods.toml`：模组元数据模板，由 Gradle 生成最终资源。

## 常用命令

编译 Java：

```bash
./gradlew compileJava
```

构建模组：

```bash
./gradlew build
```

处理资源：

```bash
./gradlew processResources
```

运行客户端：

```bash
./gradlew runClient
```

在 Windows PowerShell 中可以使用：

```powershell
.\gradlew.bat build
```

## 项目状态

当前版本：`0.0.1`

已完成的是基础内容注册、资源接入、部分方块行为，以及本地信件闭环：`LetterData` 作为 Data Component 挂在三种信件物品上，玩家可以写信、封蜡、拆封，数据可保存并同步。后续重点包括：

- “邮域—邮区—邮编”注册与寻址
- 邮筒实际投递逻辑
- 玩家、钥匙、信鸽和收信邮箱的交接与签收
- 多人游戏下的收件人与投递数据同步
- 无沿途区块加载的信鸽逻辑旅行

## 许可证

本项目使用 MIT License。
