package com.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationParserTest {

    @Test
    fun parseIncomeVietcombank() {
        val title = "Vietcombank thong bao"
        val text = "SD TK 0451 thay doi +500,000 VND luc 12:30. GD: Chuyen khoan luong thang"
        val packageName = "com.vietcombank.restyle"

        val result = NotificationParser.parse(title, text, packageName)

        assertTrue(result.success)
        assertEquals(500000.0, result.amount, 0.0)
        assertEquals("INCOME", result.type)
        assertEquals("Vietcombank", result.bankName)
        assertEquals("Chuyen khoan luong thang", result.note)
    }

    @Test
    fun parseExpenseTechcombank() {
        val title = "Techcombank: TK 1903 bien dong"
        val text = "-150,000 VND. Noi dung: Mua tra sua"
        val packageName = ""

        val result = NotificationParser.parse(title, text, packageName)

        assertTrue(result.success)
        assertEquals(150000.0, result.amount, 0.0)
        assertEquals("EXPENSE", result.type)
        assertEquals("Techcombank", result.bankName)
        assertEquals("Mua tra sua", result.note)
    }

    @Test
    fun parseExpenseMomo() {
        val title = "Thanh toán thành công"
        val text = "Ban da thanh toan thanh cong so tien -50,000 d cho dich vu GrabFood qua vi MoMo"
        val packageName = "com.mservice.momo"

        val result = NotificationParser.parse(title, text, packageName)

        assertTrue(result.success)
        assertEquals(50000.0, result.amount, 0.0)
        assertEquals("EXPENSE", result.type)
        assertEquals("MoMo", result.bankName)
    }

    @Test
    fun parseMBVNDFormat() {
        val title = "MB_BANK: GD TK..."
        val text = "+200.000 VND luc 15:40. ND: Tien mung sinh nhat"
        val packageName = "com.mbmobile"

        val result = NotificationParser.parse(title, text, packageName)

        assertTrue(result.success)
        assertEquals(200000.0, result.amount, 0.0)
        assertEquals("MB Bank", result.bankName)
        assertEquals("Tien mung sinh nhat", result.note)
    }
}
