# Everechoes

Everechoes 是一个基于 NeoForge 的 Minecraft 模组项目，目前面向 Minecraft 1.21.1 开发。

模组当前处于早期开发阶段，核心方向是围绕信件、邮筒与装饰手办构建一套带有叙事感的交互内容。现阶段已有基础物品、方块、资源与占位 GUI，完整的邮件系统仍在设计和实现中。

## 当前内容

### 信件物品

- `everechoes:sealed_letter`：封蜡信件
- `everechoes:letter`：信件
- `everechoes:opened_letter`：拆封的信件

这些物品目前已完成注册、模型、纹理和语言资源。具体的写信、封蜡、拆信、投递等行为尚未实现。

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
├─ item
│  ├─ BirdFigureBlockItems.java
│  ├─ ContainerBlockItems.java
│  ├─ LetterItems.java
│  └─ ModCreativeModeTabs.java
├─ menu
│  ├─ PostBoxMenu.java
│  └─ ModMenuTypes.java
├─ postal
│  ├─ Address.java
│  ├─ MailBoxAddress.java
│  └─ PlayerAddress.java
├─ screen
│  └─ PostBoxScreen.java
├─ Config.java
├─ Everechoes.java
└─ EverechoesClient.java
```

各层职责：

- `Everechoes`：模组入口，负责注册物品、方块、方块实体、菜单和创造模式标签页。
- `EverechoesClient`：客户端事件入口，当前用于注册邮筒界面。
- `block`：方块定义与方块注册，包括邮筒和夜鹭手办。
- `block.entity`：方块实体定义与注册，目前用于邮筒储存。
- `item`：物品注册、方块物品注册和创造模式标签页。
- `menu`：服务端容器菜单定义与菜单类型注册。
- `screen`：客户端 GUI 界面。
- `postal`：邮件系统的基础地址模型，目前包含邮筒地址和玩家地址。
- `Config`：当前仍是 NeoForge MDK 示例配置，后续需要按项目实际需求清理或改造。

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

已完成的是基础内容注册、资源接入和部分方块行为。后续重点包括：

- `LetterData` 数据结构
- 信件编辑界面
- 信件封蜡与拆封状态转换
- 邮筒实际投递逻辑
- 多人游戏下的收件人与投递数据同步
- 邮筒 GUI 正式设计

## 许可证

本项目使用 MIT License。
