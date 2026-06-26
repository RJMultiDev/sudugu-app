#!/usr/bin/env bash
# scripts/build.sh — 本地一键构建脚本（按需选平台）
# 用法：
#   ./scripts/build.sh all       # 全部目标
#   ./scripts/build.sh android   # Android APK（debug）
#   ./scripts/build.sh desktop   # Desktop 直接跑
#   ./scripts/build.sh web       # Web/Wasm 打包
#   ./scripts/build.sh server    # 编译 + 跑后端
#   ./scripts/build.sh test      # 跑测试
set -euo pipefail

cd "$(dirname "$0")/.."
PROJECT_DIR="$(pwd)"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}"

TARGET="${1:-all}"

case "$TARGET" in
  all)
    echo "==> Build all targets"
    ./gradlew :server:installDist :composeApp:assembleDebug :composeApp:run --quiet
    ;;
  android)
    echo "==> Build Android APK (debug)"
    ./gradlew :composeApp:assembleDebug
    echo "APK at: composeApp/build/outputs/apk/debug/composeApp-debug.apk"
    ;;
  android-release)
    echo "==> Build Android APK (release)"
    ./gradlew :composeApp:assembleRelease
    echo "APK at: composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk"
    ;;
  desktop)
    echo "==> Run Desktop app"
    ./gradlew :composeApp:run
    ;;
  web)
    echo "==> Build Web (Kotlin/Wasm) distribution"
    ./gradlew :composeApp:wasmJsBrowserDistribution
    echo "Web bundle at: composeApp/build/dist/js/productionExecutable/"
    echo "Serve locally:"
    echo "  cd composeApp/build/dist/js/productionExecutable/ && python3 -m http.server 8080"
    ;;
  server)
    echo "==> Build & run Ktor server"
    ./gradlew :server:installDist
    echo "Server at: server/build/install/server/bin/server"
    "${PROJECT_DIR}/server/build/install/server/bin/server"
    ;;
  test)
    echo "==> Run all tests"
    ./gradlew :composeApp:allTests :server:test
    ;;
  clean)
    echo "==> Clean"
    ./gradlew clean
    rm -rf .gradle composeApp/build server/build iosApp/build
    ;;
  *)
    echo "Usage: $0 {all|android|android-release|desktop|web|server|test|clean}"
    exit 1
    ;;
esac
