package com.nobadhabbits.cornytask.features.medat

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate

/** The MedAT test day. Change this for another year. */
val MEDAT_DATE: LocalDate = LocalDate.of(2027, 7, 9)

/** When the plan should be finished: a month before the test, to leave room for revision. */
val MEDAT_TARGET_DATE: LocalDate = MEDAT_DATE.minusMonths(1)

/** Unicorns for finishing [minutes] of studying: 5 per hour, rounded, at least 1. */
fun medatCoinsFor(minutes: Int): Int = maxOf(1, Math.round(minutes * 5 / 60.0).toInt())

const val MEDAT_PLAN_DAYS = 111

enum class MedatBlock(val label: String) {
    NEU("BMS · new topics"),
    WDH("BMS · review"),
    KFF("KFF · subtests")
}

/** One thing to do from the studymed 111-day plan, e.g. "ZF » Training". */
data class MedatPlanItem(
    val id: String,
    val day: Int,
    val block: MedatBlock,
    val topic: String,
    val title: String,
    val minutes: Int
) {
    /** Unicorns for ticking it off. */
    val coins: Int get() = medatCoinsFor(minutes)

    /** "Zahlenfolgen · Training", "Die Zelle · Zellmembran", "Wiederholung · Tag 1 und 3". */
    val displayTitle: String
        get() {
            val topicName = SUBTEST_NAMES[topic] ?: if (topic == "Wdh") "Wiederholung" else topic
            return if (title.isBlank()) topicName else "$topicName · $title"
        }

    companion object {
        val SUBTEST_NAMES = mapOf(
            "TV" to "Textverständnis",
            "FZ" to "Figuren zusammensetzen",
            "GM" to "Gedächtnis und Merkfähigkeit",
            "ZF" to "Zahlenfolgen",
            "WF" to "Wortflüssigkeit",
            "IE" to "Implikationen erkennen",
            "ER" to "Emotionen regulieren",
            "EE" to "Emotionen erkennen",
            "SE" to "Soziales Entscheiden"
        )
    }
}

data class MedatPlanDay(
    val day: Int,
    val isSuperDay: Boolean,
    val items: List<MedatPlanItem>
)

/** Reads the plan bundled in assets (generated from the studymed PDF). */
object MedatPlan {
    @Volatile
    private var cached: List<MedatPlanDay>? = null

    fun load(context: Context): List<MedatPlanDay> = cached ?: synchronized(this) {
        cached ?: parse(
            context.assets.open("medat_plan.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        ).also { cached = it }
    }

    private fun parse(json: String): List<MedatPlanDay> {
        val days = JSONArray(json)
        return (0 until days.length()).map { d ->
            val dayJson = days.getJSONObject(d)
            val day = dayJson.getInt("day")
            val items = dayJson.getJSONArray("items")
            MedatPlanDay(
                day = day,
                isSuperDay = dayJson.getBoolean("super"),
                items = (0 until items.length()).map { i ->
                    val item = items.getJSONObject(i)
                    MedatPlanItem(
                        id = item.getString("id"),
                        day = day,
                        block = MedatBlock.valueOf(item.getString("block")),
                        topic = item.getString("topic"),
                        title = item.getString("title"),
                        minutes = item.getInt("minutes")
                    )
                }
            )
        }
    }
}
