package com.oorep.android.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.oorep.android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(private val context: Context) {

    private val dbName = "oorep.db"
    private var db: SQLiteDatabase? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _initProgress = MutableStateFlow(0f)
    val initProgress: StateFlow<Float> = _initProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow("Preparing database...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        if (_isReady.value) return@withContext

        val dbFile = context.getDatabasePath(dbName)
        if (!isDatabaseValid(dbFile)) {
            _statusMessage.value = "Unpacking offline repertory dataset..."
            copyDatabaseFromAssets(dbFile)
        }

        _statusMessage.value = "Opening database..."
        try {
            db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
            createCaseTablesIfNeeded(db!!)
            _isReady.value = true
            _statusMessage.value = "Ready"
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
                createCaseTablesIfNeeded(db!!)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            _isReady.value = true
        }
    }

    private fun isDatabaseValid(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() < 10_000_000) return false
        return try {
            val testDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = testDb.rawQuery("SELECT 1 FROM rubric LIMIT 1", null)
            val valid = cursor.moveToFirst()
            cursor.close()
            testDb.close()
            valid
        } catch (e: Exception) {
            false
        }
    }

    private fun copyDatabaseFromAssets(dbFile: File) {
        dbFile.parentFile?.mkdirs()
        val tempFile = File(dbFile.parentFile, "$dbName.tmp")
        if (tempFile.exists()) tempFile.delete()

        try {
            val totalBytes = try {
                context.assets.openFd("databases/$dbName").length
            } catch (e: Exception) {
                138_000_000L
            }

            context.assets.open("databases/$dbName").use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    var bytesCopied = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            _initProgress.value = (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                    output.flush()
                }
            }
            if (dbFile.exists()) dbFile.delete()
            tempFile.renameTo(dbFile)
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
        }
    }

    @Synchronized
    private fun getReadableDb(): SQLiteDatabase? {
        if (db == null || !db!!.isOpen) {
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) {
                db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
            }
        }
        return db
    }

    private fun createCaseTablesIfNeeded(database: SQLiteDatabase) {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchRubrics(query: String, repertory: String?, chapterId: Int?, limit: Int = 60): List<Rubric> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<Rubric>()
            if (query.isBlank() && chapterId == null) return@withContext list

            try {
                val database = getReadableDb() ?: return@withContext list
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }

    suspend fun getRemediesForRubric(abbrev: String, rubricId: Int): List<RemedyGrade> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<RemedyGrade>()
            try {
                val database = getReadableDb() ?: return@withContext list
                val sql = """
                    SELECT r.id, r.nameabbrev, r.namelong, rr.weight
                    FROM rubricremedy rr
                    JOIN remedy r ON rr.remedyid = r.id
                    WHERE rr.abbrev = ? AND rr.rubricid = ?
                    ORDER BY rr.weight DESC, r.nameabbrev ASC
                """
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }

    suspend fun getChapters(repertory: String = "publicum"): List<Chapter> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<Chapter>()
            try {
                val database = getReadableDb() ?: return@withContext list
                val sql = "SELECT abbrev, id, name FROM chapter WHERE abbrev = ? ORDER BY name ASC"
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }

    suspend fun getMateriaMedicaChapters(filter: String = ""): List<MMChapter> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<MMChapter>()
            try {
                val database = getReadableDb() ?: return@withContext list
                val sql = if (filter.isBlank()) {
                    "SELECT id, heading, remedy_id FROM mmchapter ORDER BY heading ASC LIMIT 100"
                } else {
                    "SELECT id, heading, remedy_id FROM mmchapter WHERE heading LIKE ? ORDER BY heading ASC LIMIT 100"
                }
                val args = if (filter.isBlank()) emptyArray() else arrayOf("%$filter%")
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }

    suspend fun getMMSections(mmChapterId: Int): List<MMSection> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<MMSection>()
            try {
                val database = getReadableDb() ?: return@withContext list
                val sql = "SELECT id, heading, content FROM mmsection WHERE mmchapter_id = ? ORDER BY id ASC"
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }

    // --- Case Management & Repertorization ---

    suspend fun getOrCreateDefaultCase(): PatientCase = withContext(Dispatchers.IO) {
        val database = getReadableDb()
        if (database != null) {
            try {
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
                return@withContext PatientCase(id, "New Clinical Case", dateStr, "Active patient consultation")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        PatientCase(1L, "Default Case", "2026-01-01", null)
    }

    suspend fun getCaseRubrics(cazeId: Long): List<CaseRubric> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CaseRubric>()
        try {
            val database = getReadableDb() ?: return@withContext list
            val sql = "SELECT id, cazeid, abbrev, rubricid, weight, is_eliminating, label FROM cazerubric WHERE cazeid = ? ORDER BY id ASC"
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun addRubricToCase(cazeId: Long, rubric: Rubric, weight: Int = 1): Long = withContext(Dispatchers.IO) {
        try {
            val database = getReadableDb() ?: return@withContext -1L
            val cv = ContentValues().apply {
                put("cazeid", cazeId)
                put("abbrev", rubric.abbrev)
                put("rubricid", rubric.id)
                put("weight", weight)
                put("is_eliminating", 0)
                put("label", rubric.fullPath)
            }
            database.insert("cazerubric", null, cv)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    suspend fun deleteCaseRubric(id: Long) = withContext(Dispatchers.IO) {
        try {
            getReadableDb()?.delete("cazerubric", "id = ?", arrayOf(id.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateCaseRubric(id: Long, weight: Int, isEliminating: Boolean) = withContext(Dispatchers.IO) {
        try {
            val cv = ContentValues().apply {
                put("weight", weight)
                put("is_eliminating", if (isEliminating) 1 else 0)
            }
            getReadableDb()?.update("cazerubric", cv, "id = ?", arrayOf(id.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearCaseRubrics(cazeId: Long) = withContext(Dispatchers.IO) {
        try {
            getReadableDb()?.delete("cazerubric", "cazeid = ?", arrayOf(cazeId.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
