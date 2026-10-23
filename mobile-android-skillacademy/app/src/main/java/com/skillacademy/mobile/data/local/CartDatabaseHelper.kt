package com.skillacademy.mobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.skillacademy.mobile.data.model.CourseUiModel

/**
 * Base de datos local SQLite para el Entregable 2.
 * Garantiza la persistencia de los cursos agregados al carrito
 * incluso al reiniciar la aplicación o reiniciar el dispositivo.
 */
class CartDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "skillacademy_cart.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_CART = "cart_items"
        const val COL_ID = "id"
        const val COL_CURSO_ID = "curso_id"
        const val COL_TITULO = "titulo"
        const val COL_CATEGORIA = "categoria"
        const val COL_NIVEL = "nivel"
        const val COL_INSTRUCTOR = "instructor"
        const val COL_PRECIO = "precio"
        const val COL_OLD_PRECIO = "old_precio"
        const val COL_IMAGEN = "imagen"
        const val COL_DESCRIPCION = "descripcion"
        const val COL_RATING = "rating"
        const val COL_REVIEWS = "reviews"
        const val COL_FECHA_AGREGADO = "fecha_agregado"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE $TABLE_CART (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CURSO_ID INTEGER UNIQUE,
                $COL_TITULO TEXT NOT NULL,
                $COL_CATEGORIA TEXT,
                $COL_NIVEL TEXT,
                $COL_INSTRUCTOR TEXT,
                $COL_PRECIO REAL NOT NULL,
                $COL_OLD_PRECIO REAL,
                $COL_IMAGEN TEXT,
                $COL_DESCRIPCION TEXT,
                $COL_RATING REAL,
                $COL_REVIEWS INTEGER,
                $COL_FECHA_AGREGADO INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CART")
        onCreate(db)
    }

    fun insertItem(course: CourseUiModel): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CURSO_ID, course.id)
            put(COL_TITULO, course.titulo)
            put(COL_CATEGORIA, course.categoria)
            put(COL_NIVEL, course.nivel)
            put(COL_INSTRUCTOR, course.instructor)
            put(COL_PRECIO, course.precio)
            course.oldPrecio?.let { put(COL_OLD_PRECIO, it) } ?: putNull(COL_OLD_PRECIO)
            put(COL_IMAGEN, course.imagen)
            put(COL_DESCRIPCION, course.descripcion)
            put(COL_RATING, course.rating)
            put(COL_REVIEWS, course.reviews)
            put(COL_FECHA_AGREGADO, System.currentTimeMillis())
        }
        val result = db.insertWithOnConflict(TABLE_CART, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    fun deleteItem(cursoId: Long): Boolean {
        val db = writableDatabase
        val affected = db.delete(TABLE_CART, "$COL_CURSO_ID = ?", arrayOf(cursoId.toString()))
        return affected > 0
    }

    fun clearCart() {
        val db = writableDatabase
        db.delete(TABLE_CART, null, null)
    }

    fun getAllItems(): List<CourseUiModel> {
        val list = mutableListOf<CourseUiModel>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CART,
            null,
            null,
            null,
            null,
            null,
            "$COL_FECHA_AGREGADO ASC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                val cursoIdIndex = it.getColumnIndexOrThrow(COL_CURSO_ID)
                val tituloIndex = it.getColumnIndexOrThrow(COL_TITULO)
                val categoriaIndex = it.getColumnIndexOrThrow(COL_CATEGORIA)
                val nivelIndex = it.getColumnIndexOrThrow(COL_NIVEL)
                val instructorIndex = it.getColumnIndexOrThrow(COL_INSTRUCTOR)
                val precioIndex = it.getColumnIndexOrThrow(COL_PRECIO)
                val oldPrecioIndex = it.getColumnIndexOrThrow(COL_OLD_PRECIO)
                val imagenIndex = it.getColumnIndexOrThrow(COL_IMAGEN)
                val descripcionIndex = it.getColumnIndexOrThrow(COL_DESCRIPCION)
                val ratingIndex = it.getColumnIndexOrThrow(COL_RATING)
                val reviewsIndex = it.getColumnIndexOrThrow(COL_REVIEWS)

                do {
                    val cursoId = it.getLong(cursoIdIndex)
                    val titulo = it.getString(tituloIndex)
                    val categoria = it.getString(categoriaIndex) ?: ""
                    val nivel = it.getString(nivelIndex) ?: "Intermedio"
                    val instructor = it.getString(instructorIndex) ?: ""
                    val precio = it.getDouble(precioIndex)
                    val oldPrecio = if (!it.isNull(oldPrecioIndex)) it.getDouble(oldPrecioIndex) else null
                    val imagen = it.getString(imagenIndex) ?: ""
                    val descripcion = it.getString(descripcionIndex) ?: ""
                    val rating = it.getDouble(ratingIndex)
                    val reviews = it.getInt(reviewsIndex)

                    list.add(
                        CourseUiModel(
                            id = cursoId,
                            titulo = titulo,
                            categoria = categoria,
                            nivel = nivel,
                            instructor = instructor,
                            precio = precio,
                            oldPrecio = oldPrecio,
                            imagen = imagen,
                            descripcion = descripcion,
                            rating = rating,
                            reviews = reviews
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun hasItem(cursoId: Long): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CART,
            arrayOf(COL_ID),
            "$COL_CURSO_ID = ?",
            arrayOf(cursoId.toString()),
            null,
            null,
            null
        )
        cursor.use {
            return it.count > 0
        }
    }
}
