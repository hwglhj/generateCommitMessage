package com.fs

/**
 * API 路径匹配器:纯字符串逻辑、无 UI/平台依赖,便于单元测试。
 *
 * 支持三类输入:
 * 1. 路径形式(完整 URL、半截 URL、含 "/" 或 "?" 的片段)→ 滑动分段匹配,
 *    容忍 context-path 前缀、{变量} 占位符、部分路径与末段前缀输入;
 * 2. 空格分词 → 所有关键词都命中才保留,按相关性评分排序;
 * 3. 单关键词 → 包含匹配,按相关性评分排序。
 * 另支持 "GET /path" 形式的请求方式前缀过滤。
 */
class ApiPathMatcher(candidates: List<Candidate>) {

    /** 一个可被匹配的 API 条目;displayText 仅作为结果句柄返回,匹配只用结构化字段 */
    data class Candidate(
        val displayText: String,
        val httpMethod: String, // GET/POST/PUT/DELETE/PATCH 或 CLASS
        val path: String        // 定义路径,如 /bole/assess/{id}/pages
    )

    private class Indexed(
        val candidate: Candidate,
        val isClassEntry: Boolean,
        val segments: List<String>,
        val segmentsLower: List<String>,
        val pathLower: String
    )

    // 分段、小写等在构建时一次性预计算,匹配阶段不再做字符串解析。
    // segmentsLower 中的 {param} 占位符剥掉花括号,关键词打分按参数名比较,
    // 这样搜 "{id}" (等价于 "id") 时,占位符恰好叫 {id} 的路径能按整段命中排到最前
    private val indexed: List<Indexed> = candidates.map { c ->
        val path = if (c.path.isEmpty() && c.httpMethod == "CLASS") "/" else c.path
        val segments = path.split('/').filter { it.isNotEmpty() }
        Indexed(
            candidate = c,
            isClassEntry = c.httpMethod == "CLASS",
            segments = segments,
            segmentsLower = segments.map { stripBraces(it.lowercase()) },
            pathLower = path.lowercase()
        )
    }

    /** 全量条目,按路径排序(搜索文本过短时的兜底展示) */
    val allDisplayTexts: List<String> by lazy {
        indexed.sortedBy { it.pathLower }.map { it.candidate.displayText }
    }

    fun match(searchText: String): List<String> {
        // 花括号只保留参数名:搜 "{id}" 等价于搜 "id"
        val processed = searchText.replace(BRACE_REGEX) { it.groupValues[1] }.trim()
        if (processed.isEmpty()) return emptyList()

        // 支持 "GET /path" 形式:先按请求方式过滤候选,再匹配剩余部分
        val methodPrefixMatch = METHOD_PREFIX_REGEX.find(processed)
        val httpMethodFilter = methodPrefixMatch?.groupValues?.get(1)?.uppercase()
        val effectiveText = (methodPrefixMatch?.groupValues?.get(2) ?: processed).trim()
        val pool = if (httpMethodFilter != null) {
            indexed.filter { it.candidate.httpMethod == httpMethodFilter || it.isClassEntry }
        } else {
            indexed
        }

        return when {
            isPathLike(effectiveText) -> matchAsPath(pool, effectiveText)
            effectiveText.contains(" ") ->
                matchByKeywords(pool, effectiveText.lowercase().split(" ").filter { it.isNotEmpty() })
            else -> matchByKeywords(pool, listOf(effectiveText.lowercase()))
        }
    }

    // ---------- 路径形式匹配 ----------

    private fun matchAsPath(pool: List<Indexed>, text: String): List<String> {
        val searchPath = extractPathFromInput(text)
        val searchSegments = searchPath.split('/').filter { it.isNotEmpty() }

        val methodMatches = mutableListOf<Pair<Indexed, Int>>()
        val classMatches = mutableListOf<Pair<Indexed, Int>>()

        if (searchSegments.isNotEmpty()) {
            pool.forEach { entry ->
                if (entry.segments.isEmpty()) return@forEach
                var score = scorePathSegments(entry.segments, searchSegments) ?: return@forEach

                // 类条目路径只是前缀,相关性降低
                if (entry.isClassEntry && entry.segments.size < searchSegments.size) {
                    score -= 5
                }

                // 只有搜索段数不少于定义段数时(完整请求 URL 场景)才应用得分阈值过滤,
                // 用户输入部分路径时全部参与展示,由得分负责排序
                val applyThreshold = searchSegments.size >= entry.segments.size
                if (entry.isClassEntry) {
                    if (!applyThreshold || score >= -5) classMatches.add(entry to score)
                } else {
                    if (!applyThreshold || score >= 0) methodMatches.add(entry to score)
                }
            }
        }

        val byScoreThenText = compareByDescending<Pair<Indexed, Int>> { it.second }
            .thenBy { it.first.candidate.displayText }

        // 优先显示方法匹配,其次类匹配;分段匹配无结果时退回包含匹配,保证召回不低于旧行为
        return when {
            methodMatches.isNotEmpty() -> methodMatches.sortedWith(byScoreThenText).map { it.first.candidate.displayText }
            classMatches.isNotEmpty() -> classMatches.sortedWith(byScoreThenText).map { it.first.candidate.displayText }
            else -> {
                val keyword = searchPath.trim('/').lowercase()
                if (keyword.isEmpty()) emptyList() else matchByKeywords(pool, listOf(keyword))
            }
        }
    }

    /** 从任意形式的输入中提取纯路径:去掉协议、域名端口、查询参数和锚点。internal 以便单测。 */
    internal fun extractPathFromInput(input: String): String {
        var text = input.trim()
        try {
            if (text.contains("://")) {
                // 完整 URL:去掉协议和域名端口
                text = text.substringAfter("://")
                val slashIndex = text.indexOf('/')
                text = if (slashIndex != -1) text.substring(slashIndex) else "/"
            } else if (!text.startsWith("/") && text.matches(DOMAIN_PATH_REGEX)) {
                // domain.com/path 形式:去掉域名端口
                text = text.substring(text.indexOf('/'))
            }
            // 去掉查询参数和锚点
            text = text.substringBefore('?').substringBefore('#')
            // 合并连续斜杠并去掉末尾斜杠
            text = text.replace(MULTI_SLASH_REGEX, "/")
            if (text.length > 1) {
                text = text.trimEnd('/')
            }
            return text
        } catch (e: Exception) {
            return input
        }
    }

    /**
     * 计算定义路径段与搜索路径段的匹配得分,完全无法对齐时返回 null。
     * 定义段中的 {param} 占位符可匹配任意搜索段;搜索的最后一段允许前缀匹配(支持边输入边搜)。
     * internal 以便单测。
     */
    internal fun scorePathSegments(definedSegments: List<String>, searchSegments: List<String>): Int? {
        if (definedSegments.isEmpty() || searchSegments.isEmpty()) return null

        fun segmentScore(defined: String, search: String, allowPrefix: Boolean): Int? = when {
            defined.startsWith("{") && defined.endsWith("}") -> if (search.isNotEmpty()) SCORE_PLACEHOLDER else null
            defined.equals(search, ignoreCase = true) -> SCORE_EXACT
            allowPrefix && defined.startsWith(search, ignoreCase = true) -> SCORE_PREFIX
            else -> null
        }

        fun windowScore(definedOffset: Int, searchOffset: Int, length: Int): Int? {
            var sum = 0
            for (i in 0 until length) {
                val allowPrefix = searchOffset + i == searchSegments.size - 1
                sum += segmentScore(
                    definedSegments[definedOffset + i],
                    searchSegments[searchOffset + i],
                    allowPrefix
                ) ?: return null
            }
            return sum
        }

        var best: Int? = null
        if (definedSegments.size <= searchSegments.size) {
            // 搜索路径更长(如带 context-path 前缀的完整请求 URL):定义路径在搜索路径中滑动对齐
            for (offset in 0..searchSegments.size - definedSegments.size) {
                val base = windowScore(0, offset, definedSegments.size) ?: continue
                val trailing = searchSegments.size - offset - definedSegments.size
                var score = base - offset - trailing * 2 // 被跳过的前缀段(如 context-path)轻度惩罚,未覆盖的尾段较重惩罚
                if (trailing == 0) score += BONUS_TAIL_ALIGNED // 与请求路径尾部对齐,奖励
                if (offset == 0 && trailing == 0) score += BONUS_FULL_ALIGNED // 完全对齐
                if (best == null || score > best!!) best = score
            }
        } else {
            // 搜索路径更短(用户只输入了路径的一部分):搜索段在定义路径中滑动对齐
            for (offset in 0..definedSegments.size - searchSegments.size) {
                val base = windowScore(offset, 0, searchSegments.size) ?: continue
                var score = base - (definedSegments.size - searchSegments.size) // 定义路径越长惩罚越多
                if (offset + searchSegments.size == definedSegments.size) score += BONUS_DEFINED_TAIL // 与定义路径尾部对齐
                if (offset == 0) score += BONUS_DEFINED_HEAD // 从头对齐
                if (best == null || score > best!!) best = score
            }
        }
        return best
    }

    private fun isUrl(text: String): Boolean {
        return text.startsWith("http://") ||
                text.startsWith("https://") ||
                text.matches(DOMAIN_LIKE_REGEX) // 类似 domain.com/path 的格式
    }

    /** 判断输入是否是路径形式:完整 URL、含 / 的半截路径、或带查询参数的片段 */
    private fun isPathLike(text: String): Boolean {
        return isUrl(text) || text.contains("/") || text.contains("?")
    }

    // ---------- 关键词匹配 ----------

    /** 所有关键词都命中才保留,按相关性评分降序 */
    private fun matchByKeywords(pool: List<Indexed>, keywords: List<String>): List<String> {
        return pool.filter { entry -> keywords.all { entry.pathLower.contains(it) } }
            .sortedByDescending { keywordScore(it, keywords) }
            .map { it.candidate.displayText }
    }

    private fun keywordScore(entry: Indexed, keywords: List<String>): Int {
        val pathLower = entry.pathLower
        var score = 0

        keywords.forEach { keyword ->
            if (pathLower.contains(keyword)) {
                score += 5 // 关键词命中基础分

                val lastSegment = entry.segmentsLower.lastOrNull()
                if (lastSegment == keyword) {
                    score += 100 // 末段完全匹配,最相关
                } else if (lastSegment?.contains(keyword) == true) {
                    score += 20 // 末段部分匹配
                }

                if (entry.segmentsLower.dropLast(1).any { it == keyword }) {
                    score += 50 // 非末段的整段匹配
                }

                val lastIndex = pathLower.lastIndexOf(keyword)
                if (lastIndex != -1 && pathLower.isNotEmpty()) {
                    score += lastIndex * 15 / pathLower.length // 命中位置越靠后越相关(0-15)
                }
            }
        }

        score -= entry.segments.size // 路径越长惩罚越多
        return score
    }

    companion object {
        /** "{param}" → "param",非占位符原样返回 */
        private fun stripBraces(segment: String): String =
            if (segment.length > 2 && segment.startsWith("{") && segment.endsWith("}")) {
                segment.substring(1, segment.length - 1)
            } else {
                segment
            }

        private val METHOD_PREFIX_REGEX = Regex("^(GET|POST|PUT|DELETE|PATCH)\\s+(.*)$", RegexOption.IGNORE_CASE)
        private val BRACE_REGEX = Regex("\\{([^}]+)\\}")
        private val DOMAIN_PATH_REGEX = Regex("[^/?]+\\.[^/?]+/.*")
        private val DOMAIN_LIKE_REGEX = Regex(".+\\..+/.+")
        private val MULTI_SLASH_REGEX = Regex("/+")

        // 分段匹配打分:精确 > 末段前缀 = 占位符
        private const val SCORE_EXACT = 2
        private const val SCORE_PLACEHOLDER = 1
        private const val SCORE_PREFIX = 1
        private const val BONUS_TAIL_ALIGNED = 5
        private const val BONUS_FULL_ALIGNED = 2
        private const val BONUS_DEFINED_TAIL = 3
        private const val BONUS_DEFINED_HEAD = 1
    }
}
