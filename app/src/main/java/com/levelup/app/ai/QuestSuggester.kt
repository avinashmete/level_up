package com.levelup.app.ai

import com.levelup.app.data.Difficulty
import com.levelup.app.data.MissionType
import com.levelup.app.data.StatType
import com.levelup.app.data.prefs.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Optional AI quest generator.
 *
 * Modes:
 *   - "offline" (default): no network, returns a deterministic template-based set of
 *     quests so the feature still works without a key.
 *   - "openai": calls the OpenAI chat completions API with the user's BYO key.
 *
 * The shape returned to the UI is always [QuestSuggestion], regardless of provider.
 */
class QuestSuggester(private val prefs: UserPreferences) {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun suggest(goal: String, count: Int = 5): SuggestionResult {
        val provider = prefs.aiProvider.first()
        val key = prefs.aiApiKey.first()
        val model = prefs.aiModel.first()

        return when {
            provider == "openai" && key.isNotBlank() -> runCatching {
                SuggestionResult(
                    source = "OpenAI · $model",
                    suggestions = callOpenAi(goal, count, model, key)
                )
            }.getOrElse { err ->
                SuggestionResult(
                    source = "Offline (AI call failed: ${err.message ?: "unknown"})",
                    suggestions = offlineSuggestions(goal, count)
                )
            }
            else -> SuggestionResult(
                source = "Offline template",
                suggestions = offlineSuggestions(goal, count)
            )
        }
    }

    // ---- OpenAI ----------------------------------------------------------

    private suspend fun callOpenAi(goal: String, count: Int, model: String, key: String): List<QuestSuggestion> =
        withContext(Dispatchers.IO) {
            val system = """
                You design real-life RPG quests for a self-improvement game called LevelUp.
                Output strict JSON only, no prose. Schema:
                {"quests": [
                  {
                    "title": "<short imperative>",
                    "description": "<one or two sentences>",
                    "type": "MAIN" | "SIDE" | "DAILY",
                    "stat": "STRENGTH" | "INTELLIGENCE" | "DISCIPLINE" | "VITALITY" | "SOCIAL",
                    "difficulty": "TRIVIAL" | "EASY" | "NORMAL" | "HARD" | "EPIC" | "LEGENDARY"
                  }
                ]}
                Prefer concrete, doable quests. Mix DAILY and SIDE quests, and include at most
                one MAIN quest if appropriate. Keep titles under 60 characters.
            """.trimIndent()

            val body = buildJsonObject {
                put("model", model)
                put("temperature", 0.7)
                put("response_format", buildJsonObject { put("type", "json_object") })
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system"); put("content", system)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", "Goal: $goal\nReturn exactly $count quests.")
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $key")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val raw = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(raw).jsonObject
                val content = root["choices"]
                    ?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")
                    ?.jsonObject?.get("content")
                    ?.jsonPrimitive?.content.orEmpty()
                parseQuestsJson(content).take(count)
            }
        }

    private fun parseQuestsJson(text: String): List<QuestSuggestion> {
        val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = json.parseToJsonElement(cleaned).jsonObject
        val quests = obj["quests"] as? JsonArray ?: return emptyList()
        return quests.mapNotNull { el ->
            runCatching {
                val o = el.jsonObject
                QuestSuggestion(
                    title = o["title"]!!.jsonPrimitive.content,
                    description = o["description"]?.jsonPrimitive?.content.orEmpty(),
                    type = MissionType.valueOf(o["type"]?.jsonPrimitive?.content ?: "DAILY"),
                    stat = StatType.valueOf(o["stat"]?.jsonPrimitive?.content ?: "DISCIPLINE"),
                    difficulty = Difficulty.valueOf(o["difficulty"]?.jsonPrimitive?.content ?: "NORMAL")
                )
            }.getOrNull()
        }
    }

    // ---- Offline fallback ------------------------------------------------

    /**
     * Tiny rule-based generator: keyword match -> templated quests.
     * The aim is "still useful when AI is off", not "as good as a model".
     */
    private fun offlineSuggestions(goal: String, count: Int): List<QuestSuggestion> {
        val g = goal.lowercase()
        val templates = mutableListOf<QuestSuggestion>()

        fun add(title: String, desc: String, type: MissionType, stat: StatType, diff: Difficulty) {
            templates += QuestSuggestion(title, desc, type, stat, diff)
        }

        if (g.containsAny("fit", "gym", "strong", "muscle", "weight", "workout", "run")) {
            add("Daily 20-minute workout", "Push-ups, squats, plank, or a run.", MissionType.DAILY, StatType.STRENGTH, Difficulty.NORMAL)
            add("10,000 steps today", "Hit 10k steps before you sleep.", MissionType.DAILY, StatType.VITALITY, Difficulty.EASY)
            add("Hit the gym 3x this week", "Lift heavy, get out.", MissionType.SIDE, StatType.STRENGTH, Difficulty.HARD)
            add("Run a 5K", "Anywhere, any pace.", MissionType.SIDE, StatType.VITALITY, Difficulty.HARD)
            add("Reach goal physique", "Long-arc main quest.", MissionType.MAIN, StatType.STRENGTH, Difficulty.LEGENDARY)
        }
        if (g.containsAny("learn", "study", "language", "code", "read", "skill", "japanese", "spanish")) {
            add("Study 30 minutes", "Focused, no phone.", MissionType.DAILY, StatType.INTELLIGENCE, Difficulty.NORMAL)
            add("Finish one chapter", "Of the textbook / course.", MissionType.SIDE, StatType.INTELLIGENCE, Difficulty.EASY)
            add("Build something small", "Apply what you learned.", MissionType.SIDE, StatType.INTELLIGENCE, Difficulty.HARD)
            add("Master the subject", "Reach fluency / proficiency.", MissionType.MAIN, StatType.INTELLIGENCE, Difficulty.LEGENDARY)
        }
        if (g.containsAny("focus", "discipline", "habit", "procrastin", "screen", "deep work")) {
            add("60 minutes deep work", "No phone, no tabs.", MissionType.DAILY, StatType.DISCIPLINE, Difficulty.NORMAL)
            add("Plan tomorrow tonight", "Top 3 tasks before bed.", MissionType.DAILY, StatType.DISCIPLINE, Difficulty.EASY)
            add("Phone-free morning", "First hour without screen.", MissionType.DAILY, StatType.DISCIPLINE, Difficulty.NORMAL)
        }
        if (g.containsAny("sleep", "health", "diet", "eat", "water", "meditate")) {
            add("Drink 2L of water", "Track it.", MissionType.DAILY, StatType.VITALITY, Difficulty.EASY)
            add("Lights out by 11pm", "Earlier if you can.", MissionType.DAILY, StatType.VITALITY, Difficulty.NORMAL)
            add("10-min meditation", "Breath, body, done.", MissionType.DAILY, StatType.VITALITY, Difficulty.EASY)
        }
        if (g.containsAny("social", "friend", "family", "talk", "network", "date")) {
            add("Reach out to one person", "A friend you've lost touch with.", MissionType.DAILY, StatType.SOCIAL, Difficulty.EASY)
            add("Plan something with a friend", "Schedule it this week.", MissionType.SIDE, StatType.SOCIAL, Difficulty.NORMAL)
        }

        if (templates.isEmpty()) {
            add("Take one small action toward: $goal", "Anything > nothing. Decide on the first step.", MissionType.SIDE, StatType.DISCIPLINE, Difficulty.EASY)
            add("Block 30 minutes for: $goal", "Calendar it. Show up.", MissionType.DAILY, StatType.DISCIPLINE, Difficulty.NORMAL)
            add("Define what 'done' looks like", "Write a one-line success criteria.", MissionType.SIDE, StatType.INTELLIGENCE, Difficulty.EASY)
            add("Master: $goal", "Long-arc main quest.", MissionType.MAIN, StatType.DISCIPLINE, Difficulty.LEGENDARY)
        }

        return templates.distinctBy { it.title }.take(count)
    }
}

@Serializable
data class QuestSuggestion(
    val title: String,
    val description: String,
    val type: MissionType,
    val stat: StatType,
    val difficulty: Difficulty
)

data class SuggestionResult(
    val source: String,
    val suggestions: List<QuestSuggestion>
)

private fun String.containsAny(vararg needles: String): Boolean =
    needles.any { this.contains(it) }
