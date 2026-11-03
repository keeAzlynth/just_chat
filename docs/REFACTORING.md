# 重构与性能路线图

本文记录这一轮做了什么、**代码按什么风格写**，以及还剩哪些地方值得重构（按收益排序）。

---

## 一、本轮完成（有实测数字）

| 项目 | 改前 | 改后 | 提升 |
|---|---|---|---|
| `formattedTime()`（每个气泡每帧） | 388.6 ns | **17.2 ns** | 22× |
| `formattedDate()`（每条消息每次列表重建） | 454.9 ns | **31.3 ns** | 14× |
| 500 条消息的跨天判断 | 243 µs | **6.7 µs** | 36× |
| `SmartReplyProvider` 每条消息的临时 List | 5 个 | **0 个** | — |
| `MessageDeduplicator` 判重 | LinkedHashMap(accessOrder) | HashSet + FIFO 环 | 少一次链表写 + 无 Entry 分配 |

具体改动：

1. **`data/TimeFormatter.kt`（新增）**：用整数运算做 `HH:mm` / `yyyy-MM-dd`。
   时区偏移按天缓存，遇到夏令时切换日自动退化为精确查询（一年两天）。
   年月日用 Howard Hinnant 的 civil-from-days，结果写进定长 `CharArray`。
2. **`ChatModels.formattedTime/formattedDate`**：改为委托给 `TimeFormatter`。
3. **`ChatListUtils.messagesToListItems`**：跨天判断从「格式化字符串再比较」改成
   `epochDay` 整数比较；单次遍历，`prev` 用局部变量带下来；结果列表预分配。
4. **`MessageDeduplicator`**：`LinkedHashMap(accessOrder=true)` → `HashSet` + `ArrayDeque`。
5. **`SmartReplyProvider`**：匹配词表从函数体提到 object 级 `Array`，遍历用下标；
   `emojiReplies + generalReplies.take(3)` 这类每次新建的列表改成预计算。
6. **`MessageDelegate.computeSmartSuggestions`**：建议没变化就不更新 `UiState`
   （一次 update 会让整棵状态树换新对象）。
7. **`ChatScreen`**：删掉外层 35 行「解构了但没用到」的回调。

---

## 二、代码风格：让编译器和 CPU 省力的写法

这些不是教条，是本项目实测下来收益明确的几条：

1. **热路径别用 `String.format`**。它要解析格式串、走 `Formatter`、装箱，单次 400 ns，
   而手写拼接只要 17 ns。`java.time` 的 `Instant`/`LocalDateTime` 同理，每次都在分配对象。
2. **词表、正则、格式串提到 `object` 级**。写在函数体里等于每次调用都 new 一遍
   （`MarkdownParser` 的正则、`SmartReplyProvider` 的词表都是这个问题）。
3. **定长输出用 `CharArray` 直接拼**，别用 `StringBuilder`：少一层扩容和内部数组。
4. **遍历数组用下标**，`forEach`/`any`/`firstOrNull` 会带 lambda 和迭代器分配。
5. **能比整数就别比字符串**：跨天判断用 `epochDay`，不要格式化日期再比。
6. **集合已知容量就预分配**，`ArrayList(n + n/8)` 省掉扩容时的数组拷贝。
7. **别用 access-order `LinkedHashMap` 当 LRU 热点**：每次 `get` 都写链表指针，
   命中路径的成本可能比被缓存的计算还高（Web 端的 Markdown 缓存就踩过）。
8. **`data class` 的 `equals` 会深比较 `List`/`Map`**。参与 Compose 跳过判断的模型
   里放大集合，等于每次重组都在做 O(n) 比较。
9. **不可变模型 + `copy()` 是必要的**，但别在每帧都 `copy()`；能在事件入口改一次就别在渲染层改。

---

## 三、第三轮：四项结构性重构（已完成）

### 1. UiState 拆分：先把「高频状态」挪出去

完整拆成 5 个独立 StateFlow 给每个界面订阅，在这个项目里**收益有限** ——
`ChatApp` 用 `when (currentScreen)` 只组合一个界面，跨界面失效本来就不存在。
真正的痛点是**同一个界面内的高频写入**：

- `draft` 曾经在 `ChatState` 里，每敲一个字就 `_uiState.update{}` 一次 → 整棵状态树换新对象，
  `ChatApp`／`ChatScreen`／`LazyColumn` 全部进入重组流程。
- 现在 `draft` 是独立的 `MutableStateFlow<String>`，只有 `InputBar` 在内部 `collectAsState()`。
  **每敲一个字的重新组合范围从「整屏」缩到「一个输入框」。**
- 顺带把草稿落盘改成 500 ms 防抖：原来是每个字符一次 `SharedPreferences.apply()`。
- 移除了 `ChatState.draft` / `canSend` 以及 `ChatUiState` 上的转发属性，避免两处真相。

### 2. `MessageBubble` 拆分

内容区按消息类型拆到 `MessageBubbleContent.kt`：

| Composable | 职责 |
|---|---|
| `RecalledContent` | 撤回提示 |
| `ImageMessageContent` | 图片 + 点击查看 |
| `FileMessageContent` | 文件卡片 |
| `TextMessageContent` | Markdown 文本 + 时间 + 链接预览 |

`MessageBubble` 现在只负责气泡外形、滑动回复、长按菜单和类型分发，536 行 → 480 行，
且每种类型各自是独立的重组作用域，以后加语音/投票卡片不用再改那个巨型函数。

### 3. 消息 id 索引

新增 `data/MessageIndex.kt`：`HashMap<String, Int>` 缓存 id→下标，
用 size 判断是否重建，命中后再校验一次 `list[i].id == id` —— 本地消息收到服务端回执时
id 会被替换（size 不变），靠这个校验自愈。

替换了 5 处 `messages.indexOfFirst { it.id == ... }`（编辑、加表情、撤回、回执、编辑事件），
500 条消息下从 500 次字符串比较降到一次哈希查找。

### 4. 未读数接上

之前 `MessageCache.incrementUnreadCount()` **没有任何调用方**，但 HomeScreen 和角标都在读
`unreadCounts`，所以永远是 0。现在：

- `MessageDelegate.onMessageArrived()` 在「消息不属于当前会话」时累加未读并发通知；
- `GroupDelegate.selectGroup/selectPrivateUser` 切换会话时清零；
- `MessageCache` 重写：内存 Map 为准，只在值变化时更新 StateFlow 和写单条 prefs，
  不再每次遍历全部 SharedPreferences 重建 Map；
- 顺带修了取消选中时 `currentChatId` 变成 `"group_"` 半截 id 的问题（改为回到 `public`）。

---

## 四、还剩的重构项（按收益排序）

### 1. `ChatActions` 40 个回调

已经聚合过一次，但仍是「一个大对象」。建议按区域拆 `MessageActions` / `GroupActions` /
`InputActions` / `NavActions`，或用 `CompositionLocal` 下发到深层组件。

### 2. `recordingSeconds` 还在 UiState 里

录音时每 100 ms 更新一次 `ui.recordingSeconds`，同样会让整棵树换对象。
建议和 `draft` 一样拆成独立 StateFlow，只在录音条那层订阅。

### 3. `DateSeparator` 里的 `SimpleDateFormat`

每个日期分隔符都要 new 两个 `SimpleDateFormat` 再 `parse`，可以直接用 `TimeFormatter` + `remember`。

### 4. 发送后的 `markDelivery` 仍是两次线性扫描

按文本匹配的兜底逻辑无法走索引。可以在发送时把待确认消息放进 `ArrayDeque`，
回执到达时从队头匹配，避免每次扫描全表。

### 5. 构建层面

- R8 已经开了 minify/shrink，可以再开 `android.enableR8.fullMode`。
- 加 Baseline Profile（Compose 首帧 + 冷启动）。

---

## 五、怎么验证

```bash
export JAVA_HOME="D:/Android_Studio/jbr"
./gradlew :app:testDebugUnitTest      # 41 个用例
./gradlew :app:assembleDebug
```

| 测试类 | 用例 | 覆盖 |
|---|---|---|
| `MarkdownParserTest` | 10 | 死循环回归、行内/块级语法、缓存、性能基线 |
| `ChatListUtilsTest` | 8 | 分组窗口、换人断开、跨天分隔 |
| `SmartReplyProviderTest` | 7 | 问候/问句/感谢/短消息/兜底 |
| `MessageIndexTest` | 6 | 命中/未命中、增删、id 替换、2000 条比对 |
| `TimeFormatterTest` | 5 | 与 java.time 随机比对、夏令时、跨时区、跨天 |
| `MessageDeduplicatorTest` | 5 | 判重、FIFO 淘汰、补记、容量 |
