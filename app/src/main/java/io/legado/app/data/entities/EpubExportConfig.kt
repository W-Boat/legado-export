package io.legado.app.data.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Epub导出配置
 */
@Parcelize
data class EpubExportConfig(
    // CSS配置
    var cssMode: Int = 0, // 0=默认 1=自定义 2=追加
    var customCssTemplates: MutableList<CssTemplate> = mutableListOf(),
    var selectedCssTemplate: String? = null,

    // 元数据配置
    var exportTitle: Boolean = true,
    var exportAuthor: Boolean = true,
    var exportPublisher: Boolean = true,
    var exportDescription: Boolean = true,
    var exportLanguage: Boolean = true,
    var exportDate: Boolean = true,
    var customMetadata: MutableMap<String, String> = mutableMapOf(),

    // EPUB版本
    var epubVersion: String = "2.0", // "2.0" or "3.0"

    // Logo配置
    var exportLogo: Boolean = true,
    var customLogoPath: String? = null,

    // 空章节处理
    var exportEmptyChapters: Boolean = true,
    var emptyChapterContent: String = "",

    // 多看全屏封面支持
    var enableDuokanFullscreen: Boolean = false,
    var duokanFullscreenPages: MutableSet<String> = mutableSetOf(), // 页面ID列表

    // 附加资源
    var additionalAssets: MutableList<AssetFile> = mutableListOf(),

    // 预制HTML
    var customHtmlPages: MutableList<CustomHtmlPage> = mutableListOf()
) : Parcelable

@Parcelize
data class CustomHtmlPage(
    var name: String,
    var htmlContent: String,
    var cssContent: String,
    var position: Int = 0 // 0=开头 1=结尾
) : Parcelable

@Parcelize
data class CssTemplate(
    var name: String,
    var mainCss: String? = null,
    var fontsCss: String? = null,
    var additionalCss: MutableList<String> = mutableListOf()
) : Parcelable

@Parcelize
data class AssetFile(
    var path: String,
    var targetPath: String,
    var addToNav: Boolean = false
) : Parcelable
