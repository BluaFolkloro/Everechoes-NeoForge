# 代码机制笔记

本文档记录当前实现中的关键机制和注意事项。

## 注册入口

模组入口是 `Everechoes`。

当前注册内容包括：

- 创造模式标签页：`ModCreativeModeTabs`
- 方块实体：`ModBlockEntities`
- 菜单类型：`ModMenuTypes`
- 信件物品：`LetterItems`
- 容器方块与方块物品：`ContainerBlocks`、`ContainerBlockItems`
- 夜鹭手办方块与方块物品：`BirdFigureBlocks`、`BirdFigureBlockItems`

客户端入口是 `EverechoesClient`，当前用于注册 `MailBoxScreen`。

## 邮筒双格方块

`MailBoxBlock` 是双格方块：

- 下半部分持有 `MailBoxBlockEntity`。
- 上半部分没有方块实体。
- 点击上半部分时，会映射到下半部分打开菜单。
- 邮筒本体掉落由下半部分负责。
- 内部物品掉落也只由下半部分负责。

注意：双格方块破坏逻辑需要避免上下半重复掉落，也要避免创造模式破坏时通过邻居更新绕过掉落抑制。

## 邮筒容器和 GUI

当前邮筒储存和 GUI 是占位实现：

- `MailBoxBlockEntity` 使用 27 格 `SimpleContainer`。
- `MailBoxMenu` 使用 3x9 邮筒槽位和玩家背包槽位。
- `MailBoxScreen` 暂时使用原版潜影盒背景。

这些机制后续会被实际邮件系统替换或重构。

## 夜鹭手办

`BirdFigureBlock` 为夜鹭手办提供：

- `FACING` 朝向属性。
- 放置时朝向玩家。
- 随朝向旋转的选中框和碰撞箱。

三个手办共享 `BirdFigureBlock`，但注册时传入不同的 `VoxelShape`，以适配不同模型尺寸。

## 信件数据模型

`LetterData` 是可变类，用于表达信件生命周期和内容。

设计要点：

- `letterId` 创建后不变。
- 只有草稿状态可以编辑。
- 蜡封和拆封状态必须有收件地址。
- `signatureSender` 和 `letterRecipient` 内部允许为 `null`。
- 对外读取可选文本时返回 `Optional`。
- 空白可选文本会被规范化为 `null`。

后续接入 Data Component 时，需要注意 `LetterData` 是可变对象。存入组件或同步网络包时可能需要复制或序列化，避免共享引用造成状态不一致。

## 地址模型

`Address` 是 sealed interface，目前有两个实现：

- `LetterBoxAddress`
- `PlayerAddress`

`LetterBoxAddress` 要求 `postalCode` 非空且非空白。

`PlayerAddress` 要求 `playerId` 非空。

## 构建和资源

常用验证命令：

```bash
./gradlew compileJava
./gradlew build
./gradlew processResources
```

资源文件改动后，如果只运行已打开的客户端，可能会看到旧资源。需要重新处理资源并重启客户端。

