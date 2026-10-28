package com.floralorder.app.network

object ApiConfig {
    // IMPORTANT: your Apache (XAMPP) is running on PORT 8012, not the default 80
    // (check C:\xampp\apache\conf\httpd.conf -> "Listen 8012" if this ever changes).
    //
    // - REAL PHONE via USB (no internet needed, but cable required):
    //     Run in a terminal (with the phone connected): adb reverse tcp:8012 tcp:8012
    //     Then BASE_URL below can stay as "http://localhost:8012/..." because the phone's
    //     "localhost" gets forwarded to your computer over the USB cable.
    //     NOTE: you must re-run that adb reverse command every time you reconnect the
    //     USB cable or restart Android Studio, BEFORE running the app.
    //
    // - REAL PHONE via NGROK (works over ANY internet connection, no USB, no same-WiFi
    //   requirement - see README.md for setup steps):
    //     BASE_URL = "https://xxxx-xx-xx-xxx-xx.ngrok-free.app/floral_backend/"
    //     (this URL changes every time you restart ngrok unless you have a paid static domain)
    //
    // - Android EMULATOR: 10.0.2.2 maps to "localhost" on your computer automatically.
    //     BASE_URL = "http://10.0.2.2:8012/floral_backend/"
    //
    // - REAL PHONE over Wi-Fi instead of USB (only works if your network allows
    //   device-to-device traffic - some school/dorm Wi-Fi blocks this):
    //     BASE_URL = "http://<your computer's LAN IP>:8012/floral_backend/"
    //
    const val BASE_URL = "app://api/"

    const val REGISTER = BASE_URL + "register.php"
    const val LOGIN = BASE_URL + "login.php"
    const val GET_PRODUCTS = BASE_URL + "get_products.php"
    const val GET_CATEGORIES = BASE_URL + "get_categories.php"
    const val ADD_TO_CART = BASE_URL + "add_to_cart.php"
    const val GET_CART = BASE_URL + "get_cart.php"
    const val UPDATE_CART = BASE_URL + "update_cart.php"
    const val REMOVE_FROM_CART = BASE_URL + "remove_from_cart.php"
    const val CHECKOUT = BASE_URL + "checkout.php"
    const val GET_ORDERS = BASE_URL + "get_orders.php"
    const val GET_ORDER = BASE_URL + "get_order.php"
    const val UPDATE_ORDER_STATUS = BASE_URL + "update_order_status.php"
    const val CONTACT = BASE_URL + "contact.php"

    // Admin-only endpoints
    const val GET_ALL_ORDERS = BASE_URL + "get_all_orders.php"
    const val GET_ALL_USERS = BASE_URL + "get_all_users.php"
    const val UPDATE_USER = BASE_URL + "admin_update_user.php"
    const val DELETE_USER = BASE_URL + "admin_delete_user.php"
    const val ADD_PRODUCT = BASE_URL + "admin_add_product.php"
    const val UPDATE_PRODUCT = BASE_URL + "admin_update_product.php"
    const val DELETE_PRODUCT = BASE_URL + "admin_delete_product.php"
    const val UPDATE_STOCK = BASE_URL + "admin_update_stock.php"
    const val GET_ALL_MESSAGES = BASE_URL + "get_all_messages.php"
}
