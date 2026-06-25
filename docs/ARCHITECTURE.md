# 架构说明

## 分层

```
┌─────────────────────────────────────────┐
│   UI (Compose Multiplatform, common)    │  HomeScreen, ReaderScreen, ...
│   状态: SuduguViewModel + StateFlow     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│   Data                                   │
│   - SuduguApi: 客户端 API（直接抓）      │
│   - ServerApi: 服务端 API（HTTP）         │
│   - Scraper: Jsoup 解析 + AdFilter       │
│   - LocalStore: multiplatform-settings   │
└──────────────┬──────────────────────────┘
               │
        ┌──────┴──────┐
        │             │
┌───────▼────┐  ┌─────▼─────┐
│  Ktor Client│  │ Jsoup    │  (common)
│  (per-OS)  │  │ HTML     │
└─────┬──────┘  └────┬─────┘
      │              │
      │              ▼
      │       https://sudugu.org
      │
┌─────▼──────────────┐
│  Ktor Server (CIO)  │  (server/ JVM module)
│  /api/*             │
└─────────────────────┘
```

## 关键设计

### 1. 客户端/服务端同源爬虫

`composeApp/src/commonMain/data/Scraper.kt` 和 `server/src/main/.../scraper/Scraper.kt` 是**两套**实现（一份 Kotlin Multiplatform、一份纯 JVM），逻辑完全等价：
- 同样的 Jsoup selector
- 同样的 `AdFilter` 启发式
- 同样的分页累积循环

为什么不全放 server？**Android/iOS/Desktop 直接抓站点更快**（省一跳），Web (wasmJs) 因 CORS 必须走后端。

### 2. 平台 HTTP 引擎

每个 source set 通过 `expect/actual` 提供引擎：

| 平台 | 引擎 | 文件 |
| --- | --- | --- |
| Android | OkHttp | `androidMain/.../PlatformEngine.android.kt` |
| iOS | Darwin (NSURLSession) | `iosMain/.../PlatformEngine.ios.kt` |
| Desktop (JVM) | OkHttp | `jvmMain/.../PlatformEngine.jvm.kt` |
| Web (Wasm) | Js (browser fetch) | `wasmJsMain/.../PlatformEngine.wasmJs.kt` |

### 3. 存储

`multiplatform-settings` 提供统一 KMP API：

| 平台 | 底层 |
| --- | --- |
| Android | SharedPreferences |
| iOS | NSUserDefaults |
| Desktop | java.util.prefs.Preferences |
| Web | window.localStorage |

通过 `expect fun createSettings(): Settings` 在 commonMain 注入。

### 4. 状态管理

单 ViewModel 模式：`SuduguViewModel` 持有所有 UI 状态（StateFlow），用 `viewModelScope` 启动协程拉取数据。简单清晰，不引入额外 DI 框架。

### 5. 路由

`navigation-compose 2.8` + 自定义 `Routes` 工具类构造带参路由。书名等中文路径参数用 `Url.encode` 百分号编码。

## 广告过滤详解

`AdFilter.isAdParagraph(text)` 五重检查：

1. **空或太短**（< 4 字符）
2. **纯符号**（`^[\s?？,。.、…!！_+\-=*·•・]+$`）
3. **连续标点**（`[?？,。.]{3,}` —— 典型 `，??` 广告片段）
4. **关键词**（速读谷、扫码加群、点击阅读、更多小说 等）
5. **HTML/HTML entity 残留**（说明解析器没干净 strip 干净）

这条规则集从原 RN `src/services/scraper.ts` 翻译过来，并通过测试覆盖。

## 章节分页

`Scraper.scrapeChapter` 流程：

```
page = 1
loop while page < 5:
  html = fetch(/bookId/chapterId.html)        # 第一次
              | fetch(/bookId/chapterId-2.html)  # 后续
  con = .con
  paragraphs = [p for p in con.select("p") if not isAd(p.text())]
  allParagraphs += paragraphs
  nextHref = prenext.a[href]
                .where(href matches /bookId/chapterId-N\.html/)
                .last   # 取最后一个 — 上一章/目录/下一页 顺序中"下一页"在最后
  if nextHref: continue else break
```

**严格正则 `^/$bookId/$chapterId-\d+\.html$`** 防止被"上一章"链接（`/bookId/<otherChapterId>.html`，无 `-`）误识别为下一页分页。

## 扩展点

- 替换数据源：把 `composeApp/src/commonMain/data/Scraper.kt` 改成别的站结构，再让 `server/.../scraper/Scraper.kt` 同步
- 加新屏：在 `ui/screens/` 加新 Composable + 在 `ui/nav/SuduguNavHost.kt` 注册路由 + 在 `SuduguViewModel` 加 StateFlow
- 加新平台：CMP 支持的 target 加一行 `xxxMain by getting {}` 即可
