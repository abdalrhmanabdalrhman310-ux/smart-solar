package com.smartsolar.app

import android.app.Application
import android.security.NetworkSecurityPolicy
import androidx.test.core.app.ApplicationProvider
import com.smartsolar.app.data.local.SessionPreferencesManager
import com.smartsolar.app.data.remote.NetworkErrorType
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.domain.model.ApprovalStatus
import com.smartsolar.app.domain.model.ProductCategory
import com.smartsolar.app.domain.model.UserRole
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhpIntegrationFlowTest {

    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private lateinit var app: Application
    private lateinit var container: AppContainer

    private data class FakeUser(
        val id: Long,
        var fullName: String,
        val email: String,
        var phone: String,
        var city: String,
        var address: String,
        val password: String,
        val role: String,
        var approvalStatus: String,
        var age: Int = 0,
        var qualification: String = "",
        var specialization: String = "",
        var yearsExperience: Int = 0,
        var bio: String = "",
        var previousProjects: String = "",
        var storeName: String = "",
        var businessType: String = "",
        var storeDescription: String = "",
        var commercialRegister: String = ""
    )

    private val usersByEmail = ConcurrentHashMap<String, FakeUser>()
    private val sessionsByCookie = ConcurrentHashMap<String, FakeUser>()
    private val products = mutableListOf<JSONObject>()
    private var nextUserId = 100L
    private var nextProductId = 500L

    @Before
    fun setUp() {
        strictFormOnlyMode = false
        app = ApplicationProvider.getApplicationContext()
        container = AppContainer(app)
        runBlocking {
            container.sessionPreferences.clearSession()
            container.sessionPreferences.resetServerConfigToDefault()
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/solar-admin/api/") { exchange ->
            handleApiRequest(exchange)
        }
        server.start()
        val port = server.address.port
        baseUrl = "http://127.0.0.1:$port/solar-admin/api/"
    }

    @After
    fun tearDown() {
        runBlocking {
            container.sessionPreferences.clearSession()
            container.sessionPreferences.resetServerConfigToDefault()
        }
        server.stop(0)
    }

    @Test
    fun `default config targets local PHP server IP and allows cleartext HTTP`() = runBlocking {
        val defaultConfig = container.sessionPreferences.getCurrentServerConfig()
        assertEquals(SessionPreferencesManager.DEFAULT_API_BASE_URL, defaultConfig.baseUrl)
        assertTrue(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("10.46.17.35"))
        assertTrue(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("127.0.0.1"))
    }

    @Test
    fun `client registers with city and address logs in via PHPSESSID and updates profile`() = runBlocking {
        container.sessionPreferences.saveServerConfig(baseUrl, 15)

        val regRes = container.authRepository.registerClient(
            fullName = "خالد منصور",
            email = "khaled@solar.test",
            phone = "0501234567",
            city = "الرياض",
            password = "secret123",
            address = "حي النرجس شارع 14"
        )
        assertTrue("Expected success on client registration, got $regRes", regRes is NetworkResult.Success)
        val session = (regRes as NetworkResult.Success).data
        assertEquals(UserRole.CLIENT, session.role)
        assertEquals(ApprovalStatus.APPROVED, session.approvalStatus)
        assertEquals("الرياض", session.city)
        assertEquals("حي النرجس شارع 14", session.address)
        assertTrue(session.isAuthenticated)

        // Subsequent profile fetch & update relies on persisted PHPSESSID cookie
        val updateRes = container.authRepository.updateProfile(
            fullName = "خالد منصور السبيعي",
            phone = "0559998887",
            city = "جدة",
            address = "حي الشاطئ"
        )
        assertTrue(updateRes is NetworkResult.Success)
        val updated = (updateRes as NetworkResult.Success).data
        assertEquals("خالد منصور السبيعي", updated.fullName)
        assertEquals("جدة", updated.city)
        assertEquals("حي الشاطئ", updated.address)

        val refreshed = container.authRepository.refreshProfile()
        assertTrue(refreshed is NetworkResult.Success)
        assertEquals("جدة", (refreshed as NetworkResult.Success).data.city)
    }

    @Test
    fun `engineer application saves all fields as pending blocks unapproved login and unlocks on approval`() = runBlocking {
        container.sessionPreferences.saveServerConfig(baseUrl, 15)

        val regRes = container.authRepository.registerEngineer(
            fullName = "م. طارق العتيبي",
            age = 34,
            email = "tariq.eng@solar.test",
            phone = "0512345678",
            city = "الدمام",
            password = "engPass123",
            specialization = "أنظمة الطاقة الهجينة",
            qualification = "بكالوريوس هندسة كهربائية",
            yearsExperience = 8,
            bio = "خبرة في تنفيذ محطات الطاقة الشمسية التجارية",
            previousProjects = "مشروع مصنع الدمام 120 كيلوواط",
            avatarUri = null,
            portfolioUris = emptyList(),
            certificateUris = emptyList()
        )
        assertTrue(regRes is NetworkResult.Success)
        val pendingSession = (regRes as NetworkResult.Success).data
        assertEquals(UserRole.ENGINEER, pendingSession.role)
        assertEquals(ApprovalStatus.PENDING, pendingSession.approvalStatus)
        assertEquals(34, pendingSession.age)
        assertEquals("الدمام", pendingSession.city)
        assertEquals("مشروع مصنع الدمام 120 كيلوواط", pendingSession.previousProjects)

        // Login while pending -> strictly blocked
        val pendingLogin = container.authRepository.login("tariq.eng@solar.test", "engPass123", true)
        assertTrue(pendingLogin is NetworkResult.Error)
        assertEquals(
            NetworkErrorType.FORBIDDEN_OR_PENDING_APPROVAL,
            (pendingLogin as NetworkResult.Error).type
        )

        // Admin rejects -> strictly blocked
        usersByEmail["tariq.eng@solar.test"]!!.approvalStatus = "rejected"
        val rejectedLogin = container.authRepository.login("tariq.eng@solar.test", "engPass123", true)
        assertTrue(rejectedLogin is NetworkResult.Error)
        assertEquals(
            NetworkErrorType.FORBIDDEN_OR_PENDING_APPROVAL,
            (rejectedLogin as NetworkResult.Error).type
        )

        // Admin approves -> allowed into Engineer dashboard
        usersByEmail["tariq.eng@solar.test"]!!.approvalStatus = "approved"
        val approvedLogin = container.authRepository.login("tariq.eng@solar.test", "engPass123", true)
        assertTrue("Expected approved login success, got $approvedLogin", approvedLogin is NetworkResult.Success)
        val approvedSession = (approvedLogin as NetworkResult.Success).data
        assertEquals(UserRole.ENGINEER, approvedSession.role)
        assertEquals(ApprovalStatus.APPROVED, approvedSession.approvalStatus)
        assertTrue(approvedSession.isAuthenticated)
    }

    @Test
    fun `vendor application saves store fields as pending blocks unapproved login and scopes vendor products`() = runBlocking {
        container.sessionPreferences.saveServerConfig(baseUrl, 15)

        val regRes = container.authRepository.registerVendor(
            vendorName = "سعيد باوزير",
            email = "saeed.vendor@solar.test",
            phone = "0567778889",
            city = "الرياض",
            storeAddress = "طريق الملك فهد - مبنى 22",
            password = "vendorPass123",
            storeName = "متجر النخبة للطاقة",
            businessType = "ألواح وبطاريات ليثيوم",
            storeDescription = "موزع معتمد لألواح الطاقة والانفرترات",
            commercialRegister = "1010456789",
            storeLogoUri = null,
            storeImageUri = null
        )
        assertTrue(regRes is NetworkResult.Success)
        val pendingVendor = (regRes as NetworkResult.Success).data
        assertEquals(UserRole.VENDOR, pendingVendor.role)
        assertEquals(ApprovalStatus.PENDING, pendingVendor.approvalStatus)
        assertEquals("ألواح وبطاريات ليثيوم", pendingVendor.businessType)
        assertEquals("الرياض", pendingVendor.city)

        // Blocked while pending
        val pendingLogin = container.authRepository.login("saeed.vendor@solar.test", "vendorPass123", true)
        assertTrue(pendingLogin is NetworkResult.Error)
        assertEquals(
            NetworkErrorType.FORBIDDEN_OR_PENDING_APPROVAL,
            (pendingLogin as NetworkResult.Error).type
        )

        // Approved by admin -> can log in & create/manage own products
        usersByEmail["saeed.vendor@solar.test"]!!.approvalStatus = "approved"
        val approvedLogin = container.authRepository.login("saeed.vendor@solar.test", "vendorPass123", true)
        assertTrue(approvedLogin is NetworkResult.Success)
        val activeVendor = (approvedLogin as NetworkResult.Success).data
        assertEquals(UserRole.VENDOR, activeVendor.role)
        assertEquals(ApprovalStatus.APPROVED, activeVendor.approvalStatus)

        val createRes = container.catalogRepository.saveVendorProduct(
            productId = null,
            name = "لوح شمسي 580 واط",
            description = "لوح مونو كريستال عالي الكفاءة",
            category = ProductCategory.SOLAR_PANELS,
            price = 210.0,
            stock = 15,
            imageUri = null
        )
        assertTrue(createRes is NetworkResult.Success)
        val vendorProducts = container.catalogRepository.fetchProducts(vendorId = activeVendor.userId)
        assertTrue(vendorProducts is NetworkResult.Success)
        val list = (vendorProducts as NetworkResult.Success).data
        assertEquals(1, list.size)
        assertEquals(activeVendor.userId, list.first().vendorId)
        assertEquals("لوح شمسي 580 واط", list.first().name)
    }

    @Test
    fun `surfaces real PHP 400 error message and retries as form POST when PHP reads $_POST only`() = runBlocking {
        container.sessionPreferences.saveServerConfig(baseUrl, 15)

        // 1. Duplicate email HTTP 400 error body must surface PHP's exact Arabic message
        val firstReg = container.authRepository.registerClient(
            fullName = "أحمد صالح",
            email = "dup@solar.test",
            phone = "0500001111",
            city = "الرياض",
            password = "secret123",
            address = "حي الملقا"
        )
        assertTrue(firstReg is NetworkResult.Success)

        val dupReg = container.authRepository.registerClient(
            fullName = "أحمد صالح",
            email = "dup@solar.test",
            phone = "0500001111",
            city = "الرياض",
            password = "secret123",
            address = "حي الملقا"
        )
        assertTrue(dupReg is NetworkResult.Error)
        val dupErr = dupReg as NetworkResult.Error
        assertEquals(NetworkErrorType.VALIDATION_ERROR, dupErr.type)
        assertTrue("Expected PHP 400 message in ${dupErr.arabicMessage}", dupErr.arabicMessage.contains("البريد الإلكتروني مسجل مسبقاً"))

        // 2. Server rejecting application/json (requiring $_POST form-urlencoded) triggers automatic form fallback
        strictFormOnlyMode = true
        val formReg = container.authRepository.registerClient(
            fullName = "سلمان الحربي",
            email = "salman.form@solar.test",
            phone = "0502223334",
            city = "مكة",
            password = "secret123",
            address = "العزيزية"
        )
        assertTrue("Expected form-POST fallback success, got $formReg", formReg is NetworkResult.Success)
        assertEquals("سلمان الحربي", (formReg as NetworkResult.Success).data.fullName)
    }

    private fun handleApiRequest(exchange: HttpExchange) {
        val rawBody = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
        val path = exchange.requestURI.path
        val query = exchange.requestURI.rawQuery.orEmpty()
        val contentType = exchange.requestHeaders.getFirst("Content-Type").orEmpty()
        val cookieId = parseSessionCookie(exchange.requestHeaders.getFirst("Cookie"))

        val (statusCode, jsonResponse, newCookieId) = when {
            path.endsWith("auth.php") -> handleAuth(query, contentType, rawBody, cookieId)
            path.endsWith("products.php") -> handleProducts(query, contentType, rawBody, cookieId)
            else -> Triple(404, JSONObject().put("success", false).put("message", "Not found"), null)
        }

        if (newCookieId != null) {
            exchange.responseHeaders.add("Set-Cookie", "PHPSESSID=$newCookieId; Path=/; HttpOnly")
        }
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        val bytes = jsonResponse.toString().toByteArray(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private var strictFormOnlyMode = false

    private fun handleAuth(
        query: String,
        contentType: String,
        rawBody: String,
        cookieId: String?
    ): Triple<Int, JSONObject, String?> {
        val action = Regex("(?:^|&)action=([^&]+)").find(query)?.groupValues?.get(1) ?: ""
        if (action.isBlank()) {
            return Triple(400, JSONObject().put("success", false).put("message", "Missing action query parameter"), null)
        }
        return when {
            action == "profile" -> {
                val user = cookieId?.let { sessionsByCookie[it] }
                    ?: return Triple(401, JSONObject().put("success", false).put("session_expired", true), null)
                Triple(200, JSONObject().put("success", true).put("data", JSONObject().put("user", user.toJson())), null)
            }
            query.contains("action=update_profile") -> {
                val user = cookieId?.let { sessionsByCookie[it] }
                    ?: return Triple(401, JSONObject().put("success", false).put("session_expired", true), null)
                val body = JSONObject(rawBody)
                user.fullName = body.optString("full_name", user.fullName)
                user.phone = body.optString("phone", user.phone)
                user.city = body.optString("city", user.city)
                user.address = body.optString("address", user.address)
                Triple(
                    200,
                    JSONObject().put("success", true).put("message", "تم التحديث").put("data", JSONObject().put("user", user.toJson())),
                    null
                )
            }
            contentType.contains("multipart/form-data", ignoreCase = true) -> {
                val fields = parseMultipartTextFields(contentType, rawBody)
                val role = fields["role"] ?: "engineer"
                val email = fields["email"] ?: ""
                val user = FakeUser(
                    id = ++nextUserId,
                    fullName = fields["full_name"] ?: "",
                    email = email,
                    phone = fields["phone"] ?: "",
                    city = fields["city"] ?: "",
                    address = fields["store_address"] ?: fields["address"] ?: "",
                    password = fields["password"] ?: "",
                    role = role,
                    approvalStatus = "pending",
                    age = fields["age"]?.toIntOrNull() ?: 0,
                    qualification = fields["qualification"] ?: "",
                    specialization = fields["specialization"] ?: "",
                    yearsExperience = fields["years_experience"]?.toIntOrNull() ?: 0,
                    bio = fields["bio"] ?: "",
                    previousProjects = fields["previous_projects"] ?: "",
                    storeName = fields["store_name"] ?: "",
                    businessType = fields["business_type"] ?: "",
                    storeDescription = fields["store_description"] ?: "",
                    commercialRegister = fields["commercial_register"] ?: ""
                )
                usersByEmail[email] = user
                Triple(
                    200,
                    JSONObject()
                        .put("success", true)
                        .put("requiresApproval", true)
                        .put("message", "تم استلام الطلب وهو قيد المراجعة")
                        .put("data", JSONObject().put("requiresApproval", true).put("user", user.toJson())),
                    null
                )
            }
            contentType.contains("application/x-www-form-urlencoded", ignoreCase = true) -> {
                val fields = rawBody.split("&").mapNotNull { pair ->
                    val idx = pair.indexOf("=")
                    if (idx == -1) null
                    else java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8") to
                        java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                }.toMap()
                when (action) {
                    "register" -> {
                        val email = fields["email"].orEmpty()
                        if (usersByEmail.containsKey(email)) {
                            return Triple(400, JSONObject().put("success", false).put("message", "البريد الإلكتروني مسجل مسبقاً"), null)
                        }
                        val user = FakeUser(
                            id = ++nextUserId,
                            fullName = fields["full_name"].orEmpty(),
                            email = email,
                            phone = fields["phone"].orEmpty(),
                            city = fields["city"].orEmpty(),
                            address = fields["address"].orEmpty(),
                            password = fields["password"].orEmpty(),
                            role = fields["role"] ?: "client",
                            approvalStatus = "approved"
                        )
                        usersByEmail[email] = user
                        Triple(200, JSONObject().put("success", true).put("message", "تم إنشاء الحساب").put("data", JSONObject().put("user", user.toJson())), null)
                    }
                    "login" -> {
                        val user = usersByEmail[fields["email"].orEmpty()]
                        if (user == null || user.password != fields["password"]) {
                            return Triple(400, JSONObject().put("success", false).put("message", "بيانات الدخول غير صحيحة"), null)
                        }
                        val sid = "sess_${user.id}"
                        sessionsByCookie[sid] = user
                        Triple(200, JSONObject().put("success", true).put("data", JSONObject().put("user", user.toJson())), sid)
                    }
                    else -> Triple(400, JSONObject().put("success", false).put("message", "Unsupported form action"), null)
                }
            }
            else -> {
                if (strictFormOnlyMode) {
                    return Triple(400, JSONObject().put("success", false).put("message", "Required POST fields are missing"), null)
                }
                val body = JSONObject(rawBody)
                val effectiveAction = body.optString("action", action)
                when (effectiveAction) {
                    "update_profile" -> {
                        val user = cookieId?.let { sessionsByCookie[it] }
                            ?: return Triple(401, JSONObject().put("success", false).put("session_expired", true), null)
                        user.fullName = body.optString("full_name", user.fullName)
                        user.phone = body.optString("phone", user.phone)
                        user.city = body.optString("city", user.city)
                        user.address = body.optString("address", user.address)
                        Triple(
                            200,
                            JSONObject().put("success", true).put("message", "تم التحديث").put("data", JSONObject().put("user", user.toJson())),
                            null
                        )
                    }
                    "register" -> {
                        val email = body.optString("email")
                        if (usersByEmail.containsKey(email)) {
                            return Triple(400, JSONObject().put("success", false).put("message", "البريد الإلكتروني مسجل مسبقاً"), null)
                        }
                        val user = FakeUser(
                            id = ++nextUserId,
                            fullName = body.optString("full_name"),
                            email = email,
                            phone = body.optString("phone"),
                            city = body.optString("city"),
                            address = body.optString("address"),
                            password = body.optString("password"),
                            role = body.optString("role", "client"),
                            approvalStatus = "approved"
                        )
                        usersByEmail[email] = user
                        // Simulate PHP saving in users table without returning token -> triggers auto-login
                        Triple(
                            200,
                            JSONObject().put("success", true).put("message", "تم إنشاء الحساب").put("data", JSONObject().put("user", user.toJson())),
                            null
                        )
                    }
                    else -> {
                        val email = body.optString("email")
                        val password = body.optString("password")
                        val user = usersByEmail[email]
                        if (user == null || user.password != password) {
                            return Triple(200, JSONObject().put("success", false).put("message", "بيانات الدخول غير صحيحة"), null)
                        }
                        // Simulate PHP setting PHPSESSID cookie without JWT token (classic PHP session auth!)
                        val sid = "sess_${user.id}"
                        sessionsByCookie[sid] = user
                        Triple(
                            200,
                            JSONObject()
                                .put("success", true)
                                .put("data", JSONObject().put("user", user.toJson())),
                            sid
                        )
                    }
                }
            }
        }
    }

    private fun handleProducts(
        query: String,
        contentType: String,
        rawBody: String,
        cookieId: String?
    ): Triple<Int, JSONObject, String?> {
        val user = cookieId?.let { sessionsByCookie[it] }
        return if (contentType.contains("multipart/form-data", ignoreCase = true)) {
            if (user == null) {
                return Triple(401, JSONObject().put("success", false).put("session_expired", true), null)
            }
            val fields = parseMultipartTextFields(contentType, rawBody)
            val product = JSONObject()
                .put("id", (++nextProductId).toString())
                .put("vendor_id", user.id.toString())
                .put("name", fields["name"] ?: "")
                .put("description", fields["description"] ?: "")
                .put("category", fields["category"] ?: "")
                .put("price", fields["price"] ?: "0")
                .put("stock", fields["stock"] ?: "0")
                .put("is_featured", "1")
            products.add(product)
            Triple(200, JSONObject().put("success", true).put("data", product), null)
        } else {
            val vendorIdFilter = Regex("(?:^|&)vendor_id=([^&]+)").find(query)?.groupValues?.get(1)
            val arr = JSONArray()
            val matching = if (!vendorIdFilter.isNullOrBlank()) {
                products.filter { it.optString("vendor_id") == vendorIdFilter }
            } else {
                products
            }
            matching.forEach { arr.put(it) }
            Triple(200, JSONObject().put("success", true).put("data", JSONObject().put("products", arr)), null)
        }
    }

    private fun parseSessionCookie(cookieHeader: String?): String? {
        if (cookieHeader.isNullOrBlank()) return null
        return Regex("PHPSESSID=([^;\\s]+)").find(cookieHeader)?.groupValues?.get(1)
    }

    private fun parseMultipartTextFields(contentType: String, rawBody: String): Map<String, String> {
        val boundary = Regex("boundary=([^;\\s]+)").find(contentType)?.groupValues?.get(1)?.trim('"')
            ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        val parts = rawBody.split("--$boundary")
        for (part in parts) {
            val nameMatch = Regex("name=\"([^\"]+)\"").find(part) ?: continue
            val fieldName = nameMatch.groupValues[1]
            val headerEnd = part.indexOf("\r\n\r\n")
            if (headerEnd != -1) {
                val value = part.substring(headerEnd + 4).trimEnd('\r', '\n')
                result[fieldName] = value
            }
        }
        return result
    }

    private fun FakeUser.toJson(): JSONObject = JSONObject()
        .put("id", id.toString())
        .put("full_name", fullName)
        .put("email", email)
        .put("phone", phone)
        .put("city", city)
        .put("address", address)
        .put("role", role)
        .put("approval_status", approvalStatus)
        .put("age", age.toString())
        .put("qualification", qualification)
        .put("specialization", specialization)
        .put("years_experience", yearsExperience.toString())
        .put("bio", bio)
        .put("previous_projects", previousProjects)
        .put("store_name", storeName)
        .put("business_type", businessType)
        .put("store_description", storeDescription)
        .put("commercial_register", commercialRegister)
}
