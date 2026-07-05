# Just Chat — Android IM Client

仿 Telegram / QQ 风格的即时通讯 Android 客户端，纯 Kotlin + Jetpack Compose 实现。

> 本仓库仅包含 Android 前端代码。后端为独立 C++ 项目（Boost.Beast + SQLite3）。

## 功能特性

### 核心通讯
- WebSocket 实时通讯，支持断线重连 + 会话恢复
- 三级消息缓存（磁盘 L1 → 内存 L2 → 网络 L3），切换聊天秒开
- 公共聊天室 / 私聊 / 群组三种模式
- 消息已读回执（Telegram 风格 ✓✓）
- 在线状态同步 + 对方正在输入中
- 毛玻璃效果底部栏（API 31+ 原生 RenderEffect 模糊）

### 消息体验
- **Markdown 渲染** — 纯 Compose 自定义解析器，粗体/斜体/删除线/代码块/标题/`内联代码`/链接 + @提及高亮，**同时生效不互斥**
- **消息分组** — 同一人 5 分钟内连续消息合并显示（隐藏重复头像+昵称，气泡圆角自适应）
- **图片消息** — 点击全屏预览（Coil 加载 + pinch-to-zoom + 双击缩放 + 缓存）
- **文件消息** — 显示文件名和大小
- **编辑/撤回/删除** — 长按操作菜单
- **引用回复** — 双击气泡快捷引用
- **消息转发** — 单条/批量多选
- **消息置顶 + 收藏** — Pin/Unpin + 收藏夹管理页
- **Emoji 回应** — 长按选择表情反应
- **表情面板** — 8 类分类 + 最近使用置顶
- **URL 链接预览卡片** — 自动检测消息中链接

### 交互细节
- 发送触觉反馈
- 智能自动滚动（阅历史不跳底）
- 空状态引导页
- 输入框草稿自动保存/恢复
- 输入框 @ 提及自动补全
- 剪贴板粘贴按钮（有内容时显示）
- 字数计数（>180 字显示 x/500，超 450 变红）
- 语音录制按钮（长按录音 + 左滑取消）

### 页面结构
- **消息** — 聊天列表 + 未读角标 + 下拉刷新
- **收藏** — 独立收藏管理页（浏览/取消/跳转原聊天）
- **通讯录** — 在线/离线分组 + 搜索
- **我的** — 个人统计 + 收藏入口 + 设置 + 退出登录
- **设置** — 深色模式 + 缓存管理 + 版本信息

## 截屏

| 消息列表 | 聊天界面 | 收藏管理 | 个人主页 |
|----------|----------|----------|----------|
| ![](screen.png) | — | — | — |

## 技术栈

| 层 | 技术 |
|----|------|
| UI | Jetpack Compose + Material 3 |
| 主题 | MaterialKolor（种子色 `#3390EC` → HCT 科学自动生成 40+ 色值） |
| 状态管理 | StateFlow + SnapshotStateList |
| 图片加载 | Coil 3.2（三级缓存：内存→磁盘→网络） |
| 网络 | OkHttp 5.4 WebSocket |
| 序列化 | Gson + kotlinx.serialization（渐进迁移） |
| 本地存储 | SharedPreferences + DataStore（渐进迁移） |
| 时间处理 | kotlinx-datetime |
| UI 增强 | compose-shimmer-skeleton + compose-swipebox |
| 构建 | Gradle 9.1 + Kotlin 2.2 |

## 架构

```
app/src/main/java/com/course/imchat/
├── core/delegate/        # Auth / Message / Group / Pin / Connection（委托模式）
├── core/notification/    # 通知辅助
├── core/util/            # ComposeStability / ValueClasses
├── data/
│   ├── cache/            # AppCache(LRU) / LruCache / PersistentCache / SessionCache
│   ├── EmojiData         # 8 类表情 + 最近使用记录
│   ├── MessageRepository # 消息仓库
│   ├── WebSocketClient   # WebSocket 客户端
│   └── IncomingEvent     # 服务端事件模型
├── domain/               # 领域模型 + Repository 接口 + UseCase
├── presentation/         # ViewModel
├── ui/
│   ├── ChatScreen.kt     # 聊天主界面（ChatActions 统一回调，40+ params → 3）
│   ├── ChatApp.kt        # 导航容器（底部三 Tab）
│   ├── AuthScreen.kt     # 登录/注册
│   ├── Colors.kt         # 自定义色（气泡/头像等，通用色由 MaterialKolor 管理）
│   ├── GlassEffect.kt    # frostedGlass() / glassSurface() Modifier 扩展
│   ├── components/
│   │   ├── chat/         # 输入栏/气泡/表情/提及/背景/置顶栏/搜索栏等 14 个组件
│   │   ├── home/         # 会话列表/通讯录/个人页/收藏页
│   │   ├── message/      # 消息渲染/Markdown解析器/图片查看器/链接预览
│   │   ├── dialog/       # 转发弹窗
│   │   └── drawer/       # 在线用户抽屉
│   └── theme/            # MaterialKolor 动态配色
├── ChatModels.kt         # UI State + 数据类（@Serializable）
└── MainActivity.kt       # 入口
```

### 设计原则

- **ChatActions** 统一回调对象，消除 40+ 个 lambda 参数传递
- **Delegate 模式**：Auth / Message / Group / Pin 各自独立，通过 MutableStateFlow 通信
- **Markdown + @Mention 一体化解析**：单 pass token 化，不互斥
- **三级缓存架构**：PersistentCache(SP) → AppCache(LRU) → Network
- **编译器友好**：大文件拆分、constexpr where possible、纯逻辑文件零 Compose 依赖

## 构建

### 前置条件
- Android Studio 最新稳定版
- JDK 17+
- Android SDK 36

### 步骤

```bash
git clone https://github.com/keeAzlynth/just_chat.git
cd just_chat
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

### Release 签名

```properties
# ~/.gradle/gradle.properties （不提交 Git）
RELEASE_STORE_FILE=imchat.keystore
RELEASE_STORE_PASSWORD=***
RELEASE_KEY_ALIAS=imchat
RELEASE_KEY_PASSWORD=***
```

### 配合后端

后端仓库（C++）参考项目根目录 `BUILD_GUIDE.md` 编译启动。修改 `app/build.gradle.kts` 中 `WS_URL` 指向服务端地址即可联调。

## License

Business Source License 1.1 — 2026-07-05 至 2030-07-05 期间受 BSL 1.1 约束，到期后自动转为 MIT License。

详见 [LICENSE](LICENSE)。
