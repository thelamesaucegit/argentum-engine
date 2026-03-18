/**
 * Action resolver pipeline for useInteraction.
 *
 * Crew and cycling are standalone single-phase flows handled directly here.
 * Everything else goes through the pipeline coordinator (pipelineSlice)
 * which computes the full phase sequence and advances through it.
 */
import type { EntityId, LegalActionInfo } from '@/types'
import type { CrewSelectionState } from '@/store/slices/types'
import { computePhases } from '@/store/slices/ui/pipelinePhases'

export interface ActionContext {
  selectCard: (id: EntityId | null) => void
  startCrewSelection: (state: CrewSelectionState) => void
  startPipeline: (actionInfo: LegalActionInfo) => void
}

// ---------------------------------------------------------------------------
// Standalone resolvers (not part of the pipeline)
// ---------------------------------------------------------------------------

function isCrewAction(info: LegalActionInfo): boolean {
  return (
    info.action.type === 'CrewVehicle' &&
    !!info.hasCrew &&
    !!info.validCrewCreatures &&
    info.validCrewCreatures.length > 0
  )
}

function resolveCrew(info: LegalActionInfo, ctx: ActionContext): void {
  ctx.startCrewSelection({
    actionInfo: info,
    vehicleName: info.description.replace('Crew ', ''),
    crewPower: info.crewPower ?? 0,
    selectedCreatures: [],
    validCreatures: info.validCrewCreatures!,
  })
  ctx.selectCard(null)
}

function isCyclingAction(info: LegalActionInfo): boolean {
  return info.action.type === 'CycleCard' || info.action.type === 'TypecycleCard'
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Route an action through the appropriate handler. Returns true if the action
 * was handled (caller should not submit directly).
 */
export function resolveAction(actionInfo: LegalActionInfo, ctx: ActionContext): boolean {
  // Crew and cycling are standalone — not part of the pipeline
  if (isCrewAction(actionInfo)) {
    resolveCrew(actionInfo, ctx)
    return true
  }
  if (isCyclingAction(actionInfo)) {
    return true
  }

  // Everything else goes through the pipeline coordinator.
  // computePhases inside startPipeline decides what UI phases are needed;
  // if none, startPipeline submits directly.
  ctx.startPipeline(actionInfo)
  return true
}

/**
 * Returns true if the action requires interaction (selection UI) before it can
 * be submitted. Used by canAutoExecute / handleDoubleClick.
 */
export function needsInteraction(actionInfo: LegalActionInfo): boolean {
  if (isCrewAction(actionInfo)) return true
  if (isCyclingAction(actionInfo)) return true
  return computePhases(actionInfo).length > 0
}
