package com.floralorder.app.network

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The app's own database, stored on the phone. It replaces the MySQL database
 * that used to live in XAMPP. It is created (and filled with the sample
 * flowers, admin user, and categories) the first time the app runs.
 */
class LocalDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, "floral_local.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                address TEXT DEFAULT '',
                phone TEXT DEFAULT '',
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                role TEXT NOT NULL DEFAULT 'customer'
            )"""
        )
        db.execSQL(
            """CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT DEFAULT '',
                price REAL NOT NULL,
                image_url TEXT DEFAULT '',
                category_id INTEGER,
                is_featured INTEGER DEFAULT 0,
                stock INTEGER DEFAULT 100,
                FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE cart (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
            )"""
        )
        db.execSQL(
            """CREATE TABLE orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                total_amount REAL NOT NULL,
                delivery_address TEXT NOT NULL,
                payment_method TEXT NOT NULL,
                status TEXT DEFAULT 'Pending',
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )"""
        )
        db.execSQL(
            """CREATE TABLE order_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                price REAL NOT NULL,
                FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                FOREIGN KEY (product_id) REFERENCES products(id)
            )"""
        )
        db.execSQL(
            """CREATE TABLE contact_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                message TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )"""
        )

        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop existing tables and recreate if upgrading database version
        db.execSQL("DROP TABLE IF EXISTS order_items")
        db.execSQL("DROP TABLE IF EXISTS orders")
        db.execSQL("DROP TABLE IF EXISTS cart")
        db.execSQL("DROP TABLE IF EXISTS products")
        db.execSQL("DROP TABLE IF EXISTS categories")
        db.execSQL("DROP TABLE IF EXISTS contact_messages")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    private fun seed(db: SQLiteDatabase) {
        // --- 1. USERS (Admin lang ang natira) ---
        insertUser(db, 1, "Admin", "admin@gmail.com", PasswordUtil.hash("adminonly"), "N/A", "N/A", "2026-09-27 06:29:26", "admin")

        // --- 2. CATEGORIES ---
        val categories = listOf(
            Pair(1, "Roses"),
            Pair(2, "Peonies"),
            Pair(3, "Tulips"),
            Pair(4, "Sunflowers"),
            Pair(5, "LiLies")
        )
        for ((id, name) in categories) {
            val values = ContentValues().apply {
                put("id", id)
                put("name", name)
            }
            db.insert("categories", null, values)
        }

        // --- 3. PRODUCTS ---
        addProduct(db, 1, "Red Rose Bouquet", "A classic bouquet of 12 fresh red roses, hand-tied with ribbon.", 799.0, "https://i.imgur.com/aeK4nEe.jpeg", 1, 1, 9)
        addProduct(db, 2, "White Rose Elegance", "Elegant white roses arranged in a glass vase.", 899.0, "https://i.imgur.com/oqKoWpP.jpeg", 1, 1, 10)
        addProduct(db, 3, "Peonies", "Peonies 1", 650.0, "https://i.imgur.com/GRRXCar.jpeg", 2, 1, 99)
        addProduct(db, 4, "Lilies 1", "Soft pastel-colored lilies, perfect for spring.", 550.0, "https://i.imgur.com/afeA8i1.jpeg", 5, 1, 100)
        addProduct(db, 5, "Sunflower 1", "Bright sunflowers to brighten anyone's day.", 480.0, "https://i.imgur.com/CM090JA.jpeg", 4, 1, 5)
        addProduct(db, 6, "Lilies 2", "Lily", 1200.0, "https://i.imgur.com/zlgwIVR.jpeg", 5, 1, 100)
        addProduct(db, 7, "Rose 2", "Pink Roses", 950.0, "https://i.imgur.com/WNfNqtF.jpeg", 1, 0, 100)
        addProduct(db, 8, "Peonies 2", "Mix with tulips", 720.0, "https://i.imgur.com/xHOcLs0.jpeg", 2, 0, 100)
    }

    private fun insertUser(db: SQLiteDatabase, id: Int, name: String, email: String, pass: String, address: String, phone: String, createdAt: String, role: String) {
        val v = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("email", email)
            put("password", pass)
            put("address", address)
            put("phone", phone)
            put("created_at", createdAt)
            put("role", role)
        }
        db.insert("users", null, v)
    }

    private fun addProduct(
        db: SQLiteDatabase, id: Int, name: String, description: String, price: Double,
        imageUrl: String, categoryId: Int, featured: Int, stock: Int
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("price", price)
            put("image_url", imageUrl)
            put("category_id", categoryId)
            put("is_featured", featured)
            put("stock", stock)
        }
        db.insert("products", null, values)
    }

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDatabase(context.applicationContext).also { INSTANCE = it }
            }
    }
}