# Just Chat — Android IM Client

仿 Telegram / QQ 风格的即时通讯 Android 客户端，纯 Kotlin + Jetpack Compose 实现。

本仓库仅包含前端代码。后端为独立的 C++ 项目，使用 Boost.Beast + SQLite3。

## 功能特性

### 核心通讯
- WebSocket 实时通讯，支持断线重连
- 公共聊天室、私聊、群组三种模式
- 消息已读回执（Telegram 风格 ✓✓）
- 对方正在输入中... 提示
- 在线状态同步 & 最后在线时间

### 消息体验
- **Markdown 渲染** — 纯 Compose 解析器，支持粗体/斜体/删除线/代码块/标题/链接 + @提及高亮
- **图片消息** — 点击进入全屏预览，支持 pinch-to-zoom / 双击缩放
- **文件消息** — 显示文件名和大小
- **编辑消息** — 长按 → 编辑，修改后显示"已编辑"标记
- **撤回消息** — 管理员可撤回
- **引用回复** — 双击消息气泡或滑动快捷引用
- **消息转发** — 单条或批量多选转发到其他会话
- **消息置顶** — Pin / Unpin + 顶部栏跳转
- **消息收藏** — 保存到收藏夹
- **Emoji 回应** — 长按选择 emoji 表情反应
- **表情面板** — 分类表情选择器 + 最近使用记录

### 交互细节
- 发送触觉反馈（15ms 轻微震动）
- 滚动到底部 FAB 按钮
- 空状态引导页
- URL 链接预览卡片
- 输入框 @ 提及自动补全
- 输入框粘贴按钮（剪贴板有内容时显示）
- 语音录制按钮（长按录音 + 左滑取消 + 波形动画）

### 消息列表
- 日期分隔线
- 多选模式（批量删除/转发）
- 聊天内容搜索（关键词高亮）
- 智能回复建议（基于最近消息上下文）
- 最近使用 Emoji 面板顶部置顶

## 截屏

| 消息列表 | 聊天界面 | 设置 |
|----------|----------|------|
| ![](screen.png) | — | — |

> 更多截屏待补充。欢迎提 PR 添加。

## 技术栈

| 层 | 技术 |
|----|------|
| UI | Jetpack Compose + Material 3 |
| 状态管理 | StateFlow + SnapshotStateList |
| 网络 | OkHttp WebSocket |
| 序列化 | Gson |
| 持久化 | SharedPreferences (SessionCache) |
| 缓存 | LRU 内存缓存 (AppCache) |
| 构建 | Gradle 9.1 + Kotlin 2.x |

## 架构

```
app/src/main/java/com/course/imchat/
├── core/
│   ├── delegate/        # 功能委托：Auth / Message / Group / Pin / Connection
│   ├── notification/    # 通知辅助
│   └── util/            # ValueClass / Compose稳定性标记
├── data/
│   ├── cache/           # AppCache / LruCache / SessionCache / MessageCache
│   ├── EmojiData.kt     # 表情数据 + 最近使用记录
│   ├── MessageRepository.kt
│   ├── WebSocketClient.kt
│   └── IncomingEvent.kt # 服务端事件模型
├── domain/              # 领域模型 / Repository接口 / UseCase
├── presentation/        # ViewModel
├── ui/
│   ├── ChatScreen.kt    # 聊天主界面
│   ├── ChatApp.kt       # 导航容器
│   ├── AuthScreen.kt    # 登录/注册
│   ├── components/
│   │   ├── chat/        # 聊天子组件（输入栏/气泡/表情/提及等）
│   │   ├── home/        # 会话列表 / 通讯录 / 个人页
│   │   ├── message/     # 消息渲染 / Markdown / 预览 / 查看器
│   │   ├── dialog/      # 转发弹窗
│   │   └── drawer/      # 在线用户抽屉
│   └── theme/           # Material 3 主题
├── ChatModels.kt        # UI State 数据类
└── MainActivity.kt      # 入口
```

### 核心设计原则

- **ChatActions** 统一回调：ChatScreen 参数从 40+ 个 lambda 精简到 1 个 ChatActions 对象
- **Delegate 模式**：Auth / Message / Group / Pin 各自独立，通过 MutableStateFlow 通信
- **ChatRoom 不支持 P2P 直连**，所有消息经 WebSocket 服务端中转（后端为 C++ 项目）
- **消息缓存**：到达消息入内存 LRU，切换会话秒开，零等待

## 构建 & 运行

### 前置条件
- Android Studio (最新稳定版)
- JDK 17+
- Android SDK 36

### 步骤

```bash
# 1. 克隆仓库
git clone https://github.com/keeAzlynth/just_chat.git
cd just_chat

# 2. 配置服务端地址
# 编辑 app/build.gradle.kts 中 WS_URL 字段：
#   Debug:   ws://127.0.0.1:8080
#   Release: wss://your-server.com

# 3. 用 Android Studio 打开项目，或命令行编译：
./gradlew assembleDebug

# 4. APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

### Release 签名

```properties
# ~/.gradle/gradle.properties （不提交到 Git）
RELEASE_STORE_FILE=imchat.keystore
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=imchat
RELEASE_KEY_PASSWORD=your_password
```

## 配合后端使用

后端仓库（C++）请参考项目根目录的 `BUILD_GUIDE.md` 编译启动服务端。

服务端启动后，修改 `app/build.gradle.kts` 中 `WS_URL` 指向服务端地址，即可联调。

## 贡献

欢迎提 Issue / PR。提交前请确保 `./gradlew assembleDebug` 编译通过。

## License

MIT
