package com.example.layout

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException


/**
 * EchonetLiteObject
 * @param ipAddress IPアドレス 例："192.168.2.52"
 * @param eoj EOJ 3バイト 例：[0x02, 0x90, 0x01]
 */
class EchonetLiteObject<T : Number>(val ipAddress: InetAddress, eoj: List<T>) : IEchonetLiteObject {
    val eoj: List<Byte> = eoj.map { it.toByte() }
    // 参考：https://qiita.com/miyazawa_shi/items/725bc5eb6590be72970d

    private val controller = listOf(0x05.toByte(), 0xFF.toByte(), 0x01)
    private val echonetLitePort = 3610
    val stringToEsv: MutableMap<String, Byte> = mutableMapOf()
    val stringToEpc: MutableMap<String, List<Byte>> = mutableMapOf()
    val stringToEdt: MutableMap<Pair<String, String>, List<Byte>> = mutableMapOf()

    // epc to edtの形
    // 例： status[0x80] = 0x30
    //           "power" = "on"
    val status: MutableMap<Byte, Byte> = mutableMapOf()

    init {
        if (eoj.size != 3) {
            throw IllegalArgumentException("Echonet: eoj must be 3 bytes")
        }

        stringToEsv["setI"] = 0x60
        stringToEsv["setC"] = 0x61
        stringToEsv["get"] = 0x62

        stringToEpc["power"] = listOf(0x80.toByte())

        stringToEdt["power" to "on"] = listOf(0x30)
        stringToEdt["power" to "off"] = listOf(0x31)

        // eojによって使用可能なepc, edtを変更
        when {
            this.eoj[0] == 0x02.toByte() && this.eoj[1] == 0x91.toByte() -> monoLite()
            this.eoj[0] == 0x0E.toByte() && this.eoj[1] == 0xF0.toByte() -> nodeProfile()
        }
    }

    private fun monoLite() {
        println("monoLite")
        stringToEpc["liteLevel"] = listOf(0xB0.toByte())
        stringToEdt["liteLevel" to "0"] = listOf(0x00)
        stringToEdt["liteLevel" to "10"] = listOf(0x0A)
        stringToEdt["liteLevel" to "20"] = listOf(0x14)
        stringToEdt["liteLevel" to "30"] = listOf(0x1E)
        stringToEdt["liteLevel" to "40"] = listOf(0x28)
        stringToEdt["liteLevel" to "50"] = listOf(0x32)
        stringToEdt["liteLevel" to "60"] = listOf(0x3C)
        stringToEdt["liteLevel" to "70"] = listOf(0x46)
        stringToEdt["liteLevel" to "80"] = listOf(0x50)
        stringToEdt["liteLevel" to "90"] = listOf(0x5A)
        stringToEdt["liteLevel" to "100"] = listOf(0x64)
    }

    private fun nodeProfile() {
        stringToEpc["power"] = listOf(0x80.toByte())
        stringToEdt["power" to "on"] = listOf(0x30)
        stringToEdt["power" to "off"] = listOf(0x31)
        stringToEpc["selfNodeInstanceList"] = listOf(0xD6.toByte())
    }

    // 既存クラスへの追加メソッドたち
    private fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }
    private fun String.hexStringToByteArray(): ByteArray {
        return this.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun <K, V> MutableMap<K, V>.getKey(value: V): K? {
        for ((key, mapValue) in this) {
            if (mapValue == value) {
                return key
            }
        }
        return null
    }

    fun printStatus() {
//        val status = status.map { (k, v) -> "${stringToEpc.getKey(listOf(k))}: ${stringToEdt.getKey(listOf(v))}" }
//            .joinToString(", ")
        val status = status.map { (k, v) -> "${stringToEdt.getKey(listOf(v))}" }
            .joinToString(", ")
        println(status)
    }

    override fun sendEchonetPacket(esv: String, epc: String, edt: String): DatagramPacket {
        // esv, epcはnullチェックをする。edtはnullでもよい
        val esvValue = stringToEsv[esv] ?: throw IllegalArgumentException("Echonet: esv is null")
        val epcValue = stringToEpc[epc] ?: throw IllegalArgumentException("Echonet: epc is null")
        val packet = EchonetFormat.makePacket(
            EchonetLitePacketData(
                InetAddress.getLocalHost(),
                listOf(0x00, 0x0A.toByte()),
                controller,
                this.eoj,
                esvValue,
                epcValue,
                stringToEdt[epc to edt]
            )
        )

        println(packet.toHexString())

        val sendUdpSocket = DatagramSocket()
        val sendPacket = DatagramPacket(packet, packet.size, ipAddress, echonetLitePort)
        sendUdpSocket.send(sendPacket)
        sendUdpSocket.close()
        println("送信完了")
        println("to: $ipAddress")
        return sendPacket
    }

    override fun setI(epc: String, edt: String): EchonetLitePacketData {
        return EchonetFormat.parsePacket(sendEchonetPacket("setI", epc, edt))
    }

    override fun setC(epc: String, edt: String, timeout: Int): EchonetLitePacketData {
        return EchonetFormat.parsePacket(sendEchonetPacket("setC", epc, edt))
    }

    override fun get(epc: String): EchonetLitePacketData {
        return EchonetFormat.parsePacket(sendEchonetPacket("get", epc, ""))
    }

    override fun toString(): String {
        val status = status.map { (k, v) -> "$k: $v" }.joinToString(", ")
        return "{ip: $ipAddress, EOJ:${eoj.joinToString(" ") { "%02X".format(it) }}, status: $status}"
    }

    suspend fun asyncSetI(epc: String, edt: String): EchonetLitePacketData {
        return withContext(Dispatchers.IO) {
            setI(epc, edt)
        }
    }

    override suspend fun asyncSetC(epc: String, edt: String, timeout: Int): EchonetLitePacketData {
        return withContext(Dispatchers.IO) {
            setC(epc, edt, timeout)
        }
    }

    suspend fun asyncGet(epc: String): EchonetLitePacketData {
        return withContext(Dispatchers.IO) {
            get(epc)
        }
    }
}

//class EchonetStatus(val ipAddress: InetAddress, val eoj: List<Byte>) {
//    var status: MutableMap<String, String> = mutableMapOf()
//}

fun main() {
    // ipアドレスが192.168.2.52で、EOJが0x029001（単機能照明）のEchonetLiteオブジェクトを作成
    // 電源をONにし、明るさを100%に設定
//    val monoLite = StubEchonetObject("192.168.0.22", listOf(0x02, 0x90, 0x01))
    // setIはただ設定するだけで、応答を受け取らない
//    monoLite.setI("power", "on")
    // setCは設定して応答を受け取る
//    println(monoLite.setC("liteLevel", "100").joinToString { "%02X".format(it) })

    // ipアドレスが224.0.23.0で、EOJが0x0EF001（ノードプロファイル）のEchonetLiteオブジェクトを作成
    // 224.0.23.0はマルチキャストアドレス
    val selfNodeInstanceList =
        EchonetLiteObject(InetAddress.getByName("224.0.23.0"), listOf(0x0E, 0xF0.toByte(), 0x01))
    println(selfNodeInstanceList)
    println()
    // selfNodeInstanceListを取得すると、自分の持っているEOJのリストが返ってくる
    // つまり、ネットワーク内の全てのEchonetLiteオブジェクトのリストが返ってくる
    println(selfNodeInstanceList.get("selfNodeInstanceList"))
    // 取得した後解析してオブジェクトのリストを作成する処理が必要。未実装

    //  ip  :224.0.23.0
    //  DEOJ:0E,F0,01
    //  ESV :62(GET)
    //  EPC :D6
    //  PDC :00
    //  EDT :00(なし)
    //  を送るとネットワーク内のnodejsにアクセスでき、それぞれの機器のIPから
    //  ESV:72
    //  EDT:["IP内の機器の数", クラスグループコード,クラスコード,インスタンスコード, クラスグループコード,,,,]
    //  が返ってくる
    //
    //  ex.
    //  EDT[] = { 0x05, 0x02, 0x90, 0x01, 0x02, 0x90, 0x02, 0x02, 0x90, 0x03, 0x02, 0x90, 0x04, 0x02, 0xA3, 0x01 };
    //  [0]:機器が五個
    //  [1]~[12]:[02,90,xx]つまり一般照明が4個
    //  [13]~[15]:[02,A3,xx]つまりシステム照明が1個

    val socket = DatagramSocket(3610)
    socket.soTimeout = 100
    val buf = ByteArray(1024)
    val packet = DatagramPacket(buf, buf.size)

    for (i in 1..20) {
        try {
            socket.receive(packet)
            val list =
                EchonetFormat.parseSelfNodeInstanceList(
                    EchonetFormat.parsePacket(
                        packet,
                        byteArrayOf(0x00, 0x0A)
                    )
                )
            println("応答を受け取りました:${list}\n")

        } catch (_: SocketTimeoutException) {
        }
    }
    socket.close()
}

// 非同期で常に通信を読んでおく
// Echonetの通信だったら、リストにIPとデータのセットを追加する
// そのリストを定期的に読んで、データを解析する
// そのデータを元に、EchonetObjectのstatusを更新する
