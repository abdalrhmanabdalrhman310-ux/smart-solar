package com.smartsolar.app.data.remote

/**
 * Central PHP API integration contract aligned with:
 * - MySQL database: solar_platform (11 tables: users, admins, engineers, products, orders,
 *   order_items, installations, maintenance, solar_designs, settings, notifications)
 * - PHP API folder: solar-admin/api/ (11 files)
 *
 * IMPORTANT INTEGRATION NOTICE:
 * Because the original PHP backend source files are hosted externally on the user's server,
 * exact action parameter names (e.g., `action=...`) and field names in each PHP endpoint MUST
 * be verified against the actual PHP code on the server. All endpoints below are centralized
 * in one place so any action/field mismatch can be adjusted cleanly without touching UI code.
 */
object PhpEndpointContract {

    const val FILE_BOOTSTRAP = "_bootstrap.php"
    const val FILE_AUTH = "auth.php"
    const val FILE_PRODUCTS = "products.php"
    const val FILE_ORDERS = "orders.php"
    const val FILE_SETTINGS = "settings.php"
    const val FILE_SOLAR_DESIGNS = "solar_designs.php"
    const val FILE_ENGINEERS = "engineers.php"
    const val FILE_INSTALLATIONS = "installations.php"
    const val FILE_MAINTENANCE = "maintenance.php"
    const val FILE_RATINGS = "ratings.php"
    const val FILE_NOTIFICATIONS = "notifications.php"

    data class EndpointSpec(
        val phpFile: String,
        val mysqlTables: String,
        val supportedMethods: String,
        val purposeArabic: String,
        val requiresPhpConfirmationNote: String
    )

    val allEndpoints: List<EndpointSpec> = listOf(
        EndpointSpec(
            phpFile = FILE_BOOTSTRAP,
            mysqlTables = "settings, users",
            supportedMethods = "GET",
            purposeArabic = "تهيئة الجلسة والتحقق من جاهزية الخادم",
            requiresPhpConfirmationNote = "تحقق من أن _bootstrap.php يُرجع JSON أو استخدم settings.php لاختبار الاتصال."
        ),
        EndpointSpec(
            phpFile = FILE_AUTH,
            mysqlTables = "users, engineers, admins",
            supportedMethods = "POST (JSON / Multipart)",
            purposeArabic = "تسجيل الدخول، تسجيل العميل/المهندس/البائع، تحديث الملف الشخصي، تسجيل الخروج",
            requiresPhpConfirmationNote = "تحتاج لتأكيد أسماء الحقول (action: login / register / update_profile / logout، وحالة الموافقة approval_status)."
        ),
        EndpointSpec(
            phpFile = FILE_PRODUCTS,
            mysqlTables = "products",
            supportedMethods = "GET / POST (JSON / Multipart)",
            purposeArabic = "عرض المنتجات وتفاصيلها، إضافة/تعديل/حذف منتجات البائع، رفع صور المنتجات",
            requiresPhpConfirmationNote = "تحتاج لتأكيد أسماء معاملات التصفية (category, search, vendor_id) وعمليات CRUD للبائع."
        ),
        EndpointSpec(
            phpFile = FILE_ORDERS,
            mysqlTables = "orders, order_items, products",
            supportedMethods = "GET / POST (JSON)",
            purposeArabic = "إنشاء طلب شراء، التحقق من المخزون، عرض طلبات العميل وطلبات البائع",
            requiresPhpConfirmationNote = "تحتاج لتأكيد بنية مصفوفة items عند إرسال الطلب (product_id, quantity) وحقول حالة الطلب."
        ),
        EndpointSpec(
            phpFile = FILE_SETTINGS,
            mysqlTables = "settings",
            supportedMethods = "GET",
            purposeArabic = "جلب إعدادات المنصة العامة والتحقق من الاتصال بالخادم",
            requiresPhpConfirmationNote = "تحتاج لتأكيد مفاتيح الإعدادات مثل متطلبات السجل التجاري أو سياسة موافقة الإدارة."
        ),
        EndpointSpec(
            phpFile = FILE_SOLAR_DESIGNS,
            mysqlTables = "solar_designs",
            supportedMethods = "GET / POST (JSON)",
            purposeArabic = "حفظ تصاميم المنظومة الشمسية من الحاسبة وعرض التصاميم المحفوظة",
            requiresPhpConfirmationNote = "تحتاج لتأكيد أسماء الأعمدة في جدول solar_designs عند الحفظ."
        ),
        EndpointSpec(
            phpFile = FILE_ENGINEERS,
            mysqlTables = "engineers, users",
            supportedMethods = "GET / POST (JSON / Multipart)",
            purposeArabic = "قائمة المهندسين، ملف المهندس، تحديث حالة التوفر (متاح/مشغول/غير متاح)",
            requiresPhpConfirmationNote = "تحتاج لتأكيد أسماء حقول التقييمات والشهادات وصور الأعمال السابقة."
        ),
        EndpointSpec(
            phpFile = FILE_INSTALLATIONS,
            mysqlTables = "installations",
            supportedMethods = "GET / POST (JSON)",
            purposeArabic = "إرسال طلبات التركيب، عرض مهام التركيب للمهندس، قبول المهمة وتحديث الحالة والتقرير الفني",
            requiresPhpConfirmationNote = "تحتاج لتأكيد أسماء الإجراءات (create / list / accept / update_status / complete) في PHP."
        ),
        EndpointSpec(
            phpFile = FILE_MAINTENANCE,
            mysqlTables = "maintenance",
            supportedMethods = "GET / POST (JSON)",
            purposeArabic = "إرسال طلبات الصيانة، متابعة الأعطال، قبول وتحديث مهام الصيانة للمهندس",
            requiresPhpConfirmationNote = "تحتاج لتأكيد أسماء حقول العطل (issue_type / fault_details) وإجراءات المهندس."
        ),
        EndpointSpec(
            phpFile = FILE_RATINGS,
            mysqlTables = "engineers, installations, maintenance",
            supportedMethods = "GET / POST (JSON)",
            purposeArabic = "تقييم المهندس (1-5 نجوم + تعليق) وعرض التقييمات ومنع التكرار",
            requiresPhpConfirmationNote = "تحتاج لتأكيد ربط التقييم برقم الخدمة (service_id, service_type) في جدول التقييمات."
        ),
        EndpointSpec(
            phpFile = FILE_NOTIFICATIONS,
            mysqlTables = "notifications",
            supportedMethods = "GET / POST (JSON)",
            purposeArabic = "جلب إشعارات المستخدم وتحديث حالة القراءة",
            requiresPhpConfirmationNote = "تحتاج لتأكيد إجراء تعليم الإشعار كمقروء (mark_read)."
        )
    )
}
