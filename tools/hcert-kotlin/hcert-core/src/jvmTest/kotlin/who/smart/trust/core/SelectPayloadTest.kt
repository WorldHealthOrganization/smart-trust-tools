package who.smart.trust.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import who.smart.trust.issuer.IssueError
import who.smart.trust.payloads.Payload
import who.smart.trust.payloads.RawPayload
import who.smart.trust.subclaims.PayloadKind
import who.smart.trust.types.Subclaim

/** Skill `hcert-select-payload`: QR Code Generation step 1. */
class SelectPayloadTest {
    @Test fun `STT-SEL-001 the registry holds the subclaims the guide defines`() {
        val reg = SubclaimRegistry()
        assertEquals(setOf(1, 3, 4, 5, -6, -7), reg.all().map { it.key }.toSet())
        assertEquals("http://smart.who.int/trust-phw/StructureDefinition/DVCMin", reg.lookup(-6)?.logicalModel)
        assertEquals(-6, reg.subclaimFor(PayloadKind.DVC_MIN))
        assertEquals(5, reg.subclaimFor(PayloadKind.VHL))
    }

    @Test fun `STT-SEL-002 a host registers its own subclaim and issues under it`() {
        val reg = SubclaimRegistry()
        assertNull(reg.lookup(42))
        val body = CborEncoder.encode(CborItem.Map(listOf(CborItem.Text("k") to CborItem.Text("v"))))
        assertEquals(IssueError.UnknownSubclaim(42), HcertIssuer.prepare(Testkit.request(Payload.Raw(RawPayload(42, body))), reg).err())
        reg.register(Subclaim(42, "Local payload"))
        Testkit.issue(Testkit.request(Payload.Raw(RawPayload(42, body))), reg)
    }

    @Test fun `STT-SEL-003 a development-range subclaim needs the explicit opt-in`() {
        val r = HcertIssuer.prepare(Testkit.request(Payload.DvcMin(NEWBORN), dev = false), SubclaimRegistry())
        assertEquals(IssueError.DevelopmentSubclaimNotAllowed(-6), r.err())
    }

    @Test fun `STT-SEL-004 an issued HCERT carries exactly one payload`() {
        val hcert = claimsOf(Testkit.issue(Payload.DvcMin(NEWBORN)).cose)[-260L] as CborItem.Map
        assertEquals(listOf(CborItem.Int(-6)), hcert.entries.map { it.first })
    }
}
