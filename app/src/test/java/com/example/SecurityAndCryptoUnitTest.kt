package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.TronPaymentService
import com.example.security.SecurityGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAndCryptoUnitTest {

    @Test
    fun testTronWalletAddressConfiguredCorrectly() {
        assertEquals("TBMvBiVB6mhu1gnaAAE1Pg5YohKvV1NSnB", TronPaymentService.TRON_WALLET_ADDRESS)
        assertEquals(4.0, TronPaymentService.getPlanAmountUsdt("plan_1month"), 0.01)
        assertEquals(10.0, TronPaymentService.getPlanAmountUsdt("plan_3months"), 0.01)
        assertEquals(30.0, TronPaymentService.getPlanAmountUsdt("plan_1year"), 0.01)
    }

    @Test
    fun testAntiReplayTxidProtection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyTxid = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"

        assertFalse(SecurityGuard.isTxidUsed(context, dummyTxid))

        SecurityGuard.recordUsedTxid(context, dummyTxid)
        assertTrue(SecurityGuard.isTxidUsed(context, dummyTxid))
        // Case-insensitivity check
        assertTrue(SecurityGuard.isTxidUsed(context, dummyTxid.uppercase()))
    }

    @Test
    fun testTamperProofVipSignature() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Initially no VIP
        SecurityGuard.revokeVip(context)
        assertFalse(SecurityGuard.verifyVipIntegrity(context))

        // Commit cryptographically signed VIP token
        SecurityGuard.commitVerifiedVip(context, "plan_1month", 30)
        assertTrue(SecurityGuard.verifyVipIntegrity(context))
    }

    @Test
    fun testClockIntegrityMonitoring() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(SecurityGuard.checkClockIntegrity(context))
    }

    @Test
    fun testFastestServerSelection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.vpn.VpnStateManager.initialize(context)

        val fastest = com.example.vpn.VpnStateManager.selectFastestServer()
        assertTrue(fastest.id.isNotEmpty())
    }
}
