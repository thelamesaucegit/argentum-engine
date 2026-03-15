package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/**
 * Static hexproof from one or more colors (Rule 702.11b).
 * Attached to permanents/cards that have innate hexproof from a color
 * (e.g., Knight of Malice has hexproof from white).
 *
 * "Hexproof from [quality]" means "This permanent can't be the target of
 * [quality] spells or abilities your opponents control."
 */
@Serializable
data class HexproofFromColorComponent(val colors: Set<Color>) : Component
