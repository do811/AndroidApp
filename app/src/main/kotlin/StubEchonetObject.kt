import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

interface _StubEchonetObject {
    /**
     * Echonetのパケットを作成し、送信する（つもり）
     * @param esv
     * @param epc
     * @param edt これはnullでもよい
     * @return 送信したパケット
     */
    fun sendEchonetPacket(esv: String, epc: String, edt: String): ByteArray

    /**
     * 任意のEPCのEDTをセットする 応答を受け取らない
     * @param epc EPCの値
     * @param edt EDTの値
     */
    fun setI(epc: String, edt: String)

    /**
     * 任意のEPCのEDTをセットする 応答を受け取る
     * @param epc EPCの値
     * @param edt EDTの値
     * @param timeout タイムアウト時間(ms) デフォルトは5秒
     * @return 応答（今のところ受け取ったパケットそのまま）
     */
    fun setC(epc: String, edt: String, timeout: Int = 5000): ByteArray

    /**
     * 任意のEPCのEDTを知る
     * @param epc EPCの値
     * @return 応答（今のところ受け取ったパケットそのまま）
     */
    fun get(epc: String): ByteArray
}

/**
 * EchonetObjectのスタブ
 * 通信部分はダミー処理。それ以外の部分を実装している
 * @param ipAddress IPアドレス 例："192.168.2.52"
 * @param eoj EOJ 3バイト 例：[0x02, 0x90, 0x01]
 */
class StubEchonetObject(private val ipAddress: String, private val eoj: List<Int>) :
    _StubEchonetObject {
    // 参考：https://qiita.com/miyazawa_shi/items/725bc5eb6590be72970d

    private val controller = listOf(0x05, 0xFF, 0x01)
    private val echonetLitePort = 3610
    private val esvMap: MutableMap<String, Int> = mutableMapOf()
    private val epcMap: MutableMap<String, List<Int>> = mutableMapOf()
    private val edtMap: MutableMap<Pair<String, String>, List<Int>> = mutableMapOf()

    init {
        if (eoj.size != 3) {
            throw IllegalArgumentException("Echonet: eoj must be 3 bytes")
        }

        esvMap["setI"] = 0x60
        esvMap["setC"] = 0x61
        esvMap["get"] = 0x62

        epcMap["power"] = listOf(0x80)

        edtMap["power" to "on"] = listOf(0x30)
        edtMap["power" to "off"] = listOf(0x31)

        // eojによって使用可能なepc, edtを変更
        when {
            eoj[0] == 0x02 && eoj[1] == 0x90 -> monoLite()
            eoj[0] == 0x0E && eoj[1] == 0xF0 -> nodeProfile()
        }
    }

    private fun monoLite() {
        epcMap["lightLevel"] = listOf(0xB0)
        edtMap["lightLevel" to "0"] = listOf(0x00)
        edtMap["lightLevel" to "10"] = listOf(0x0A)
        edtMap["lightLevel" to "20"] = listOf(0x14)
        edtMap["lightLevel" to "30"] = listOf(0x1E)
        edtMap["lightLevel" to "40"] = listOf(0x28)
        edtMap["lightLevel" to "50"] = listOf(0x32)
        edtMap["lightLevel" to "60"] = listOf(0x3C)
        edtMap["lightLevel" to "70"] = listOf(0x46)
        edtMap["lightLevel" to "80"] = listOf(0x50)
        edtMap["lightLevel" to "90"] = listOf(0x5A)
        edtMap["lightLevel" to "100"] = listOf(0x64)
    }

    private fun nodeProfile() {
        epcMap["power"] = listOf(0x80)
        edtMap["power" to "on"] = listOf(0x30)
        edtMap["power" to "off"] = listOf(0x31)
        epcMap["selfNodeInstanceList"] = listOf(0xD6)
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
    private fun String.hexStringToByteArray(): ByteArray {
        return this.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * esv, epc, edtからEchonetLiteのパケットを作成する
     * @param esv 例：0x60
     * @param epc 例：[0x80]
     * @param edt 例：[0x30]
     * @return EchonetLiteのパケット 例：[0x10, 0x81, 0x00, 0x0A, 0x05, 0xFF, 0x01, 0x02, 0x90, 0x01, 0x60, 0x01, 0x80, 0x01,
     * 0x30]
     */
    private fun makeEchonetPacket(esv: Int, epc: List<Int>, edt: List<Int>?): ByteArray {
        val payload = mutableListOf(0x10, 0x81, 0x00, 0x0A)
        payload.addAll(controller)
        payload.addAll(eoj)
        payload.add(esv)
        payload.add(epc.size)
        payload.addAll(epc)
        // ノードプロファイルの場合などedtを送らないことがある
        edt?.let {
            payload.add(it.size)
            payload.addAll(it)
        } ?: run { payload.add(0) }
        return payload.map { it.toByte() }.toByteArray()
    }

    override fun sendEchonetPacket(esv: String, epc: String, edt: String): ByteArray {
        // esv, epcはnullチェックをする。edtはnullでもよい
        val esvValue = esvMap[esv] ?: throw IllegalArgumentException("Echonet: esv is null")
        val epcValue = epcMap[epc] ?: throw IllegalArgumentException("Echonet: epc is null")
        val packet = makeEchonetPacket(esvValue, epcValue, edtMap[epc to edt])

        println(packet.toHexString())

        val sendUdpSocket = DatagramSocket()
        val ipAddress = InetAddress.getByName(ipAddress)
        val sendPacket = DatagramPacket(packet, packet.size, ipAddress, echonetLitePort)
        sendUdpSocket.send(sendPacket)
        sendUdpSocket.close()
        println("送信完了")
        println("to: $ipAddress")
        return packet
    }

    override fun setI(epc: String, edt: String) {
//        sendEchonetPacket("setI", epc, edt)
    }

    override fun setC(epc: String, edt: String, timeout: Int): ByteArray {
//        sendEchonetPacket("setC", epc, edt)
        // ここに応答を待つ処理が入り、あれば応答をreturnする。
        // 応答は今のところByteArrayだが、もう少し分かりやすい形（esv等の形式？）にするかもしれない。
        return listOf(
            0x10,
            0x81,
            0x00,
            0x0A,
            0x02,
            0x91,
            0x01,
            0x71,
            0x01,
            0x80
        ).map { it.toByte() }.toByteArray()
    }

    override fun get(epc: String): ByteArray {
//        sendEchonetPacket("get", epc, "")
        // ここに応答を待つ処理が入り、あれば応答をreturnする。
        // 応答は今のところByteArrayだが、もう少し分かりやすい形（esv等の形式？）にするかもしれない。
        return listOf(
            0x10,
            0x81,
            0x00,
            0x0A,
            0x02,
            0x91,
            0x01,
            0x71,
            0x01,
            0x80
        ).map { it.toByte() }.toByteArray()
    }
}


fun main() {
    // ipアドレスが192.168.2.52で、EOJが0x029001（単機能照明）のEchonetLiteオブジェクトを作成
    // 電源をONにし、明るさを100%に設定
    val monoLite = StubEchonetObject("192.168.2.52", listOf(0x02, 0x90, 0x01))
    // setIはただ設定するだけで、応答を受け取らない
    monoLite.setI("power", "on")
    // setCは設定して応答を受け取る
    println(monoLite.setC("lightLevel", "100").joinToString { "%02x".format(it) })

    // ipアドレスが224.0.23.0で、EOJが0x0EF001（ノードプロファイル）のEchonetLiteオブジェクトを作成
    // 224.0.23.0はマルチキャストアドレス
    val selfNodeInstanceList = StubEchonetObject("224.0.23.0", listOf(0x0E, 0xF0, 0x01))
    // selfNodeInstanceListを取得すると、自分の持っているEOJのリストが返ってくる
    // つまり、ネットワーク内の全てのEchonetLiteオブジェクトのリストが返ってくる
    println(selfNodeInstanceList.get("selfNodeInstanceList").joinToString { "%02x".format(it) })
    // 取得した後解析してオブジェクトのリストを作成する処理が必要。未実装
}
