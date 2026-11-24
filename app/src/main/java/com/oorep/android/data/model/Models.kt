package com.oorep.android.data.model

data class Rubric(
    val abbrev: String,
    val id: Int,
    val mother: Int?,
    val isMother: Boolean,
    val chapterId: Int?,
    val fullPath: String,
    val path: String
)

data class Remedy(
    val id: Int,
    val nameAbbrev: String,
    val nameLong: String?,
    val nameAlt: String?
)

data class RemedyGrade(
    val remedyId: Int,
    val nameAbbrev: String,
    val nameLong: String?,
    val weight: Int
)

data class Chapter(
    val abbrev: String,
    val id: Int,
    val name: String
)

data class MMChapter(
    val id: Int,
    val heading: String,
    val remedyId: Int?
)

data class MMSection(
    val id: Int,
    val heading: String,
    val content: String
)

data class PatientCase(
    val id: Long,
    val header: String,
    val date: String,
    val description: String?
)

data class CaseRubric(
    val id: Long = 0,
    val cazeId: Long,
    val abbrev: String,
    val rubricId: Int,
    val weight: Int = 1,
    val isEliminating: Boolean = false,
    val label: String
)

data class RepertorizationScore(
    val remedyId: Int,
    val nameAbbrev: String,
    val nameLong: String?,
    val totalScore: Int,
    val coveredRubricsCount: Int,
    val coveredRubricIds: List<Int>
)
