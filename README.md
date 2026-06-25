# 速读谷 — Compose Multiplatform

速读谷是一个中文小说阅读 App，**完全用 Jetpack Compose Multiplatform 重写**，一次代码、四端运行：

- **Android** (Compose for Android)
- **iOS** (Compose for iOS)
- **Desktop** (Compose for Desktop, JVM)
- **Web** (Compose for Web, Kotlin/Wasm)

后端用 **Ktor 3 + CIO** 引擎，可以独立部署（web 端默认通过后端访问 sudugu.org，绕过浏览器 CORS 限制）；Android/iOS/Desktop 直接在 App 内通过 Jsoup 抓取上游。

## 数据源

`https://sudugu.org` —— 中文小说站，免费阅读。**保留作为唯一数据源**。

## 关键修复（vs. 原 RN 版本）

原 React Native 版本有两个 bug 重写时已修复：

1. **广告切断**：上游会在 `<div class="con">` 中插入垃圾 `<p>`（如 `，??`、纯符号、广告关键词）。`AdFilter` 对象用启发式（短文本、纯符号、关键词、HTML 残留）过滤这些段。
2. **分页抓取**：章节分多页（`<chapterId>-2.html`、`-3.html`），`Scraper.scrapeChapter` 现在循环累积 5 页，并严格匹配 `${chapterId}-N.html` 模式（避免被"上一页"链接误识别）。

## 模块结构

```
.
├── composeApp/                  # Compose Multiplatform 共享模块
│   ├── src/commonMain/          # 共享 UI、ViewModel、爬虫、Repository
│   ├── src/androidMain/         # Android 入口（MainActivity + SuduguApplication）
│   ├── src/iosMain/             # iOS 入口（MainViewController）
│   ├── src/jvmMain/             # Desktop 入口（main.kt）
│   └── src/wasmJsMain/          # Web/Wasm 入口 + index.html
├── server/                      # Ktor CIO 后端
│   ├── src/main/kotlin/com/sudugu/server/
│   │   ├── Application.kt       # Ktor 启动 + 插件
│   │   ├── routes/              # /api/* 路由
│   │   └── scraper/             # Jsoup 服务端爬虫（与客户端共用 AdFilter 逻辑）
│   └── src/main/resources/logback.xml
├── iosApp/                      # iOS 壳工程（SwiftUI + Podfile）
├── gradle/libs.versions.toml    # 版本目录
├── settings.gradle.kts
├── build.gradle.kts
└── gradlew
```

## 一键构建（本地）

### 前置环境

- **JDK 17**（Temurin 推荐）
- **Android Studio** + Android SDK Platform 35（Android 端）
- **Xcode 15+**（iOS 端，Mac only）
- **Node 18+**（不需要，Web 是 Kotlin/Wasm）

### 1. Clone 与 Gradle wrapper

```bash
git clone https://github.com/rjmultidev/sudugu-app.git
cd sudugu-app
chmod +x gradlew
./gradlew --version
```

### 2. Android

```bash
./gradlew :composeApp:assembleDebug
# APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk

# 安装到设备
./gradlew :composeApp:installDebug

# Release
./gradlew :composeApp:assembleRelease
# 需先在 composeApp/build.gradle.kts 配置 signingConfigs
```

### 3. iOS（Mac only）

```bash
# 生成 composeApp 的 Podspec
./gradlew :composeApp:podspec

# 进入 iOS 工程
cd iosApp
pod install
open iosApp.xcworkspace
# 在 Xcode 中 Cmd+R 运行
```

> 首次生成 iosApp.xcworkspace 需要先用 Xcode 新建一个 iOS App 工程，把 iosApp/ 目录里的 Info.plist、iOSApp.swift 拖入，并把 `MainViewControllerKt.MainViewController()` 作为 root view controller。或参考 `kotlin-multiplatform` 官方向导的产物结构。

### 4. Desktop

```bash
./gradlew :composeApp:run
# 或打包 dmg / msi / deb
./gradlew :composeApp:packageDistributionForCurrentOS
# 产物: composeApp/build/compose/binaries/main/
```

### 5. Web (Kotlin/Wasm)

```bash
# 开发模式
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# 生产构建
./gradlew :composeApp:wasmJsBrowserDistribution
# 产物: composeApp/build/dist/js/productionExecutable/
# 部署: 把整个目录丢到任何静态服务器
```

Web 端因为 CORS，**必须同时启动后端**（见下）：

```bash
cd server
./gradlew :server:run
# 默认监听 0.0.0.0:3001
```

`server/src/main/resources/application.conf` 默认未启用，开发模式下 Ktor 自动给 CORS 配置为 `anyHost` 允许所有来源。

## 后端

Ktor CIO 引擎，纯 Kotlin 协程，无 Netty 依赖阻塞。生产部署建议：

```bash
./gradlew :server:installDist
./server/build/install/server/bin/server
```

端口由 `PORT` 环境变量控制（默认 3001）。

## 依赖

| 用途 | 库 |
| --- | --- |
| UI | Compose Multiplatform 1.7 |
| Navigation | JetBrains androidx-navigation 2.8 |
| 网络 | Ktor 3.0 + OkHttp (Android/JVM) / Darwin (iOS) / JS (Web) |
| 序列化 | kotlinx-serialization-json |
| 爬虫 | jsoup 1.18 |
| 存储 | multiplatform-settings 1.2 |
| 图像 | Coil 3.0 |
| 日志（server） | logback |

所有依赖在 `gradle/libs.versions.toml` 集中管理。

## 测试

```bash
./gradlew :composeApp:allTests             # 共享模块的 AdFilter 单测
./gradlew :server:test                      # 后端集成测试（连真站）
```

## 已知风险

- **沙盒环境无法验证编译** —— 这个仓库是设计在本地装有 JDK 17 + Android SDK + Xcode 的机器上构建的。CI 上验证需要装好 `cmdline-tools` + `platform-tools` + 至少一个 platform-35。
- **iOS 工程** —— `iosApp/` 提供了最小 Swift 入口 + Podfile + Info.plist，但完整的 `.xcodeproj` 没有生成（沙盒无 Xcode）。首次需要在 Xcode 里把上述文件组装成工程。

## License

原 RN 项目作者保留所有权利。本仓库重写部分以 MIT 重新发布。
