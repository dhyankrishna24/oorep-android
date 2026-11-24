package com.oorep.android.domain

import com.oorep.android.data.db.DatabaseHelper
import com.oorep.android.data.model.CaseRubric
import com.oorep.android.data.model.RepertorizationScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepertorizationEngine(private val dbHelper: DatabaseHelper) {

    suspend fun repertorize(caseRubrics: List<CaseRubric>): List<RepertorizationScore> = withContext(Dispatchers.Default) {
        if (caseRubrics.isEmpty()) return@withContext emptyList()

        // remedyId -> (score, coveredRubricIds)
        val scoreMap = mutableMapOf<Int, Int>()
        val coverageMap = mutableMapOf<Int, MutableList<Int>>()
        val nameAbbrevMap = mutableMapOf<Int, String>()
        val nameLongMap = mutableMapOf<Int, String?>()

        val eliminatingRubrics = caseRubrics.filter { it.isEliminating }
        val eliminatingRubricIds = eliminatingRubrics.map { it.rubricId }.toSet()

        for (caseRubric in caseRubrics) {
            val remedies = dbHelper.getRemediesForRubric(caseRubric.abbrev, caseRubric.rubricId)
            for (rg in remedies) {
                nameAbbrevMap[rg.remedyId] = rg.nameAbbrev
                nameLongMap[rg.remedyId] = rg.nameLong

                val currentScore = scoreMap.getOrDefault(rg.remedyId, 0)
                scoreMap[rg.remedyId] = currentScore + (caseRubric.weight * rg.weight)

                val covered = coverageMap.getOrPut(rg.remedyId) { mutableListOf() }
                covered.add(caseRubric.rubricId)
            }
        }

        val results = mutableListOf<RepertorizationScore>()
        for ((remedyId, score) in scoreMap) {
            val coveredIds = coverageMap[remedyId] ?: emptyList()

            // Filter out remedies that don't cover all eliminating symptoms
            if (eliminatingRubrics.isNotEmpty()) {
                val coversAllEliminating = eliminatingRubricIds.all { it in coveredIds }
                if (!coversAllEliminating) continue
            }

            results.add(
                RepertorizationScore(
                    remedyId = remedyId,
                    nameAbbrev = nameAbbrevMap[remedyId] ?: "Unknown",
                    nameLong = nameLongMap[remedyId],
                    totalScore = score,
                    coveredRubricsCount = coveredIds.size,
                    coveredRubricIds = coveredIds
                )
            )
        }

        // Sort: Most rubrics covered first, then highest total score
        results.sortedWith(
            compareByDescending<RepertorizationScore> { it.coveredRubricsCount }
                .thenByDescending { it.totalScore }
                .thenBy { it.nameAbbrev }
        )
    }
}
