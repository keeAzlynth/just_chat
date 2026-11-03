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

## 三、还需要重构的地方（按收益排序）

### 1. `UiState` 太大（影响最大）

`ChatUiState` 嵌了 auth/chat/ui/nav 四层，**任何字段变化都会让所有读它的 Composable 重组**。
一条消息进来会连续触发 2~3 次 `_uiState.update{}`。

建议：拆成 `messagesFlow` / `typingFlow` / `inputFlow` / `dialogFlow`，`ChatScreen` 只订阅需要的。
Compose 的 `derivedStateOf` 只能缓解，不能替代。

### 2. `MessageBubble.kt` 536 行

一个函数里塞了 text/image/file/recalled/引用/表情/多选/时间戳。建议按 `messageType`
拆成 `TextBubble` / `ImageBubble` / `FileBubble` / `RecalledBubble`，主函数只做分发和气泡外形。

### 3. `ChatActions` 40 个回调

已经聚合过一次，但仍是「一个大对象」：任何一个回调变化都会让 `ChatContent` 重组的输入变脏。
建议按区域拆 `MessageActions` / `GroupActions` / `InputActions` / `NavActions`，
或用 `CompositionLocal` 下发到深层组件，避免一路透传。

### 4. 消息按 id 查找是 O(n)

`saveEditMessage` / `addReaction` / `markDelivery` 都在 `messages.indexOfFirst { it.id == ... }`。
500 条消息就是 500 次字符串比较，而且每次读 `SnapshotStateList` 元素都会建立快照读取。

建议：维护 `HashMap<String, ChatMessage>` + 位置索引，或者让 id 与顺序一致后二分。

### 5. 未读数功能是断的

`MessageCache.incrementUnreadCount` / `saveUnreadCount` **没有任何调用方**；
而且它的实现是「每次遍历全部 SharedPreferences → 重建 Map → 更新 StateFlow」。
要么接上（改用内存 Map + 批量落盘），要么直接删掉，别留地雷。

### 6. `DateSeparator` 里的 `SimpleDateFormat`

每个日期分隔符都要 new 两个 `SimpleDateFormat` 再做 `parse`。
分隔符数量不多，但可以直接用 `TimeFormatter` + `remember` 算「今天/昨天/日期」。

### 7. `MessageCache.loadUnreadCounts` 全量重建

每次调用都 `prefs.all.forEach` 一遍并重建 Map。改成增量更新 + `apply()` 批量提交。

### 8. 构建层面

- R8 已经开了 minify/shrink，可以再开 `android.enableR8.fullMode`。
- 加 Baseline Profile（Compose 首帧 + 冷启动），首屏能再快一截。

---

## 四、怎么验证

```bash
export JAVA_HOME="D:/Android_Studio/jbr"
./gradlew :app:testDebugUnitTest      # 35 个用例
./gradlew :app:assembleDebug
```

测试分布：

| 测试类 | 用例 | 覆盖 |
|---|---|---|
| `MarkdownParserTest` | 10 | 死循环回归、行内/块级语法、缓存、性能基线 |
| `TimeFormatterTest` | 5 | 与 java.time 随机比对、夏令时、跨时区、跨天边界 |
| `ChatListUtilsTest` | 8 | 分组窗口、换人断开、跨天分隔 |
| `MessageDeduplicatorTest` | 5 | 判重、FIFO 淘汰、补记、容量 |
| `SmartReplyProviderTest` | 7 | 问候/问句/感谢/短消息/兜底 |
