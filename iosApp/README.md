# iOS 集成说明

由于沙盒里没有 Xcode，本仓库**没有**自动生成 `iosApp.xcodeproj`。下面是用 SwiftPM 集成 Compose Multiplatform 框架的标准步骤。

## 步骤

### 1. 生成 Kotlin/Native 框架

```bash
cd ../
./gradlew :composeApp:linkReleaseFrameworkIos
# 或一次性打四个目标
./gradlew :composeApp:linkReleaseFrameworkIosArm64
./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64
./gradlew :composeApp:linkReleaseFrameworkIosX64
```

产物在 `composeApp/build/bin/ios/main/release/framework/composeApp.framework/`

### 2. 在 Xcode 创建新工程

打开 Xcode → File → New → Project → iOS → App
- Product Name: `iosApp`
- Interface: SwiftUI
- Language: Swift
- Save into the `iosApp/` directory of this repo

把仓库里现成的两个文件覆盖进新工程：
- `iosApp/iosApp/iOSApp.swift`
- `iosApp/iosApp/Info.plist`（覆盖项目自带的）

### 3. 集成 Compose 框架（SwiftPM）

Xcode → File → Add Package Dependencies → Add Local...
- 点 "+ Add Local..."
- 选 `composeApp/build/bin/ios/main/release/framework/` 目录（或 `composeApp.xcframework`）

### 4. 加入 ContentView root

`iOSApp.swift` 已经在仓库里。它调用 `MainViewControllerKt.MainViewController()`——这是 Kotlin 编译生成的 Swift binding，会从 `composeApp.framework` 提供。

### 5. 运行

Cmd+R 即可。应该能看到 速读谷 首页。

## 故障排查

- **`'composeApp' module not found`**：framework 没正确链接。重新跑 `./gradlew :composeApp:linkReleaseFrameworkIos`，确认产物的 search path 加入了 Xcode 的 "Frameworks, Libraries, and Embedded Content"。
- **编译报 `@MainActor` 错误**：Xcode 14+ 默认对 SwiftUI 视图要求 `@MainActor`。包一层 `.task` 或者给 `MainViewController()` 调用加 `@MainActor`。
- **图标缺失**：把 `assets/android-icon-foreground.png` 转成 1024x1024 的 PNG → Xcode 的 Assets.xcassets → AppIcon。

## 不用 Cocoapods 的原因

本项目刻意**不使用** Kotlin Cocoapods plugin（`org.jetbrains.kotlin.plugin.cocoapods`），原因：
1. 该 plugin 的 Gradle Plugin Portal marker 没有在 Maven Central 单独发布，依赖 `kotlin-gradle-plugin` 自带，需要 CocoaPods/Xcode 完整工具链。
2. SwiftPM 在 Xcode 14+ 已经成熟，Cocoapods 已经不是 iOS 集成的首选。
3. SwiftPM 直接消费 `*.xcframework`，CI/CD 更简单。
