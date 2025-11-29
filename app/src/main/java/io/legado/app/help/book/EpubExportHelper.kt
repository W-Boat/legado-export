package io.legado.app.help.book

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.EpubExportConfig
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.FileDoc
import me.ag2s.epublib.domain.EpubBook
import me.ag2s.epublib.domain.Resource
import me.ag2s.epublib.domain.TOCReference
import splitties.init.appCtx
import java.io.File

/**
 * Epub导出辅助类
 */
object EpubExportHelper {

    /**
     * 应用CSS配置
     */
    fun applyCssConfig(epubBook: EpubBook, config: EpubExportConfig, customPath: FileDoc?) {
        when (config.cssMode) {
            0 -> loadDefaultCss(epubBook) // 默认
            1 -> loadCustomCss(epubBook, config, customPath) // 自定义
            2 -> loadAppendCss(epubBook, config, customPath) // 追加
        }
    }

    private fun loadDefaultCss(epubBook: EpubBook) {
        epubBook.resources.add(
            Resource(appCtx.assets.open("epub/fonts.css").readBytes(), "Styles/fonts.css")
        )
        epubBook.resources.add(
            Resource(appCtx.assets.open("epub/main.css").readBytes(), "Styles/main.css")
        )
    }

    private fun loadCustomCss(epubBook: EpubBook, config: EpubExportConfig, customPath: FileDoc?) {
        val template = config.customCssTemplates.find { it.name == config.selectedCssTemplate }
        template?.let {
            it.mainCss?.let { css ->
                epubBook.resources.add(Resource(css.toByteArray(), "Styles/main.css"))
            }
            it.fontsCss?.let { css ->
                epubBook.resources.add(Resource(css.toByteArray(), "Styles/fonts.css"))
            }
            it.additionalCss.forEachIndexed { index, css ->
                epubBook.resources.add(Resource(css.toByteArray(), "Styles/custom_$index.css"))
            }
        } ?: loadDefaultCss(epubBook)
    }

    private fun loadAppendCss(epubBook: EpubBook, config: EpubExportConfig, customPath: FileDoc?) {
        loadDefaultCss(epubBook)
        val template = config.customCssTemplates.find { it.name == config.selectedCssTemplate }
        template?.additionalCss?.forEachIndexed { index, css ->
            epubBook.resources.add(Resource(css.toByteArray(), "Styles/append_$index.css"))
        }
    }

    /**
     * 应用Logo配置
     */
    fun applyLogoConfig(epubBook: EpubBook, config: EpubExportConfig) {
        if (!config.exportLogo) return

        config.customLogoPath?.let { path ->
            File(path).takeIf { it.exists() }?.let {
                epubBook.resources.add(Resource(it.readBytes(), "Images/logo.png"))
                return
            }
        }

        epubBook.resources.add(
            Resource(appCtx.assets.open("epub/logo.png").readBytes(), "Images/logo.png")
        )
    }

    /**
     * 应用元数据配置
     */
    fun applyMetadataConfig(epubBook: EpubBook, book: Book, config: EpubExportConfig) {
        val metadata = epubBook.metadata

        if (!config.exportTitle) metadata.titles.clear()
        if (!config.exportAuthor) metadata.authors.clear()
        if (!config.exportPublisher) metadata.publishers.clear()
        if (!config.exportDescription) metadata.descriptions.clear()
        if (!config.exportLanguage) metadata.language = ""
        if (!config.exportDate) metadata.dates.clear()

        config.customMetadata.forEach { (key, value) ->
            metadata.otherProperties.add(me.ag2s.epublib.domain.Metadata.OtherProperty(key, value))
        }
    }

    /**
     * 添加附加资源
     */
    fun addAdditionalAssets(epubBook: EpubBook, config: EpubExportConfig) {
        config.additionalAssets.forEach { asset ->
            File(asset.path).takeIf { it.exists() }?.let { file ->
                epubBook.resources.add(Resource(file.readBytes(), asset.targetPath))
            }
        }
    }

    /**
     * 添加预制HTML页面
     */
    fun addCustomHtmlPages(epubBook: EpubBook, config: EpubExportConfig, position: Int) {
        config.customHtmlPages.filter { it.position == position }.forEach { page ->
            val cssPath = "Styles/${page.name}.css"
            epubBook.resources.add(Resource(page.cssContent.toByteArray(), cssPath))

            val html = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>${page.name}</title>
    <link href="../$cssPath" type="text/css" rel="stylesheet"/>
</head>
<body>
${page.htmlContent}
</body>
</html>"""

            epubBook.addSection(page.name, Resource(html.toByteArray(), "Text/${page.name}.html"))
        }
    }

    /**
     * 处理空章节
     */
    fun shouldExportChapter(chapter: BookChapter, content: String?, config: EpubExportConfig): Boolean {
        if (config.exportEmptyChapters) return true
        return !content.isNullOrBlank()
    }

    fun getEmptyChapterContent(config: EpubExportConfig): String {
        return config.emptyChapterContent.ifBlank { "" }
    }

    /**
     * 应用多看全屏封面属性
     */
    fun shouldApplyDuokanFullscreen(pageId: String, config: EpubExportConfig): Boolean {
        return config.enableDuokanFullscreen && config.duokanFullscreenPages.contains(pageId)
    }

    /**
     * 搜索并标记HTML
     */
    fun markHtmlTags(content: String, pattern: String, tag: String): String {
        if (pattern.isBlank()) return content
        return content.replace(Regex(pattern), "<$tag>$0</$tag>")
    }

    /**
     * 章节编辑操作
     */
    data class ChapterEdit(
        val chapter: BookChapter,
        var isDeleted: Boolean = false,
        var newOrder: Int = chapter.index,
        var newLevel: Int = if (chapter.isVolume) 0 else 1, // 0=卷 1=章节
        var mergeWithNext: Boolean = false
    )

    /**
     * 应用章节编辑
     */
    fun applyChapterEdits(
        chapters: List<BookChapter>,
        edits: List<ChapterEdit>
    ): List<BookChapter> {
        val editMap = edits.associateBy { it.chapter.url }
        val result = mutableListOf<BookChapter>()

        chapters.forEach { chapter ->
            val edit = editMap[chapter.url]
            if (edit?.isDeleted == true) return@forEach

            val modified = chapter.copy(
                index = edit?.newOrder ?: chapter.index,
                isVolume = edit?.newLevel == 0
            )
            result.add(modified)
        }

        return result.sortedBy { it.index }
    }

    /**
     * 合并章节内容
     */
    fun mergeChapters(
        chapters: List<BookChapter>,
        contents: Map<String, String>,
        edits: List<ChapterEdit>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val editMap = edits.associateBy { it.chapter.url }

        var i = 0
        while (i < chapters.size) {
            val chapter = chapters[i]
            val edit = editMap[chapter.url]

            if (edit?.mergeWithNext == true && i + 1 < chapters.size) {
                val nextChapter = chapters[i + 1]
                val merged = "${contents[chapter.url] ?: ""}\n\n${contents[nextChapter.url] ?: ""}"
                result[chapter.url] = merged
                i += 2
            } else {
                result[chapter.url] = contents[chapter.url] ?: ""
                i++
            }
        }

        return result
    }
}
