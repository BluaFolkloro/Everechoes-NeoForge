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

## 收信邮箱方块

`MailBoxBlock` 依安装样式使用一格或两格：

- 方块状态：`facing`（水平）+ `style`（立柱、平放、墙挂、普通悬挂、抬高悬挂、内置横链悬挂）+ `half` + `chain_axis`（内置横链方向）。
- `POST` 使用上下两格，恢复原始模型比例；其余安装样式只使用 `LOWER` 单格状态。
- `POST` 仅在初次放置时检查下方稳固面；放置完成后移除底座不会触发自毁，但上下两半仍必须保持配对。
- 放置：普通点击上表面 → `POST`；潜行点击上表面 → `DESK`；点击侧面 → `WALL`；直接点击竖直铁链下表面 → `HANGING`。
- `DESK` 额外允许放在栅栏、墙和竖直末地烛顶部，放好后即使移除支撑也不会消失。
- 悬挂式无需潜行，要求上方保持竖向/横向铁链、`HANGING_CHAINED` 复合邮箱或横向末地烛；横向支点使用 `HANGING_RAISED` 内部状态，将模型上移以接触支点。邮箱的水平朝向由玩家放置方向决定，与横向末地烛或铁链的轴向正交/平行均合法。玩家可对抬高悬挂邮箱的水平侧面使用一条锁链，将其转换为 `HANGING_CHAINED`：邮箱与横链通过 multipart 同格渲染，下方邮箱无需潜行即可把该状态视为横链支点；拆除复合方块时额外返还锁链。
- `getCollisionShape` 与 `getShape` 分离。遮挡/视觉形状为空。
- `MailBoxBlockEntity` 持久化 `mailboxId`。服务端 `onLoad` 调用 `PostalNetwork.registerEndpoint`；不是 `MenuProvider`，BE 内不调用 `bindEndpoint`。
- `MailBoxBlock.onRemove`（LOWER）调用 `markEndpointOffline`。已绑定端点转为 OFFLINE 并保留投递码；未绑定记录删除。
- `mailboxId` 由服务端在首次 load/save 时分配；客户端不铸造。未来客户端读取来自菜单或数据包。
- 绑定必须显式传入 `districtId`，不从 `districtsAt` 猜测。区内投递码由 `DeliveryCodeAllocator` 按 `PostalCodes.deliveryAt` 分配，容量 4000。
- 仅 `LOWER` 状态掉落 `everechoes:mail_box`，避免双格立柱重复掉落。

## 发信邮筒容器和 GUI

`post_box` 是 5 格承运交接口：

- 只接受已封蜡且带收件地址的信件。
- 放入时写入 `Waybill`（待承运人领取）。
- 玩家取出时把 `Waybill` 改为玩家承运。
- 每个邮筒有稳定的内部 `nodeId`；多个节点可共享一个 `districtId`。是否入网由 SavedData 中的 `DomainMembership` 决定。
- `DistrictCoverage` 保存邮区的连续区块集合；新邮区从主邮筒所在区块开始，附属邮筒必须位于辖区内。区块可被多个邮区同时覆盖；反向索引是 `chunk → Set<districtId>` 缓存，可从辖区重建。
- `PostalNetwork` 建立区块到邮区的空间索引，并从有效邮区实时派生邮域范围；邮域不保存重复辖区。
- 不统计玩家探索过的区块。旧存档里的 `exploredChunks` 加载时忽略，保存时不再写出。
- `PostalAtlasLimits`：默认窗口为 `25×19`，单边上限 `25`，`MAX_WINDOW_CELLS=475`，`MAX_SUBMIT_CHUNKS=DistrictCoverage.MAX_CHUNKS`（4096）。合法区块坐标为 `MIN_CHUNK..MAX_CHUNK`（由 `Level.MAX_LEVEL_SIZE` 换算，避免 chunk×16 溢出）；不使用动态世界边界。
- `PostalNetwork.atlasWindow` 返回 `PostalAtlasSnapshot`（无探索集合）；非法窗口或未知邮区为 `Optional.empty()`。`includeSavedCoverage=false` 时 `savedCoveragePacked` 为空，但 `savedCoverageSize` 仍是完整辖区大小。`nodePacked` 含本邮区全部节点区块；`hubPacked` / `collectionPacked` 按窗口裁剪。打开、平移、提交不调用 `getChunk`、不生成区块。
- `postal_atlas` 物品对已加入邮区的邮筒 `useOn` 打开 `PostalAtlasMenu`。C2S `postal_atlas_request`（PAN/SUBMIT）不信任客户端 `districtId`；PAN 携带受服务器上限校验的窗口宽高，提交集合在解码时若 `count` 超过上限会抛 `DecoderException`。S2C `postal_atlas_sync` 推送窗口或提交结果。屏幕绘制 25×19 横向坐标网格，格子只表示邮政状态；地图与右侧邮政工具面板使用独立响应式布局。
- `PostalNetwork` 使用 `schemaVersion`。当前为 **6**；版本 5 会显式迁移，并兼容其缺失或开发期附加的 `mailboxes` 列表。半绑定（district XOR code）跳过；双字段但非法投递码按未绑定加载。加载后会校验邮区引用、在线邮箱辖区位置和区内投递码唯一性；在线坏绑定降级为未绑定，离线坏绑定删除。遇到其他无法识别的版本时保留原始 NBT，并以只读降级状态运行，避免自动保存把旧网络覆盖为空。旧邮筒库存继续保留。
- 信箱 UUID 是路由身份，显示邮编只是信件上的快照。已解析地址不会按显示码重新寻址；复制出相同 UUID 的方块实体时，后出现的副本取得新 UUID，原端点的位置、绑定和投递码保持不变。
- 潜行右键打开入网界面，提交建立/加入/退出请求。
- 仅 `ACTIVE` 邮区接收新邮件；标题显示当前 `domainCode` 与邮区号。
- GUI 暂用原版漏斗背景；正式美化需先预览再改。
- 旧世界 27 格内容加载时，可投递信件填入前 5 格，其余掉落。

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
- 服务端通过 `LetterActionPayload` 校验：物品仍在对应手里、仍是同一封信，并且草稿才能编辑。关闭写信界面时保存包可能晚于关菜单包，因此不要求菜单仍开着。

常用验证命令还包括：

```bash
./gradlew test
```

## 地址模型

`Address` 是 sealed interface，目前有两个实现：

- `MailBoxAddress`
- `PlayerAddress`

`MailBoxAddress` 分开保存内部 UUID 与显示快照。当前显示格式为暂定的 `EV12 7QF`。旧的 `AAAXX-YYY` 不再猜测转换，避免多个旧地址被错误合并。邮筒只拥有外码；完整地址属于未来的收件端。

`MailBoxAddress` 表示未来收信端 `mail_box` 的地址，不表示当前负责发信的 `post_box`。

`Waybill` 是独立运输组件，不保存信件正文。

`PlayerAddress` 要求 `playerId` 非空。

## 构建和资源

常用验证命令：

```bash
./gradlew compileJava
./gradlew build
./gradlew processResources
```

资源文件改动后，如果只运行已打开的客户端，可能会看到旧资源。需要重新处理资源并重启客户端。
