package com.wingedsheep.sdk.scripting.text

import com.wingedsheep.sdk.core.Subtype

/**
 * Abstraction for performing text replacements on game data.
 *
 * Provided by the engine's TextReplacementComponent to the SDK types.
 * This allows SDK data classes to handle their own text replacement
 * without depending on engine internals.
 */
interface TextReplacer {
    /** Replace a creature type string (e.g., "Elf" → "Goblin"). */
    fun replaceCreatureType(subtype: String): String

    /** Replace a Subtype value. */
    fun replaceSubtype(subtype: Subtype): Subtype
}

/**
 * Marker interface for SDK types that can have their text replaced
 * by Layer 3 text-changing effects (e.g., Artificial Evolution).
 *
 * Every concrete implementation of a sealed interface that extends this
 * MUST implement [applyTextReplacement]. The Kotlin compiler enforces this,
 * guaranteeing that new types cannot silently skip text replacement.
 */
interface TextReplaceable<T> {
    /**
     * Returns a copy of this object with creature type text replaced,
     * or `this` if no replacements apply.
     */
    fun applyTextReplacement(replacer: TextReplacer): T
}
