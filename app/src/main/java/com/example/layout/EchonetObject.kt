import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException


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

class EchonetFormat {
    companion object {
        /**
         * esv, epc, edtからEchonetLiteのパケットを作成する
         * @param data EchonetLiteのデータ
         * @return EchonetLiteのパケット 例：[0x10, 0x81, 0x00, 0x0A, 0x05, 0xFF, 0x01, 0x02, 0x90, 0x01, 0x60, 0x01, 0x80, 0x01, 0x30]
         */
        fun makePacket(data: EchonetLitePacketData): ByteArray {
            val payload = mutableListOf(0x10, 0x81.toByte())
            payload.addAll(data.tid)
            payload.addAll(data.seoj)
            payload.addAll(data.deoj)
            payload.add(data.esv)
            payload.add(data.epc.size.toByte())
            payload.addAll(data.epc)
            // ノードプロファイルの場合などedtを送らないことがある
            data.edt?.let {
                payload.add(it.size.toByte())
                payload.addAll(it)
            } ?: run { payload.add(0) }
            return payload.map { it }.toByteArray()
        }

        /**
         * EchonetLiteのパケットを解析する
         * Tidの照合も行うことが可能
         * @param packet EchonetLiteのパケット
         * @param collectTid Tidの照合を行う場合は指定する
         * @return EchonetLiteのデータ
         * @throws IllegalArgumentException パケットが短すぎる、EchonetLiteでない、Tidが一致しない場合
         */
        fun parsePacket(
            packet: DatagramPacket,
            collectTid: ByteArray? = null
        ): EchonetLitePacketData {
            val data = packet.data
            if (data.size < 12) {
                throw IllegalArgumentException("Echonet: packet is too short")
            }
            if (data[0] != 0x10.toByte() || data[1] != 0x81.toByte()) {
                throw IllegalArgumentException("Echonet: packet is not EchonetLite")
            }
            if (collectTid != null) {
                if (collectTid.size != 2) {
                    throw IllegalArgumentException("Echonet: tid must be 2 bytes")
                }
                if (data[2] != collectTid[0] || data[3] != collectTid[1]) {
                    throw IllegalArgumentException("Echonet: tid is not match")
                }
            }

            val tid = data.slice(2 until 4).map { it }

            val seoj = data.slice(4 until 4 + 3).map { it }
            val deoj = data.slice(7 until 7 + 3).map { it }

            val esv = data[10]

            val epcSize = data[11]
            val epc = data.slice(12 until 12 + epcSize).map { it }

            val edtSizeIndex = 12 + epcSize
            val edtSize = data[edtSizeIndex]

            val edt = if (0 < edtSize) {
                data.slice(edtSizeIndex + 1 until edtSizeIndex + 1 + edtSize).map { it }
            } else {
                null
            }
            return EchonetLitePacketData(packet.address, tid, seoj, deoj, esv, epc, edt)
        }

        fun parseSelfNodeInstanceList(data: EchonetLitePacketData): List<EchonetObject<Number>> {
            if (data.esv != 0x72.toByte()) {
                throw IllegalArgumentException("Echonet: esv is not 0x72")
            }
            val edt = data.edt ?: return listOf()
            val num = edt.get(0).toInt()
            val list = mutableListOf<EchonetObject<Number>>()
            for (i in 1..num) {
                list.add(EchonetObject(data.ipAddress, edt.slice(1 + (i - 1) * 3 until 1 + i * 3)))
            }
            return list
        }

        fun parseSelfNodeInstanceList(
            packet: DatagramPacket,
            collectTid: ByteArray?
        ): List<EchonetObject<Number>> {
            return parseSelfNodeInstanceList(parsePacket(packet, collectTid))
        }
    }
}

interface IEchonetObject {
    /**
     * Echonetのパケットを作成し、送信する
     * @param esv
     * @param epc
     * @param edt これはnullでもよい
     * @return 送信したパケット
     */
    fun sendEchonetPacket(esv: String, epc: String, edt: String): DatagramPacket

    /**
     * 任意のEPCのEDTをセットする 応答を受け取らない
     * @param epc EPCの値
     * @param edt EDTの値
     */
    fun setI(epc: String, edt: String): EchonetLitePacketData

    /**
     * 任意のEPCのEDTをセットする 応答を受け取る
     * @param epc EPCの値
     * @param edt EDTの値
     * @param timeout タイムアウト時間(ms) デフォルトは5秒
     * @return 応答（今のところ受け取ったパケットそのまま）
     */
    fun setC(epc: String, edt: String, timeout: Int = 5000): EchonetLitePacketData

    /**
     * 任意のEPCのEDTを知る
     * @param epc EPCの値
     * @return 応答（今のところ受け取ったパケットそのまま）
     */
    fun get(epc: String): EchonetLitePacketData

    suspend fun asyncSetC(epc: String, edt: String, timeout: Int = 5000): EchonetLitePacketData
}

/**
 * EchonetObject
 * @param ipAddress IPアドレス 例："192.168.2.52"
 * @param eoj EOJ 3バイト 例：[0x02, 0x90, 0x01]
 */
class EchonetObject<T : Number>(val ipAddress: InetAddress, eoj: List<T>) : IEchonetObject {
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
        EchonetObject(InetAddress.getByName("224.0.23.0"), listOf(0x0E, 0xF0.toByte(), 0x01))
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
