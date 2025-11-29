# Epub导出组件优化实现说明

## 概述
本次优化为legado-export项目的Epub导出功能添加了多项高级配置选项，提供更灵活的导出控制。

## 已实现功能

### 1. 配置数据结构 (EpubExportConfig.kt)
创建了完整的配置数据类，支持：
- CSS模板管理（默认/自定义/追加模式）
- 元数据自定义输出
- EPUB版本选择（2.0/3.0）
- Logo自定义
- 空章节处理
- 多看全屏封面支持
- 附加资源文件
- 章节编辑功能

### 2. 配置项集成 (AppConfig.kt + PreferKey.kt)
添加了以下配置项：
- `epubCssMode`: CSS模式（0=默认, 1=自定义, 2=追加）
- `epubVersion`: EPUB版本（"2.0" 或 "3.0"）
- `epubExportLogo`: 是否导出Logo
- `epubExportEmptyChapters`: 是否导出空章节
- `epubEnableDuokanFullscreen`: 启用多看全屏封面

### 3. 辅助工具类 (EpubExportHelper.kt)
提供以下功能：
- `applyCssConfig()`: 应用CSS配置（默认/自定义/追加）
- `applyLogoConfig()`: 应用Logo配置
- `applyMetadataConfig()`: 应用元数据配置
- `addAdditionalAssets()`: 添加附加资源
- `shouldExportChapter()`: 判断是否导出空章节
- `markHtmlTags()`: HTML标签标记
- `applyChapterEdits()`: 应用章节编辑
- `mergeChapters()`: 合并章节内容

### 4. 核心库扩展 (SpineReference.java + PackageDocumentWriter.java)
- 为`SpineReference`添加`properties`字段
- 支持在`<itemref>`标签中输出`properties`属性
- 实现多看阅读全屏封面支持（`duokan-page-fullscreen`）

### 5. 导出服务集成 (ExportBookService.kt)
- 使用`AppConfig.epubVersion`设置EPUB版本
- 根据`AppConfig.epubExportLogo`控制Logo导出
- 实现`applyDuokanFullscreen()`方法应用全屏属性
- 支持分卷二级目录（已有功能，通过`chapter.isVolume`判断）

## 使用方式

### 基础配置
```kotlin
// 设置EPUB版本
AppConfig.epubVersion = "3.0"

// 启用/禁用Logo导出
AppConfig.epubExportLogo = false

// 启用多看全屏封面
AppConfig.epubEnableDuokanFullscreen = true

// CSS模式
AppConfig.epubCssMode = 1 // 0=默认, 1=自定义, 2=追加
```

### 自定义CSS模板
在导出目录创建`Asset`文件夹，按以下结构组织：
```
Asset/
├── Styles/
│   ├── main.css      # 主样式
│   ├── fonts.css     # 字体样式
│   └── custom_*.css  # 附加样式
├── Text/
│   ├── chapter.html  # 章节模板
│   ├── cover.html    # 封面模板
│   └── intro.html    # 简介模板
└── Images/
    └── logo.png      # 自定义Logo
```

### 章节编辑
```kotlin
val edits = listOf(
    ChapterEdit(chapter1, isDeleted = true),  // 删除章节
    ChapterEdit(chapter2, newOrder = 5),      // 调整顺序
    ChapterEdit(chapter3, newLevel = 0),      // 设为卷
    ChapterEdit(chapter4, mergeWithNext = true) // 与下一章合并
)
val editedChapters = EpubExportHelper.applyChapterEdits(chapters, edits)
```

### HTML标签标记
```kotlin
val markedContent = EpubExportHelper.markHtmlTags(
    content = chapterContent,
    pattern = "第.*?章",
    tag = "span class='chapter-title'"
)
```

## 技术细节

### 多看全屏封面实现
多看阅读通过在`content.opf`的`<itemref>`标签添加`properties="duokan-page-fullscreen"`实现全屏封面：
```xml
<spine toc="ncx">
    <itemref idref="cover" properties="duokan-page-fullscreen"/>
    <itemref idref="chapter_001"/>
</spine>
```

实现步骤：
1. 扩展`SpineReference`类添加`properties`字段
2. 修改`PackageDocumentWriter`输出properties属性
3. 在导出时根据配置设置属性值

### 分卷二级目录
已支持通过`BookChapter.isVolume`标识分卷：
- `isVolume = true`: 作为一级目录（卷）
- `isVolume = false`: 作为二级目录（章节）

导出时自动创建层级结构：
```kotlin
if (chapter.isVolume) {
    parentSection = epubBook.addSection(title, chapterResource)
} else {
    epubBook.addSection(parentSection, title, chapterResource)
}
```

### CSS模式说明
- **默认模式(0)**: 使用内置的main.css和fonts.css
- **自定义模式(1)**: 完全使用自定义CSS模板
- **追加模式(2)**: 在默认CSS基础上追加自定义CSS

## 待完善功能

### UI界面
需要在设置页面添加以下配置项：
1. EPUB版本选择器
2. CSS模板管理界面
3. 元数据编辑器
4. Logo上传/选择
5. 多看全屏封面开关
6. 章节编辑器

### 建议实现位置
- 在`CacheActivity.kt`的菜单中添加"EPUB设置"选项
- 创建新的`EpubSettingsActivity`或对话框
- 使用`PreferenceFragment`实现配置界面

### 配置持久化
当前配置通过`AppConfig`保存到SharedPreferences，复杂配置（如CSS模板列表）建议：
- 使用JSON序列化存储
- 或创建数据库表存储

## 文件清单

### 新增文件
1. `app/src/main/java/io/legado/app/data/entities/EpubExportConfig.kt`
2. `app/src/main/java/io/legado/app/help/book/EpubExportHelper.kt`
3. `EPUB_EXPORT_OPTIMIZATION.md`

### 修改文件
1. `app/src/main/java/io/legado/app/constant/PreferKey.kt`
2. `app/src/main/java/io/legado/app/help/config/AppConfig.kt`
3. `app/src/main/java/io/legado/app/service/ExportBookService.kt`
4. `modules/book/src/main/java/me/ag2s/epublib/domain/SpineReference.java`
5. `modules/book/src/main/java/me/ag2s/epublib/epub/PackageDocumentWriter.java`

## 兼容性说明
- 所有修改向后兼容，不影响现有导出功能
- 默认配置保持原有行为
- EPUB 2.0和3.0均已支持
- 多看全屏属性仅在启用时添加，不影响其他阅读器

## 测试建议
1. 测试默认配置导出
2. 测试EPUB 3.0导出
3. 测试自定义CSS模板
4. 测试多看全屏封面（使用多看阅读验证）
5. 测试分卷二级目录
6. 测试空章节处理
7. 测试章节编辑功能

## 注意事项
1. 自定义CSS模板需确保路径正确
2. 多看全屏属性仅多看阅读器支持
3. 章节编辑操作不可逆，建议先备份
4. EPUB 3.0可能在某些旧阅读器不兼容
