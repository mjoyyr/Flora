package com.floralorder.app.network

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local replacement for the PHP backend. Every endpoint the app used to call
 * over HTTP is answered here from the SQLite database on the phone, returning
 * the same JSON shape, so the Activities work exactly as before.
 */
object LocalApi {

    private val ORDER_STATUSES =
        listOf("Pending", "Processing", "Out for Delivery", "Delivered", "Cancelled")

    fun handle(context: Context, url: String, body: JSONObject?): JSONObject {
        val db = LocalDatabase.getInstance(context).writableDatabase
        val uri = Uri.parse(url)
        val b = body ?: JSONObject()

        return when (uri.lastPathSegment ?: "") {
            "register.php" -> register(db, b)
            "login.php" -> login(db, b)
            "get_products.php" -> getProducts(db, uri)
            "get_categories.php" -> getCategories(db)
            "add_to_cart.php" -> addToCart(db, b)
            "get_cart.php" -> getCart(db, uri)
            "update_cart.php" -> updateCart(db, b)
            "remove_from_cart.php" -> removeFromCart(db, b)
            "checkout.php" -> checkout(db, b)
            "get_orders.php" -> getOrders(db, uri)
            "get_order.php" -> getOrder(db, uri)
            "update_order_status.php" -> updateOrderStatus(db, b)
            "contact.php" -> contact(db, b)
            "get_all_orders.php" -> getAllOrders(db)
            "get_all_users.php" -> getAllUsers(db)
            "get_all_messages.php" -> getAllMessages(db)
            "admin_update_user.php" -> updateUser(db, b)
            "admin_delete_user.php" -> deleteUser(db, b)
            "admin_add_product.php" -> addProduct(db, b)
            "admin_update_product.php" -> updateProduct(db, b)
            "admin_delete_product.php" -> deleteProduct(db, b)
            "admin_update_stock.php" -> updateStock(db, b)
            else -> fail("Unknown request.")
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun ok(vararg pairs: Pair<String, Any?>): JSONObject {
        val o = JSONObject()
        o.put("success", true)
        for ((key, value) in pairs) o.put(key, value)
        return o
    }

    private fun fail(message: String): JSONObject {
        val o = JSONObject()
        o.put("success", false)
        o.put("message", message)
        return o
    }

    private fun strArgs(args: Array<out Any>): Array<String> =
        Array(args.size) { args[it].toString() }

    private fun intParam(uri: Uri, name: String): Int =
        uri.getQueryParameter(name)?.toIntOrNull() ?: 0

    /** Runs a SELECT and returns every row as a JSON object. */
    private fun query(db: SQLiteDatabase, sql: String, vararg args: Any): JSONArray {
        val result = JSONArray()
        db.rawQuery(sql, strArgs(args)).use { c ->
            while (c.moveToNext()) {
                val row = JSONObject()
                for (i in 0 until c.columnCount) {
                    val name = c.getColumnName(i)
                    when (c.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> row.put(name, c.getLong(i))
                        Cursor.FIELD_TYPE_FLOAT -> row.put(name, c.getDouble(i))
                        Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                        else -> row.put(name, c.getString(i))
                    }
                }
                result.put(row)
            }
        }
        return result
    }

    private fun exists(db: SQLiteDatabase, sql: String, vararg args: Any): Boolean =
        db.rawQuery(sql, strArgs(args)).use { it.moveToFirst() }

    private fun scalarInt(db: SQLiteDatabase, sql: String, vararg args: Any): Int =
        db.rawQuery(sql, strArgs(args)).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun attachItems(db: SQLiteDatabase, orders: JSONArray) {
        for (i in 0 until orders.length()) {
            val order = orders.getJSONObject(i)
            order.put(
                "items",
                query(
                    db,
                    "SELECT oi.quantity, oi.price, p.name, p.image_url FROM order_items oi " +
                        "JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?",
                    order.getLong("id")
                )
            )
        }
    }

    // ------------------------------------------------------------------- auth

    private fun register(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val name = b.optString("name").trim()
        val email = b.optString("email").trim()
        val password = b.optString("password")
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return fail("Name, email, and password are required.")
        }
        if (exists(db, "SELECT id FROM users WHERE email = ? COLLATE NOCASE", email)) {
            return fail("Email is already registered.")
        }

        val values = ContentValues()
        values.put("name", name)
        values.put("email", email)
        values.put("password", PasswordUtil.hash(password))
        values.put("address", b.optString("address"))
        values.put("phone", b.optString("phone"))
        values.put("role", "customer")
        val id = db.insert("users", null, values)
        return ok("message" to "Registration successful.", "user_id" to id)
    }

    private fun login(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val email = b.optString("email").trim()
        val password = b.optString("password")
        if (email.isEmpty() || password.isEmpty()) {
            return fail("Email and password are required.")
        }

        val users = query(
            db,
            "SELECT id, name, email, password, address, phone, role FROM users " +
                "WHERE email = ? COLLATE NOCASE",
            email
        )
        if (users.length() > 0) {
            val user = users.getJSONObject(0)
            if (PasswordUtil.verify(password, user.getString("password"))) {
                user.remove("password")
                return ok("message" to "Login successful.", "user" to user)
            }
        }
        return fail("Invalid email or password.")
    }

    // --------------------------------------------------------------- products

    private fun getProducts(db: SQLiteDatabase, uri: Uri): JSONObject {
        var sql = "SELECT p.id, p.name, p.description, p.price, p.image_url, p.category_id, " +
            "p.is_featured, p.stock, COALESCE(c.name, '') AS category_name " +
            "FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE 1=1"
        val args = mutableListOf<Any>()

        val categoryId = intParam(uri, "category_id")
        if (categoryId != 0) {
            sql += " AND p.category_id = ?"
            args.add(categoryId)
        }
        if (!uri.getQueryParameter("featured").isNullOrEmpty()) {
            sql += " AND p.is_featured = 1"
        }
        val id = intParam(uri, "id")
        if (id != 0) {
            sql += " AND p.id = ?"
            args.add(id)
        }
        sql += " ORDER BY p.id"

        return ok("products" to query(db, sql, *args.toTypedArray()))
    }

    private fun getCategories(db: SQLiteDatabase): JSONObject =
        ok("categories" to query(db, "SELECT id, name FROM categories ORDER BY name"))

    private fun productValues(b: JSONObject, name: String): ContentValues {
        val values = ContentValues()
        values.put("name", name)
        values.put("description", b.optString("description"))
        values.put("price", b.optDouble("price"))
        values.put("image_url", b.optString("image_url"))
        if (b.has("category_id") && !b.isNull("category_id")) {
            values.put("category_id", b.optInt("category_id"))
        } else {
            values.putNull("category_id")
        }
        values.put("is_featured", if (b.optBoolean("is_featured", false)) 1 else 0)
        values.put("stock", b.optInt("stock", 0))
        return values
    }

    private fun addProduct(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val name = b.optString("name").trim()
        if (name.isEmpty() || !b.has("price")) return fail("name and price are required.")
        val id = db.insert("products", null, productValues(b, name))
        return ok("message" to "Product added.", "product_id" to id)
    }

    private fun updateProduct(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val productId = b.optInt("product_id")
        val name = b.optString("name").trim()
        if (productId == 0 || name.isEmpty() || !b.has("price")) {
            return fail("product_id, name, and price are required.")
        }
        db.update("products", productValues(b, name), "id = ?", arrayOf(productId.toString()))
        return ok("message" to "Product updated.")
    }

    /** Inventory: sets the stock of one product to an exact number. */
    private fun updateStock(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val productId = b.optInt("product_id")
        if (productId == 0 || !b.has("stock")) return fail("product_id and stock are required.")
        val stock = b.optInt("stock")
        if (stock < 0) return fail("Stock cannot be negative.")

        val values = ContentValues()
        values.put("stock", stock)
        db.update("products", values, "id = ?", arrayOf(productId.toString()))
        return ok("message" to "Stock updated.", "stock" to stock)
    }

    private fun deleteProduct(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val productId = b.optInt("product_id")
        if (productId == 0) return fail("product_id is required.")

        // Products that were already ordered must stay, or old orders would break.
        if (scalarInt(db, "SELECT COUNT(*) FROM order_items WHERE product_id = ?", productId) > 0) {
            return fail(
                "Cannot delete this product: it has order history. " +
                    "Set its stock to 0 instead to hide it from customers."
            )
        }
        db.delete("cart", "product_id = ?", arrayOf(productId.toString()))
        db.delete("products", "id = ?", arrayOf(productId.toString()))
        return ok("message" to "Product deleted.")
    }

    // ------------------------------------------------------------------- cart

    private fun addToCart(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val userId = b.optInt("user_id")
        val productId = b.optInt("product_id")
        val quantity = b.optInt("quantity")
        if (userId == 0 || productId == 0 || quantity <= 0) {
            return fail("user_id, product_id, and quantity are required.")
        }

        val stock = scalarInt(db, "SELECT stock FROM products WHERE id = ?", productId)
        val existing = query(
            db,
            "SELECT id, quantity FROM cart WHERE user_id = ? AND product_id = ?",
            userId, productId
        )
        val currentQty = if (existing.length() > 0) existing.getJSONObject(0).getInt("quantity") else 0

        if (currentQty + quantity > stock) {
            return fail(if (stock <= 0) "This product is out of stock." else "Only $stock in stock.")
        }

        val values = ContentValues()
        if (existing.length() > 0) {
            values.put("quantity", currentQty + quantity)
            db.update(
                "cart", values, "id = ?",
                arrayOf(existing.getJSONObject(0).getLong("id").toString())
            )
        } else {
            values.put("user_id", userId)
            values.put("product_id", productId)
            values.put("quantity", quantity)
            db.insert("cart", null, values)
        }
        return ok("message" to "Added to cart.")
    }

    private fun getCart(db: SQLiteDatabase, uri: Uri): JSONObject {
        val items = query(
            db,
            "SELECT c.id AS cart_id, c.quantity, p.id AS product_id, p.name, p.price, p.image_url " +
                "FROM cart c JOIN products p ON c.product_id = p.id " +
                "WHERE c.user_id = ? ORDER BY c.id",
            intParam(uri, "user_id")
        )
        var total = 0.0
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            total += item.getDouble("price") * item.getInt("quantity")
        }
        return ok("items" to items, "total" to total)
    }

    private fun updateCart(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val cartId = b.optInt("cart_id")
        if (cartId == 0 || !b.has("quantity")) return fail("cart_id and quantity are required.")

        val quantity = b.optInt("quantity")
        if (quantity <= 0) {
            db.delete("cart", "id = ?", arrayOf(cartId.toString()))
        } else {
            val values = ContentValues()
            values.put("quantity", quantity)
            db.update("cart", values, "id = ?", arrayOf(cartId.toString()))
        }
        return ok("message" to "Cart updated.")
    }

    private fun removeFromCart(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val cartId = b.optInt("cart_id")
        if (cartId == 0) return fail("cart_id is required.")
        db.delete("cart", "id = ?", arrayOf(cartId.toString()))
        return ok("message" to "Item removed.")
    }

    // ----------------------------------------------------------------- orders

    private fun checkout(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val userId = b.optInt("user_id")
        val address = b.optString("delivery_address")
        val payment = b.optString("payment_method")
        if (userId == 0 || address.isEmpty() || payment.isEmpty()) {
            return fail("user_id, delivery_address, and payment_method are required.")
        }

        db.beginTransaction()
        try {
            val items = query(
                db,
                "SELECT c.product_id, c.quantity, p.price, p.stock, p.name " +
                    "FROM cart c JOIN products p ON c.product_id = p.id WHERE c.user_id = ?",
                userId
            )
            if (items.length() == 0) return fail("Your cart is empty.")

            // Make sure there is enough stock for every item before placing the order.
            var total = 0.0
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.getInt("quantity") > item.getInt("stock")) {
                    return fail(
                        "Sorry, \"${item.getString("name")}\" only has " +
                            "${item.getInt("stock")} left in stock."
                    )
                }
                total += item.getDouble("price") * item.getInt("quantity")
            }

            val orderValues = ContentValues()
            orderValues.put("user_id", userId)
            orderValues.put("total_amount", total)
            orderValues.put("delivery_address", address)
            orderValues.put("payment_method", payment)
            orderValues.put("status", "Pending")
            val orderId = db.insert("orders", null, orderValues)

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val productId = item.getInt("product_id")
                val qty = item.getInt("quantity")

                val itemValues = ContentValues()
                itemValues.put("order_id", orderId)
                itemValues.put("product_id", productId)
                itemValues.put("quantity", qty)
                itemValues.put("price", item.getDouble("price"))
                db.insert("order_items", null, itemValues)

                db.execSQL(
                    "UPDATE products SET stock = stock - ? WHERE id = ?",
                    arrayOf<Any>(qty, productId)
                )
            }

            db.delete("cart", "user_id = ?", arrayOf(userId.toString()))
            db.setTransactionSuccessful()
            return ok(
                "message" to "Order placed successfully.",
                "order_id" to orderId,
                "total" to total
            )
        } finally {
            db.endTransaction()
        }
    }

    private fun getOrders(db: SQLiteDatabase, uri: Uri): JSONObject {
        val orders = query(
            db,
            "SELECT id, total_amount, delivery_address, payment_method, status, created_at " +
                "FROM orders WHERE user_id = ? ORDER BY id DESC",
            intParam(uri, "user_id")
        )
        attachItems(db, orders)
        return ok("orders" to orders)
    }

    private fun getOrder(db: SQLiteDatabase, uri: Uri): JSONObject {
        val orders = query(
            db,
            "SELECT id, user_id, total_amount, delivery_address, payment_method, status, created_at " +
                "FROM orders WHERE id = ?",
            intParam(uri, "id")
        )
        if (orders.length() == 0) return fail("Order not found.")
        attachItems(db, orders)
        return ok("order" to orders.getJSONObject(0))
    }

    private fun updateOrderStatus(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val orderId = b.optInt("order_id")
        val status = b.optString("status")
        if (orderId == 0 || status.isEmpty()) return fail("order_id and status are required.")
        if (status !in ORDER_STATUSES) return fail("Invalid status value.")

        val values = ContentValues()
        values.put("status", status)
        db.update("orders", values, "id = ?", arrayOf(orderId.toString()))
        return ok("message" to "Order status updated to $status")
    }

    // ---------------------------------------------------------------- contact

    private fun contact(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val name = b.optString("name").trim()
        val email = if (b.optString("email").isNotEmpty()) b.optString("email").trim() else b.optString("phone").trim()
        val message = b.optString("message").trim()
        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            return fail("Name, contact info, and message are required.")
        }

        val values = ContentValues()
        values.put("name", name)
        values.put("email", email)
        values.put("message", message)
        db.insert("contact_messages", null, values)
        return ok("message" to "Your message has been sent.")
    }

    // ------------------------------------------------------------------ admin

    private fun getAllOrders(db: SQLiteDatabase): JSONObject {
        val orders = query(
            db,
            "SELECT o.id, o.total_amount, o.delivery_address, o.payment_method, o.status, " +
                "o.created_at, u.name AS customer_name, u.email AS customer_email " +
                // Oldest order first: whoever ordered first is always at the top of the list.
                "FROM orders o JOIN users u ON o.user_id = u.id ORDER BY o.created_at ASC, o.id ASC"
        )
        attachItems(db, orders)
        return ok("orders" to orders)
    }

    private fun getAllUsers(db: SQLiteDatabase): JSONObject =
        ok(
            "users" to query(
                db,
                "SELECT id, name, email, address, phone, role, created_at FROM users " +
                    "WHERE role = 'customer' ORDER BY id DESC"
            )
        )

    private fun getAllMessages(db: SQLiteDatabase): JSONObject =
        ok(
            "messages" to query(
                db,
                "SELECT id, name, email, message, created_at FROM contact_messages ORDER BY id DESC"
            )
        )

    private fun updateUser(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val userId = b.optInt("user_id")
        val name = b.optString("name").trim()
        val email = b.optString("email").trim()
        if (userId == 0 || name.isEmpty() || email.isEmpty()) {
            return fail("user_id, name, and email are required.")
        }
        if (exists(db, "SELECT id FROM users WHERE email = ? COLLATE NOCASE AND id != ?", email, userId)) {
            return fail("Email is already used by another account.")
        }

        val values = ContentValues()
        values.put("name", name)
        values.put("email", email)
        values.put("phone", b.optString("phone"))
        values.put("address", b.optString("address"))
        db.update("users", values, "id = ? AND role = 'customer'", arrayOf(userId.toString()))
        return ok("message" to "Customer updated.")
    }

    private fun deleteUser(db: SQLiteDatabase, b: JSONObject): JSONObject {
        val userId = b.optInt("user_id")
        if (userId == 0) return fail("user_id is required.")

        // Customers who already ordered must stay, or their orders would be orphaned.
        if (scalarInt(db, "SELECT COUNT(*) FROM orders WHERE user_id = ?", userId) > 0) {
            return fail("Cannot delete this customer: they have existing orders.")
        }
        db.delete("users", "id = ? AND role = 'customer'", arrayOf(userId.toString()))
        return ok("message" to "Customer deleted.")
    }
}
