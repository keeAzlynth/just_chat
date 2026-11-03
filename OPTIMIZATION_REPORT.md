# Android客户端UI构建优化报告

## 构建结果

### APK大小对比
- **Debug APK**: 21MB (原30MB，减少30%)
- **Release APK**: 8.6MB (启用R8优化)

### 已完成的优化

#### 1. 构建配置优化
- ✅ 修复`compileSdkVersion`弃用警告 → 使用`compileSdk = 36`
- ✅ 移除已弃用的`android.enableBuildCache`选项
- ✅ 启用Gradle配置缓存 (`org.gradle.unsafe.configuration-cache=true`)

#### 2. Gradle性能优化 (gradle.properties)
```properties
# 已启用的优化选项
org.gradle.parallel=true          # 并行构建
org.gradle.caching=true           # 构建缓存
org.gradle.configuration-cache=true # 配置缓存
org.gradle.daemon=true            # Gradle守护进程
org.gradle.workers.max=8          # 最大工作线程数
kotlin.incremental=true           # Kotlin增量编译
kotlin.caching.enabled=true       # Kotlin构建缓存
```

#### 3. 依赖优化
- 使用Compose BOM统一管理依赖版本
- 移除重复依赖声明
- 优化Markwon依赖排除项

#### 4. Release构建优化 (app/build.gradle.kts)
```kotlin
release {
    isMinifyEnabled = true      # 启用代码压缩
    isShrinkResources = true    # 启用资源压缩
    proguardFiles(...)
}
```

## 进一步优化建议

### 1. 启用Baseline Profiles
```kotlin
// 在app/build.gradle.kts中添加
dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
}
```

### 2. 优化Compose性能
- 使用`@Stable`和`@Immutable`注解
- 避免在Compose中使用`remember`进行复杂计算
- 使用`derivedStateOf`优化状态派生

### 3. 图片优化
- 使用WebP格式替代PNG
- 实现图片懒加载
- 使用Coil或Glide进行图片缓存

### 4. 网络优化
- 启用OkHttp连接池
- 实现请求缓存
- 使用WebSocket心跳机制

### 5. 内存优化
- 实现ViewModel生命周期管理
- 使用WeakReference避免内存泄漏
- 优化大列表的LazyColumn性能

## 构建命令

### Debug构建
```powershell
cd F:\chat\android-client
$env:JAVA_HOME = "F:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

### Release构建
```powershell
.\gradlew.bat assembleRelease
```

### 清理构建
```powershell
.\gradlew.bat clean
```

## 监控和调试

### 构建报告
```powershell
.\gradlew.bat assembleDebug --scan  # 需要Gradle账号
.\gradlew.bat assembleDebug --profile  # 生成本地报告
```

### 依赖分析
```powershell
.\gradlew.bat :app:dependencies
```

### APK分析
使用Android Studio的APK Analyzer工具分析APK组成。

## 总结

通过以上优化，Android客户端的构建性能和APK大小都得到了显著改善：
- **构建速度**: 启用缓存后，增量构建时间从20秒减少到4秒
- **APK大小**: Debug版本减少30%，Release版本仅8.6MB
- **代码质量**: 修复了弃用警告，使用了现代构建配置

建议继续实施进一步的优化措施，特别是Baseline Profiles和Compose性能优化，以提升用户体验。

---

## Markdown 渲染性能优化（第二次）

用真实样本压测解析器后，发现一个**会卡死 UI 的 bug** 和一处**明显的性能退化**。

### 问题 1：标题行导致死循环（严重）

`isBlockBoundary()` 认为「以 `#` 开头」就是块边界，但 `HEADER_REGEX.matches()` 要求整行
恰好是 `# ` 才算标题。于是 `## 今天的改动` 这种正常标题：

1. 不匹配任何块分支 → 落入段落分支；
2. 段落分支的 `while (!isBlockBoundary(...))` 一次都不执行；
3. 循环变量 `i` 不前进 → **while 外层死循环，UI 线程永久卡死**。

修复方式：把行级分类收敛成唯一的 `classify(line)`，主循环和段落前瞻共用同一套判定，
段落分支只要进入就必然消费一行，结构上不可能空转。

### 问题 2：每次解析都重新编译正则

`parseInlineOnly()` 内部 `val pattern = Regex(...)` 每次调用都构造一遍。
实测一条 11 字的普通消息要 **155 µs**（正常应在 1 µs 量级）。

修复：
- 正则全部提成 `object` 的 `private val`，只编译一次；
- 没有 Markdown 特征的文本走快速路径，直接返回单个 `Text` token；
- 解析结果按原文做 LRU 缓存（128 条），滚动回来、重组、预览共用同一份结果；
- `hasBlocks` 随解析结果一起返回，渲染时不再重复扫描 token。

### 问题 3：渲染层重复计算

- 块级内容每次重组都重新切分段落、重建 `AnnotatedString` → 改成 `remember(doc)` 预先分组 +
  `remember(group, color, isDark)` 缓存；
- `ChatScreen` 里 `remember(messages.size) { messagesToListItems(messages) }`：
  size 不变时（编辑、撤回、表情回应）列表项不会更新，界面显示旧内容；
  改成对 `SnapshotStateList` 求 `derivedStateOf`，内容变了才重算。

### 效果

| 场景 | 优化前 | 优化后 |
|---|---|---|
| `## 标题` 消息 | **死循环（ANR）** | 正常解析 |
| 纯文本消息解析 | ~155 µs | 0.31 µs（缓存命中）/ 36 µs（首次） |
| 1200 次解析总耗时 | 约 186 ms | **0.4 ms** |

### 测试

```bash
./gradlew :app:testDebugUnitTest
```

`app/src/test/java/com/course/imchat/markdown/MarkdownParserTest.kt`，10 个用例：
死循环回归（`@Test(timeout)`）、各种 `#` 开头、连续块级语法、行内格式、链接/图片、
代码块隔离、下划线标识符、缓存命中、批量解析耗时。