package com.dndsheet.app.ui.character.edit

import com.dndsheet.domain.enums.Ability

/**
 * Which (if any) edit dialog is currently being shown. Encoding it as a
 * sealed interface gives us:
 *   - exhaustive `when` rendering in the screen,
 *   - one piece of state to drive the entire dialog layer,
 *   - per-dialog data carried alongside (no scattered "is this dialog open?"
 *     booleans + "what value is it editing?" mutables).
 */
sealed interface ActiveDialog {
    data object None : ActiveDialog

    data object EditName : ActiveDialog
    data object EditSpecies : ActiveDialog
    data object EditBackground : ActiveDialog
    data object EditAlignment : ActiveDialog
    data object EditRuleset : ActiveDialog

    data object EditHp : ActiveDialog
    /** Quick HP adjustment outside edit mode. [isHeal] flips sign; the dialog itself is the same. */
    data class AdjustHp(val isHeal: Boolean) : ActiveDialog
    /** Receive / clear temporary HP. Available outside edit mode like [AdjustHp]. */
    data object AddTempHp : ActiveDialog
    data class EditAbility(val ability: Ability) : ActiveDialog

    data object AddClass : ActiveDialog
}
