package com.oorep.android.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.oorep.android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(private val context: Context) {

    private val dbName = "oorep.db"
    private var db: SQLiteDatabase? = null

    init {
        ensureDatabaseCopied()
    }

    private fun ensureDatabaseCopied() {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            try {
                context.assets.open("databases/$dbName").use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    private fun getReadableDb(): SQLiteDatabase {
        if (db == null || !db!!.isOpen) {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) {
                ensureDatabaseCopied()
            }
            db = if (dbFile.exists()) {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
            } else {
                context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
            }
            createCaseTablesIfNeeded(db!!)
        }
        return db!!
    }

    private fun createCaseTablesIfNeeded(database: SQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS caze (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                header TEXT NOT NULL,
                date_ TEXT NOT NULL,
                description TEXT
            );
        """)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS cazerubric (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cazeid INTEGER NOT NULL,
                abbrev TEXT NOT NULL,
                rubricid INTEGER NOT NULL,
                weight INTEGER DEFAULT 1,
                is_eliminating INTEGER DEFAULT 0,
                label TEXT
            );
        """)
    }

    suspend fun searchRubrics(query: String, repertory: String?, chapterId: Int?, limit: Int = 60): List<Rubric> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<Rubric>()
            if (query.isBlank() && chapterId == null) return@withContext list

            val terms = query.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            val whereClauses = mutableListOf<String>()
            val args = mutableListOf<String>()

            for (term in terms) {
                whereClauses.add("fullpath LIKE ?")
                args.add("%$term%")
            }

            if (!repertory.isNullOrBlank()) {
                whereClauses.add("abbrev = ?")
                args.add(repertory)
            }

            if (chapterId != null && chapterId >= 0) {
                whereClauses.add("chapterid = ?")
                args.add(chapterId.toString())
            }

            val whereSql = if (whereClauses.isNotEmpty()) "WHERE " + whereClauses.joinToString(" AND ") else ""
            val sql = "SELECT abbrev, id, mother, ismother, chapterid, fullpath, path FROM rubric $whereSql LIMIT $limit"

            val database = getReadableDb()
            database.rawQuery(sql, args.toTypedArray()).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        Rubric(
                            abbrev = cursor.getString(0),
                            id = cursor.getInt(1),
                            mother = if (cursor.isNull(2)) null else cursor.getInt(2),
                            isMother = cursor.getInt(3) == 1,
                            chapterId = if (cursor.isNull(4)) null else cursor.getInt(4),
                            fullPath = cursor.getString(5) ?: "",
                            path = cursor.getString(6) ?: ""
                        )
                    )
                }
            }
            list
        }

    suspend fun getRemediesForRubric(abbrev: String, rubricId: Int): List<RemedyGrade> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<RemedyGrade>()
            val sql = """
                SELECT r.id, r.nameabbrev, r.namelong, rr.weight
                FROM rubricremedy rr
                JOIN remedy r ON rr.remedyid = r.id
                WHERE rr.abbrev = ? AND rr.rubricid = ?
                ORDER BY rr.weight DESC, r.nameabbrev ASC
            """
            val database = getReadableDb()
            database.rawQuery(sql, arrayOf(abbrev, rubricId.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        RemedyGrade(
                            remedyId = cursor.getInt(0),
                            nameAbbrev = cursor.getString(1),
                            nameLong = cursor.getString(2),
                            weight = cursor.getInt(3)
                        )
                    )
                }
            }
            list
        }

    suspend fun getChapters(repertory: String = "publicum"): List<Chapter> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<Chapter>()
            val sql = "SELECT abbrev, id, name FROM chapter WHERE abbrev = ? ORDER BY name ASC"
            val database = getReadableDb()
            database.rawQuery(sql, arrayOf(repertory)).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        Chapter(
                            abbrev = cursor.getString(0),
                            id = cursor.getInt(1),
                            name = cursor.getString(2)
                        )
                    )
                }
            }
            list
        }

    suspend fun getMateriaMedicaChapters(filter: String = ""): List<MMChapter> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<MMChapter>()
            val sql = if (filter.isBlank()) {
                "SELECT id, heading, remedy_id FROM mmchapter ORDER BY heading ASC LIMIT 100"
            } else {
                "SELECT id, heading, remedy_id FROM mmchapter WHERE heading LIKE ? ORDER BY heading ASC LIMIT 100"
            }
            val args = if (filter.isBlank()) emptyArray() else arrayOf("%$filter%")
            val database = getReadableDb()
            database.rawQuery(sql, args).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        MMChapter(
                            id = cursor.getInt(0),
                            heading = cursor.getString(1),
                            remedyId = if (cursor.isNull(2)) null else cursor.getInt(2)
                        )
                    )
                }
            }
            list
        }

    suspend fun getMMSections(mmChapterId: Int): List<MMSection> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<MMSection>()
            val sql = "SELECT id, heading, content FROM mmsection WHERE mmchapter_id = ? ORDER BY id ASC"
            val database = getReadableDb()
            database.rawQuery(sql, arrayOf(mmChapterId.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        MMSection(
                            id = cursor.getInt(0),
                            heading = cursor.getString(1) ?: "",
                            content = cursor.getString(2) ?: ""
                        )
                    )
                }
            }
            list
        }

    // --- Case Management & Repertorization ---

    suspend fun getOrCreateDefaultCase(): PatientCase = withContext(Dispatchers.IO) {
        val database = getReadableDb()
        database.rawQuery("SELECT id, header, date_, description FROM caze ORDER BY id DESC LIMIT 1", null).use { cursor ->
            if (cursor.moveToNext()) {
                return@withContext PatientCase(
                    id = cursor.getLong(0),
                    header = cursor.getString(1),
                    date = cursor.getString(2),
                    description = cursor.getString(3)
                )
            }
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cv = ContentValues().apply {
            put("header", "New Clinical Case")
            put("date_", dateStr)
            put("description", "Active patient consultation")
        }
        val id = database.insert("caze", null, cv)
        PatientCase(id, "New Clinical Case", dateStr, "Active patient consultation")
    }

    suspend fun getCaseRubrics(cazeId: Long): List<CaseRubric> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CaseRubric>()
        val sql = "SELECT id, cazeid, abbrev, rubricid, weight, is_eliminating, label FROM cazerubric WHERE cazeid = ? ORDER BY id ASC"
        val database = getReadableDb()
        database.rawQuery(sql, arrayOf(cazeId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    CaseRubric(
                        id = cursor.getLong(0),
                        cazeId = cursor.getLong(1),
                        abbrev = cursor.getString(2),
                        rubricId = cursor.getInt(3),
                        weight = cursor.getInt(4),
                        isEliminating = cursor.getInt(5) == 1,
                        label = cursor.getString(6) ?: ""
                    )
                )
            }
        }
        list
    }

    suspend fun addRubricToCase(cazeId: Long, rubric: Rubric, weight: Int = 1): Long = withContext(Dispatchers.IO) {
        val database = getReadableDb()
        val cv = ContentValues().apply {
            put("cazeid", cazeId)
            put("abbrev", rubric.abbrev)
            put("rubricid", rubric.id)
            put("weight", weight)
            put("is_eliminating", 0)
            put("label", rubric.fullPath)
        }
        database.insert("cazerubric", null, cv)
    }

    suspend fun deleteCaseRubric(id: Long) = withContext(Dispatchers.IO) {
        getReadableDb().delete("cazerubric", "id = ?", arrayOf(id.toString()))
    }

    suspend fun updateCaseRubric(id: Long, weight: Int, isEliminating: Boolean) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("weight", weight)
            put("is_eliminating", if (isEliminating) 1 else 0)
        }
        getReadableDb().update("cazerubric", cv, "id = ?", arrayOf(id.toString()))
    }

    suspend fun clearCaseRubrics(cazeId: Long) = withContext(Dispatchers.IO) {
        getReadableDb().delete("cazerubric", "cazeid = ?", arrayOf(cazeId.toString()))
    }
}
