# NovelForge

> 🤖 AI驱动的中长篇小说生成工具 | Powered by Grok 4.3 API

[![Android CI](https://github.com/owjk123/NovelForge/actions/workflows/android.yml/badge.svg)](https://github.com/owjk123/NovelForge/actions)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📖 项目介绍

NovelForge 是一款基于 AI 的 Android 小说创作应用，接入 Grok 4.3 API，帮助作者高效生成中长篇小说内容。

### 核心功能

- ✨ **智能章节生成** - 一键生成小说章节内容
- 🎭 **多类型支持** - 玄幻、科幻、都市、后宫、悬疑等类型
- 📚 **章节管理** - 自动保存历史章节，支持续写
- 💾 **本地存储** - Room 数据库离线存储
- 🔄 **上下文连贯** - 自动拼接上下文，保持剧情连贯
- 📱 **Jetpack Compose** - 现代 Material 3 UI 设计

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| 架构 | MVVM + Repository |
| UI | Jetpack Compose |
| 网络 | Retrofit + OkHttp |
| JSON | Kotlinx Serialization |
| 异步 | Coroutines + Flow |
| 数据库 | Room |
| 构建 | Gradle Kotlin DSL |

## 🔧 API 配置

### 1. 获取 API Key

本项目使用 Grok 4.3 API，请从 [api.apiyi.com](https://api.apiyi.com) 获取 API Key。

### 2. 配置 API

在项目根目录创建 `local.properties` 文件：

```properties
API_KEY=your_api_key_here
API_BASE_URL=https://api.apiyi.com/v1
```

或在 GitHub Secrets 中配置：
- `API_KEY` - 你的 API Key
- `API_BASE_URL` - API 基础地址

## 📦 构建 APK

### 本地构建

```bash
# 克隆项目
git clone https://github.com/owjk123/NovelForge.git
cd NovelForge

# 创建 local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties
echo "API_KEY=your_api_key" >> local.properties
echo "API_BASE_URL=https://api.apiyi.com/v1" >> local.properties

# 授予 Gradle 执行权限
chmod +x gradlew

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

### GitHub Actions 自动构建

推送代码到 main 分支后，GitHub Actions 会自动构建：

1. 运行单元测试
2. 构建 Release APK
3. 上传构建产物

APK 文件位于 `app/build/outputs/apk/release/` 目录。

## 📱 使用说明

### 首页 - 创建小说

1. 输入小说标题
2. 选择小说类型（玄幻/科幻/都市/后宫/悬疑）
3. 填写主角设定（姓名、性格、背景等）
4. 填写世界观设定
5. 点击「开始创作」

### 写作页 - 章节管理

- **开始创作** - 生成新章节
- **继续写** - 续写当前章节
- **保存草稿** - 手动保存当前内容

### 书架页 - 小说管理

- 查看所有已创建的小说
- 点击进入阅读/继续创作
- 删除不需要的小说

## 📂 项目结构

```
app/src/main/java/com/novelforge/app/
├── data/
│   ├── api/           # Retrofit API 接口
│   ├── db/            # Room 数据库
│   ├── model/         # 数据模型
│   └── repository/    # 数据仓库
├── domain/
│   ├── prompt/        # Prompt 构建器
│   └── usecase/       # 业务用例
├── ui/
│   ├── home/          # 首页
│   ├── writing/       # 写作页
│   ├── library/       # 书架页
│   └── theme/         # 主题配置
└── viewmodel/         # ViewModel
```

## 🧪 测试

```bash
# 运行单元测试
./gradlew test

# 运行 UI 测试
./gradlew connectedAndroidTest
```

## 📄 许可证

本项目基于 MIT 许可证开源。

## 🙏 致谢

- [Grok API](https://api.apiyi.com) - AI 模型支持
- [Jetpack Compose](https://developer.android.com/compose) - UI 框架
- [Room](https://developer.android.com/training/data-storage/room) - 本地数据库
