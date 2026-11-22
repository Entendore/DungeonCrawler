package com.yourpackage.dungeoncrawler

enum class NarrativeEvent { ROOM_ENTER, ENEMY_SPOTTED, PLAYER_ATTACK_HIT, PLAYER_ATTACK_MISS, ENEMY_ATTACK_HIT, ENEMY_ATTACK_MISS, ENEMY_DIED, LOW_HP_WARNING, LEVEL_TRANSITION, EXIT_REACHED, VICTORY, DEFEAT, IDLE_WHISPER }

data class NarrativeLine(val text: String, val style: Style = Style.NORMAL) {
    enum class Style { NORMAL, DANGER, TRIUMPH, MYSTIC, COMBAT }
}

class NarrativeEngine {
    private val adjectives = listOf("musty","oppressive","eerie","crumbling","bloodstained","silent","ancient","twisted")
    private val rooms = listOf("chamber","hall","corridor","crypt","vault","pit","sanctum","passage")
    private val sounds = listOf("dripping water","distant chanting","skittering claws","a low growl","echoing footsteps")
    private val templates: Map<NarrativeEvent, List<Pair<Int, String>>> = mapOf(
        NarrativeEvent.ROOM_ENTER to listOf(5 to "You step into a {adjective} {room}.", 3 to "The {adjective} {room} opens before you."),
        NarrativeEvent.ENEMY_DIED to listOf(4 to "The {enemy} collapses.", 3 to "With a final gasp, the {enemy} falls."),
        NarrativeEvent.PLAYER_ATTACK_HIT to listOf(4 to "Your strike hits the {enemy} for {damage} damage."),
        NarrativeEvent.ENEMY_ATTACK_HIT to listOf(4 to "The {enemy} strikes you for {damage} damage!"),
        NarrativeEvent.LEVEL_TRANSITION to listOf(4 to "You descend deeper. Level {level} awaits."),
        NarrativeEvent.LOW_HP_WARNING to listOf(4 to "Your vision swims. You are badly wounded."),
        NarrativeEvent.IDLE_WHISPER to listOf(3 to "The walls seem to whisper your name.", 2 to "Something watches from the dark.")
    )
    
    fun generate(event: NarrativeEvent, context: Map<String, String> = emptyMap()): NarrativeLine {
        val pool = templates[event] ?: return NarrativeLine("...")
        val total = pool.sumOf { it.first }; var roll = kotlin.random.Random.nextInt(total)
        var template = pool.last().second
        for ((weight, t) in pool) { roll -= weight; if (roll < 0) { template = t; break } }
        val defaults = mapOf("adjective" to adjectives.random(), "room" to rooms.random(), "sound" to sounds.random(), "enemy" to (context["enemy"] ?: "creature"), "damage" to (context["damage"] ?: "?"), "level" to (context["level"] ?: "1"))
        var result = template; for ((key, value) in defaults + context) result = result.replace("{$key}", value)
        val style = when (event) { NarrativeEvent.LOW_HP_WARNING, NarrativeEvent.DEFEAT -> NarrativeLine.Style.DANGER; NarrativeEvent.VICTORY, NarrativeEvent.EXIT_REACHED, NarrativeEvent.ENEMY_DIED -> NarrativeLine.Style.TRIUMPH; NarrativeEvent.ROOM_ENTER, NarrativeEvent.LEVEL_TRANSITION, NarrativeEvent.IDLE_WHISPER -> NarrativeLine.Style.MYSTIC; else -> NarrativeLine.Style.COMBAT }
        return NarrativeLine(result, style)
    }
}
