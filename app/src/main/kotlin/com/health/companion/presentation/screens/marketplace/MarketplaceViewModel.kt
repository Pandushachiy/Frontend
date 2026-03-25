package com.health.companion.presentation.screens.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.*
import com.health.companion.data.repositories.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ── Умная классификация по ключевым словам в названии и описании ───────────

data class MergedCategory(val id: String, val name: String)

private val SMART_CATEGORIES = listOf(
    MergedCategory("dev",          "Разработка"),
    MergedCategory("ai",           "ИИ и агенты"),
    MergedCategory("data",         "Данные"),
    MergedCategory("finance",      "Финансы"),
    MergedCategory("social",       "Соцсети"),
    MergedCategory("productivity", "Продуктивность"),
    MergedCategory("content",      "Контент"),
    MergedCategory("crypto",       "Крипто"),
    MergedCategory("search",       "Поиск и веб"),
    MergedCategory("other",        "Другое")
)

private val CATEGORY_KEYWORDS: Map<String, List<String>> = mapOf(
    "dev"          to listOf("code","git","github","deploy","docker","api","sdk","cli","script","python","javascript","typescript","kotlin","java","rust","go lang","compiler","debug","test","ci/cd","devops","kubernetes","linux","terminal","bash","shell","development","programming","software","build","framework","library"),
    "ai"           to listOf("ai","llm","gpt","claude","gemini","openai","anthropic","agent","prompt","embedding","vector","rag","fine-tun","model","neural","machine learning","deep learning","inference","langchain","openclaw","cursor","copilot","whisper","tts","stt","speech","vision","image generation","midjourney","stable diffusion"),
    "data"         to listOf("data","database","sql","analytics","dashboard","csv","excel","spreadsheet","pandas","spark","pipeline","etl","warehouse","bi","tableau","power bi","mongodb","postgres","redis","kafka","scrape","crawl","extract","parse"),
    "finance"      to listOf("finance","trading","stock","forex","market","investment","portfolio","crypto fund","hedge","quant","earnings","economic","gdp","inflation","dividend","option","future","nasdaq","nyse"),
    "social"       to listOf("twitter","x.com","instagram","facebook","tiktok","linkedin","reddit","discord","slack","telegram","whatsapp","wechat","youtube","twitch","social media","post","tweet","comment","follower"),
    "productivity" to listOf("productivity","task","todo","reminder","calendar","schedule","planner","habit","goal","note","email","inbox","organize","workflow","automation","zapier","make.com","n8n","meeting","project management","trello","notion","obsidian"),
    "content"      to listOf("content","blog","article","write","seo","copywriting","marketing","newsletter","podcast","video script","subtitle","transcript","translate","summarize","pdf","document","report","presentation","powerpoint"),
    "crypto"       to listOf("crypto","bitcoin","ethereum","blockchain","defi","nft","wallet","solana","polygon","avalanche","chainlink","uniswap","metamask","web3","token","smart contract","dex","dao","staking","yield"),
    "search"       to listOf("search","web search","browse","browser","google","bing","duckduckgo","url","fetch","scrape web","news","rss","weather","maps","location","geocod")
)

fun classifySkill(skill: MarketplaceSkill): String {
    val text = "${skill.name} ${skill.description} ${skill.tags.joinToString(" ")}".lowercase()
    for ((catId, keywords) in CATEGORY_KEYWORDS) {
        if (keywords.any { text.contains(it) }) return catId
    }
    return "other"
}

// ───────────────────────────────────────────────────────────────────────────

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val repo: MarketplaceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MarketplaceSkill>>(emptyList())
    val searchResults: StateFlow<List<MarketplaceSkill>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _rawSkills = MutableStateFlow<List<MarketplaceSkill>>(emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedSort = MutableStateFlow("downloads")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    private val _selectedSkill = MutableStateFlow<MarketplaceSkill?>(null)
    val selectedSkill: StateFlow<MarketplaceSkill?> = _selectedSkill.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _installMessage = MutableStateFlow<String?>(null)
    val installMessage: StateFlow<String?> = _installMessage.asStateFlow()

    private var searchJob: Job? = null
    private var loadAllJob: Job? = null

    // Категории — только те у которых есть скиллы
    val categories: StateFlow<List<MergedCategory>> = _rawSkills.map { skills ->
        SMART_CATEGORIES.filter { mc ->
            skills.any { classifySkill(it) == mc.id }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSkills: StateFlow<List<MarketplaceSkill>> =
        combine(_rawSkills, _selectedCategory, _selectedSort) { raw, category, sort ->
            val filtered = if (category != null) {
                raw.filter { classifySkill(it) == category }
            } else raw

            when (sort) {
                "stars"  -> filtered.sortedByDescending { it.stars }
                "recent" -> filtered.sortedByDescending { it.updatedAt ?: "" }
                else     -> filtered.sortedByDescending { it.downloads }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalResults: StateFlow<Int> =
        combine(_searchQuery, allSkills, _searchResults) { q, skills, results ->
            if (q.isBlank()) skills.size else results.size
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        loadAllJob?.cancel()
        loadAllJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repo.getFirstPage()
                .onSuccess { firstPage ->
                    _rawSkills.value = firstPage.skills.distinctBy { it.id.ifEmpty { it.slug } }
                    _isLoading.value = false
                    Timber.d("First page loaded: ${firstPage.skills.size} skills")

                    if (firstPage.hasNext && firstPage.nextCursor != null) {
                        _isLoadingMore.value = true
                        loadRemainingPages(firstPage.nextCursor)
                        _isLoadingMore.value = false
                    }
                }
                .onFailure {
                    _error.value = "Не удалось загрузить: ${it.localizedMessage}"
                    _isLoading.value = false
                }
        }
    }

    private suspend fun loadRemainingPages(startCursor: String) {
        var cursor: String = startCursor
        var page = 1
        val maxExtraPages = 15
        val seenCursors = mutableSetOf(startCursor)

        while (page <= maxExtraPages) {
            repo.getPage(cursor)
                .onSuccess { resp ->
                    if (resp.skills.isEmpty()) return

                    val existingKeys = _rawSkills.value.map { it.id.ifEmpty { it.slug } }.toSet()
                    val newSkills = resp.skills.filter { s ->
                        val key = s.id.ifEmpty { s.slug }
                        key.isNotEmpty() && key !in existingKeys
                    }

                    if (newSkills.isEmpty()) return

                    _rawSkills.value = _rawSkills.value + newSkills
                    Timber.d("Bg page $page: +${newSkills.size}, total=${_rawSkills.value.size}")

                    val nextCursor = resp.nextCursor
                    if (resp.hasNext && nextCursor != null && nextCursor !in seenCursors) {
                        seenCursors.add(nextCursor)
                        cursor = nextCursor
                        page++
                    } else return
                }
                .onFailure {
                    Timber.e(it, "Failed loading bg page $page")
                    return
                }
        }
    }

    fun refresh() {
        _rawSkills.value = emptyList()
        _selectedCategory.value = null
        _selectedSort.value = "downloads"
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        loadInitialData()
    }

    fun selectSort(sort: String) {
        if (_selectedSort.value == sort) return
        _selectedSort.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            // Поиск по уже загруженным скиллам
            val q = query.lowercase()
            _searchResults.value = _rawSkills.value.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.tags.any { tag -> tag.lowercase().contains(q) }
            }
            // Дополнительно ищем через API
            repo.search(query, page = 1, limit = 50)
                .onSuccess { resp ->
                    val merged = (_searchResults.value + resp.skills)
                        .distinctBy { it.id }
                    _searchResults.value = merged
                }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
    }

    fun showSkillDetails(skill: MarketplaceSkill) { _selectedSkill.value = skill }
    fun hideSkillDetails() { _selectedSkill.value = null }

    fun installSkill(skill: MarketplaceSkill) {
        val slug = skill.slug.ifEmpty { skill.id }
        viewModelScope.launch {
            _isLoading.value = true
            repo.installSkill(slug)
                .onSuccess { resp ->
                    _installMessage.value = resp.message
                        ?: "Навык ${resp.skillName.ifEmpty { skill.name }} установлен"
                    markAsInstalled(skill)
                    _selectedSkill.value = null
                }
                .onFailure { _error.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    private fun markAsInstalled(target: MarketplaceSkill) {
        val mark: (MarketplaceSkill) -> MarketplaceSkill = {
            if (it.id == target.id || it.slug == target.slug) it.copy(isInstalled = true) else it
        }
        _searchResults.value = _searchResults.value.map(mark)
        _rawSkills.value = _rawSkills.value.map(mark)
        _selectedSkill.value = _selectedSkill.value?.let(mark)
    }

    fun clearError() { _error.value = null }
    fun clearInstallMessage() { _installMessage.value = null }
}
