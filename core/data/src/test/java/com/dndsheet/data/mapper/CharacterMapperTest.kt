package com.dndsheet.data.mapper

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.Alignment
import com.dndsheet.domain.enums.DamageType
import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.enums.Ruleset
import com.dndsheet.domain.enums.Skill
import com.dndsheet.domain.model.AbilityScores
import com.dndsheet.domain.model.BoxPosition
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.model.ClassLevel
import com.dndsheet.domain.model.InventoryItem
import com.dndsheet.domain.model.ManualOverrides
import com.dndsheet.domain.model.Proficiencies
import com.dndsheet.domain.model.SheetLayout
import com.dndsheet.domain.model.Spell
import com.dndsheet.domain.model.SpellcastingProgression
import com.dndsheet.domain.model.Weapon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mapper has no business logic — its job is to round-trip without losing
 * anything. So every test here builds a domain Character, maps to entity,
 * maps back, and asserts equality. If a new field gets added to Character
 * that doesn't survive the trip, one of these tests will catch it.
 */
class CharacterMapperTest {

    @Test fun `default character round-trips`() {
        val original = Character(name = "Test")
        val restored = CharacterMapper.toDomain(CharacterMapper.toEntity(original))
        assertEquals(original, restored)
    }

    @Test fun `top-level columns match domain fields`() {
        val original = Character(
            name = "Aria",
            ruleset = Ruleset.DND_5E_2014,
            classes = listOf(
                ClassLevel("Wizard", 5, hitDie = 6,
                    progression = SpellcastingProgression.FULL,
                    spellcastingAbility = Ability.INT)
            ),
            updatedAt = 1_700_000_000_000L,
            revision = 7
        )
        val entity = CharacterMapper.toEntity(original)
        assertEquals("Aria", entity.name)
        assertEquals("DND_5E_2014", entity.ruleset)
        assertEquals(5, entity.totalLevel)
        assertEquals(1_700_000_000_000L, entity.updatedAt)
        assertEquals(7L, entity.revision)
        assertNotNull(entity.json)
        assertTrue(entity.json.contains("Aria"))
    }

    @Test fun `fully populated character round-trips without losing fields`() {
        val original = fullyLoadedCharacter()
        val restored = CharacterMapper.toDomain(CharacterMapper.toEntity(original))
        assertEquals(original, restored)
    }

    @Test fun `unknown JSON field is ignored on read (forward compat)`() {
        val original = Character(name = "Future")
        val baseEntity = CharacterMapper.toEntity(original)
        // Inject a field that doesn't exist on Character. ignoreUnknownKeys
        // should make this a no-op rather than a crash.
        val mutated = baseEntity.copy(
            json = baseEntity.json.replaceFirst("{", """{"futureField":42,""")
        )
        val restored = CharacterMapper.toDomain(mutated)
        // Original equals restored — the unknown field was dropped silently.
        assertEquals(original, restored)
    }

    @Test fun `multiclass total level is reflected in top-level column`() {
        val original = Character(
            name = "Multi",
            classes = listOf(
                ClassLevel("Cleric", 3, hitDie = 8,
                    progression = SpellcastingProgression.FULL,
                    spellcastingAbility = Ability.WIS),
                ClassLevel("Paladin", 2, hitDie = 10,
                    progression = SpellcastingProgression.HALF,
                    spellcastingAbility = Ability.CHA)
            )
        )
        val entity = CharacterMapper.toEntity(original)
        assertEquals(5, entity.totalLevel)
    }

    @Test fun `manual overrides survive round trip`() {
        val original = Character(
            name = "Pinned",
            overrides = ManualOverrides(
                abilityModifiers = mapOf(Ability.STR to 6),
                proficiencyBonus = 4,
                skillBonuses = mapOf(Skill.STEALTH to 12),
                saveBonuses = mapOf(Ability.DEX to 9),
                passivePerception = 22,
                weaponAttackBonus = mapOf("Longsword" to 99)
            )
        )
        val restored = CharacterMapper.toDomain(CharacterMapper.toEntity(original))
        assertEquals(original.overrides, restored.overrides)
    }

    private fun fullyLoadedCharacter(): Character = Character(
        id = "test-id",
        name = "Eltharion",
        species = "Elf",
        background = "Acolyte",
        alignment = Alignment.CHAOTIC_GOOD,
        ruleset = Ruleset.DND_5E_2024,

        classes = listOf(
            ClassLevel("Wizard", 4, subclass = "Evocation", hitDie = 6,
                progression = SpellcastingProgression.FULL,
                spellcastingAbility = Ability.INT),
            ClassLevel("Cleric", 1, subclass = "Light", hitDie = 8,
                progression = SpellcastingProgression.FULL,
                spellcastingAbility = Ability.WIS)
        ),

        abilityScores = AbilityScores(
            strength = 10, dexterity = 14, constitution = 13,
            intelligence = 17, wisdom = 12, charisma = 8
        ),

        proficiencies = Proficiencies(
            skills = mapOf(
                Skill.ARCANA to ProficiencyLevel.EXPERTISE,
                Skill.HISTORY to ProficiencyLevel.PROFICIENT,
                Skill.PERCEPTION to ProficiencyLevel.HALF
            ),
            saves = mapOf(
                Ability.INT to ProficiencyLevel.PROFICIENT,
                Ability.WIS to ProficiencyLevel.PROFICIENT
            ),
            armor = listOf("Light Armor"),
            weapons = listOf("Daggers", "Quarterstaffs"),
            tools = listOf("Calligrapher's Supplies"),
            languages = listOf("Common", "Elvish", "Draconic")
        ),

        weapons = listOf(
            Weapon(
                name = "Quarterstaff",
                damageDice = "1d6",
                damageType = DamageType.BLUDGEONING,
                versatileDice = "1d8"
            ),
            Weapon(
                name = "Dagger of Venom",
                damageDice = "1d4",
                damageType = DamageType.PIERCING,
                isFinesse = true,
                magicBonus = 1
            )
        ),

        spells = listOf(
            Spell(name = "Fire Bolt", level = 0, sourceClassName = "Wizard"),
            Spell(name = "Magic Missile", level = 1, sourceClassName = "Wizard",
                isPrepared = true),
            Spell(name = "Sacred Flame", level = 0, sourceClassName = "Cleric")
        ),

        inventory = listOf(
            InventoryItem(name = "Potion of Healing", quantity = 3, weightLbs = 0.5),
            InventoryItem(name = "Spellbook", weightLbs = 3.0, isEquipped = true)
        ),

        maxHp = 32,
        currentHp = 28,
        temporaryHp = 4,
        hitDiceRemaining = mapOf(6 to 4, 8 to 1),

        experience = 6500,
        inspiration = true,
        conditions = listOf("Blessed"),
        notes = "Looking for the lost tome.",

        overrides = ManualOverrides(
            spellSaveDc = mapOf("Wizard" to 14)
        ),

        layout = SheetLayout(
            positions = mapOf(
                "VITALS_HP" to BoxPosition(x = 12f, y = 40f, width = 120f, height = 140f, z = 2),
                "ABILITY_STR" to BoxPosition(x = 0f, y = 200f)
            )
        ),

        revision = 12,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_001_000_000L
    )
}
