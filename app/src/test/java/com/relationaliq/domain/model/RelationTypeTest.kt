package com.relationaliq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationTypeTest {

    @Test
    fun `inverse of MORE_THAN is LESS_THAN`() {
        assertEquals(RelationType.LESS_THAN, RelationType.MORE_THAN.inverse())
    }

    @Test
    fun `inverse of LESS_THAN is MORE_THAN`() {
        assertEquals(RelationType.MORE_THAN, RelationType.LESS_THAN.inverse())
    }

    @Test
    fun `inverse of BEFORE is AFTER`() {
        assertEquals(RelationType.AFTER, RelationType.BEFORE.inverse())
    }

    @Test
    fun `inverse of AFTER is BEFORE`() {
        assertEquals(RelationType.BEFORE, RelationType.AFTER.inverse())
    }

    @Test
    fun `inverse of CONTAINS is WITHIN`() {
        assertEquals(RelationType.WITHIN, RelationType.CONTAINS.inverse())
    }

    @Test
    fun `inverse of WITHIN is CONTAINS`() {
        assertEquals(RelationType.CONTAINS, RelationType.WITHIN.inverse())
    }

    @Test
    fun `symmetric relations are their own inverse`() {
        assertEquals(RelationType.SAME, RelationType.SAME.inverse())
        assertEquals(RelationType.DIFFERENT, RelationType.DIFFERENT.inverse())
        assertEquals(RelationType.OPPOSITE, RelationType.OPPOSITE.inverse())
    }

    @Test
    fun `SAME, DIFFERENT, OPPOSITE are symmetric`() {
        assertTrue(RelationType.SAME.isSymmetric())
        assertTrue(RelationType.DIFFERENT.isSymmetric())
        assertTrue(RelationType.OPPOSITE.isSymmetric())
    }

    @Test
    fun `asymmetric relations are not symmetric`() {
        assertFalse(RelationType.MORE_THAN.isSymmetric())
        assertFalse(RelationType.LESS_THAN.isSymmetric())
        assertFalse(RelationType.BEFORE.isSymmetric())
        assertFalse(RelationType.AFTER.isSymmetric())
        assertFalse(RelationType.CONTAINS.isSymmetric())
        assertFalse(RelationType.WITHIN.isSymmetric())
    }

    @Test
    fun `double inverse returns original relation`() {
        for (relation in RelationType.entries) {
            assertEquals(relation, relation.inverse().inverse())
        }
    }

    @Test
    fun `all relation types have display names`() {
        for (relation in RelationType.entries) {
            assertTrue(relation.displayName.isNotBlank())
        }
    }

    @Test
    fun `all relation types have symbols`() {
        for (relation in RelationType.entries) {
            assertTrue(relation.symbol.isNotBlank())
        }
    }
}
