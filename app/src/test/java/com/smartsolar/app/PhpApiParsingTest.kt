package com.smartsolar.app

import com.smartsolar.app.data.remote.ApiBaseResponse
import com.smartsolar.app.data.remote.AuthPayloadDto
import com.smartsolar.app.data.remote.NetworkClientFactory
import com.smartsolar.app.data.remote.ProductsPayloadDto
import com.smartsolar.app.domain.model.ApprovalStatus
import com.smartsolar.app.domain.model.ProductCategory
import com.smartsolar.app.domain.model.UserRole
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhpApiParsingTest {

    private val moshi = NetworkClientFactory.buildMoshi()

    @Test
    fun `parses auth response for pending engineer approval`() {
        val json = """
            {
              "success": true,
              "message": "تم تسجيل المهندس بنجاح وبانتظار موافقة الإدارة",
              "data": {
                "token": "tok_123",
                "requiresApproval": true,
                "user": {
                  "id": 42,
                  "full_name": "أحمد محمد",
                  "email": "ahmed@solar.test",
                  "role": "engineer",
                  "approval_status": "pending"
                }
              }
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiBaseResponse::class.java, AuthPayloadDto::class.java)
        val adapter = moshi.adapter<ApiBaseResponse<AuthPayloadDto>>(type)
        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertTrue(parsed!!.isOperationSuccess())
        assertEquals(UserRole.ENGINEER, UserRole.fromApi(parsed.data?.user?.role))
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromApi(parsed.data?.user?.approvalStatus))
    }

    @Test
    fun `parses products list and maps Arabic or English categories`() {
        val json = """
            {
              "success": true,
              "data": {
                "products": [
                  {
                    "id": 10,
                    "vendor_id": 5,
                    "name": "لوح شمسي 550 واط",
                    "category": "solar_panels",
                    "price": 185.5,
                    "stock": 24,
                    "is_featured": true
                  }
                ]
              }
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiBaseResponse::class.java, ProductsPayloadDto::class.java)
        val adapter = moshi.adapter<ApiBaseResponse<ProductsPayloadDto>>(type)
        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        val item = parsed!!.data?.products?.firstOrNull()
        assertNotNull(item)
        assertEquals(ProductCategory.SOLAR_PANELS, ProductCategory.fromApi(item!!.category))
        assertEquals(24, item.stock)
    }

    @Test
    fun `detects session_expired response`() {
        val json = """
            {
              "success": false,
              "session_expired": true,
              "message": "Session expired"
            }
        """.trimIndent()
        val type = Types.newParameterizedType(ApiBaseResponse::class.java, AuthPayloadDto::class.java)
        val adapter = moshi.adapter<ApiBaseResponse<AuthPayloadDto>>(type)
        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertFalse(parsed!!.isOperationSuccess())
        assertEquals(true, parsed.sessionExpired)
    }

    @Test(expected = JsonEncodingException::class)
    fun `throws JsonEncodingException on malformed PHP HTML error`() {
        val htmlError = "<br /><b>Fatal error</b>: Uncaught Error in auth.php"
        val type = Types.newParameterizedType(ApiBaseResponse::class.java, AuthPayloadDto::class.java)
        val adapter = moshi.adapter<ApiBaseResponse<AuthPayloadDto>>(type)
        adapter.fromJson(htmlError)
    }
}
