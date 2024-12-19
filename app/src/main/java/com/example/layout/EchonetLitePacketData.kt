package com.example.layout

import java.net.InetAddress

data class EchonetLitePacketData(
    val ipAddress: InetAddress,
    val tid: List<Byte>,
    val seoj: List<Byte>,
    val deoj: List<Byte>,
    val esv: Byte,
    val epc: List<Byte>,
    val edt: List<Byte>?,
) {
    override fun toString(): String {
        val tidHex = tid.joinToString(" ") { "%02X".format(it) }
        val seojHex = seoj.joinToString(" ") { "%02X".format(it) }
        val deojHex = deoj.joinToString(" ") { "%02X".format(it) }
        val esvHex = "%02X".format(esv)
        val epcHex = epc.joinToString(" ") { "%02X".format(it) }
        val edtHex = edt?.joinToString(" ") { "%02X".format(it) }

        return "ipAddress: $ipAddress, tid: [$tidHex], seoj: [$seojHex], deoj: [$deojHex], esv: $esvHex, epc: [$epcHex], edt: [$edtHex]"
    }
}
