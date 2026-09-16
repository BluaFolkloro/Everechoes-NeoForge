# 代码机制笔记

本文档记录当前实现中的关键机制和注意事项。

## 注册入口

模组入口是 `Everechoes`。

当前注册内容包括：

- 创造模式标签页：`ModCreativeModeTabs`
- 数据组件：`ModDataComponents`
- 方块实体：`ModBlockEntities`
- 菜单类型：`ModMenuTypes`
- 信件物品：`LetterItems`
- 容器方块与方块物品：`ContainerBlocks`、`ContainerBlockItems`
- 夜鹭手办方块与方块物品：`BirdFigureBlocks`、`BirdFigureBlockItems`

客户端入口是 `EverechoesClient`，当前用于注册 `PostBoxScreen` 和 `LetterScreen`。

## 发信邮筒双格方块

`PostBoxBlock` 是双格方块：

- 下半部分持有 `PostBoxBlockEntity`。
- 上半部分没有方块实体。
- 点击上半部分时，会映射到下半部分打开菜单。
- 发信邮筒本体掉落由下半部分负责。
- 内部物品掉落也只由下半部分负责。

注意：双格方块破坏逻辑需要避免上下半重复掉落，也要避免创造模式破坏时通过邻居更新绕过掉落抑制。

## 发信邮筒容器和 GUI

当前 `post_box` 储存和 GUI 是占位实现：

- `PostBoxBlockEntity` 使用 27 格 `SimpleContainer`。
- `PostBoxMenu` 使用 3x9 发信邮筒槽位和玩家背包槽位。
- `PostBoxScreen` 暂时使用原版潜影盒背景。

这些机制后续会被实际邮件系统替换或重构。

## 夜鹭手办

`BirdFigureBlock` 为夜鹭手办提供：

- `FACING` 朝向属性。
- 放置时朝向玩家。
- 随朝向旋转的选中框和碰撞箱。

三个手办共享 `BirdFigureBlock`，但注册时传入不同的 `VoxelShape`，以适配不同模型尺寸。

## 信件数据模型

`LetterData` 是不可变值对象，作为信件物品的 Data Component 存储。

设计要点：

- `letterId` 创建后不变。
- 只有草稿状态可以编辑；编辑方法返回新的 `LetterData`。
- 状态转换是单向的：`DRAFT -> SEALED -> OPENED`。
- 蜡封和拆封状态必须有收件地址。
- `signatureSender` 和 `letterRecipient` 内部允许为 `null`。
- 对外读取可选文本时返回 `Optional`。
- 空白可选文本会被规范化为 `null`。
- Data Component 使用 `LetterDataSerializer.CODEC` 持久化，并使用 `STREAM_CODEC` 同步。

三种信件物品共享同一组件类型。右键草稿会自动生成 `letterId`，并把退回地址设为当前玩家。封蜡和拆封会转换物品类型，但保留同一份 `letterId` 和信件内容。

## 信件交互

右键信件会打开 `LetterMenu` / `LetterScreen`。

- 草稿：可编辑标题、正文、称呼、落款和收件地址，关闭界面时保存。
- 收件地址支持邮箱邮编或玩家名；玩家名在服务端解析。
- 封蜡会把 `letter` 转换成 `sealed_letter`，并保留同一 `letterId`。
- 拆封会把 `sealed_letter` 转换成 `opened_letter`。
- 蜡封和拆封信件打开只读界面。
- 服务端通过 `LetterActionPayload` 校验：菜单仍打开、物品仍在对应手里、仍是同一封信，并且草稿才能编辑。

常用验证命令还包括：

```bash
./gradlew test
```

## 地址模型

`Address` 是 sealed interface，目前有两个实现：

- `MailBoxAddress`
- `PlayerAddress`

`MailBoxAddress` 要求 `postalCode` 非空且非空白。

`MailBoxAddress` 表示未来收信端 `mail_box` 的地址，不表示当前负责发信的 `post_box`。

`PlayerAddress` 要求 `playerId` 非空。

## 构建和资源

常用验证命令：

```bash
./gradlew compileJava
./gradlew build
./gradlew processResources
```

资源文件改动后，如果只运行已打开的客户端，可能会看到旧资源。需要重新处理资源并重启客户端。
