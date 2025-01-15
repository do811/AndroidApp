package com.example.layout

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

/**
 * exp. name["ja"] = "一般照明"
 *      name["en"] = "General lighting"
 */
typealias LangDict = Map<String, String>

/**
 * EchonetLiteObject
 * @param ipAddress IPアドレス 例："192.168.2.52"
 * @param eoj EOJ 3バイト 例：[0x02, 0x90, 0x01]
 */
class EchonetLiteObject<T : Number>(
    val id: Int,
    val ipAddress: InetAddress, eoj: List<T>,
    private val assetManager: android.content.res.AssetManager
) : IELObject {
    // 参考：https://qiita.com/miyazawa_shi/items/725bc5eb6590be72970d

    companion object {
        fun indent(n: Int): String {
            return "    ".repeat(n)
        }
    }

    private val definitions: Map<String, DeviceJson.Companion.Data>

    enum class DataType {
        STATE, UNIMPLEMENTED
    }

    data class EnumData(
        val edt: List<Byte>,
        val name: String,
        val descriptions: LangDict,
    ) {
        override fun toString(): String {
            return "edt: 0x${edt.joinToString(" ") { "%02X".format(it) }}, name: $name, descriptions: $descriptions"
        }

        fun getIndentString(n: Int): List<String> {
            val strList = mutableListOf<String>()
            strList.add("${indent(n)}edt: 0x${edt.joinToString(" ") { "%02X".format(it) }}")
            strList.add("${indent(n + 1)}name: $name")
            strList.add("${indent(n + 1)}descriptions: ")
            descriptions.forEach { strList.add("${indent(n + 2)}${it.key} : ${it.value}") }
            return strList
        }
    }

    data class Property(
        val epc: List<Byte>,
        val name: LangDict,
        val shortName: String,
        val descriptions: LangDict,
        val accessRule: DeviceJson.Companion.AccessRule,
        val type: DataType,
        val enumList: List<EnumData>?,
        val status: EnumData? = null,
    ) {
        override fun toString(): String {
            // 16進数で表示
            return "epc: 0x${epc.joinToString(" ") { "%02X".format(it) }}, name: $name, shortName: $shortName, descriptions: $descriptions, accessRule: $accessRule, type: $type, enumList: $enumList, status: $status"
        }

        fun getIndentString(n: Int): List<String> {
            val stringList = mutableListOf<String>()

            stringList.add("${indent(n)}epc : 0x${epc.joinToString(" ") { "%02X".format(it) }}")

            stringList.add("${indent(n + 1)}name : ")
            name.forEach { stringList.add("${indent(n + 2)}${it.key} : ${it.value}") }

            stringList.add("${indent(n + 1)}shortName : $shortName")

            stringList.add("${indent(n + 1)}descriptions : ")
            descriptions.forEach { stringList.add("${indent(n + 2)}${it.key} : ${it.value}") }

            stringList.add("${indent(n + 1)}accessRule : ")
            stringList.addAll(accessRule.getIndentString(n + 2))

            stringList.add("${indent(n + 1)}type : $type")

            if (enumList == null) {
                stringList.add("${indent(n + 1)}enumList : null")
            } else {
                stringList.add("${indent(n + 1)}enumList : ")
                enumList.forEach { stringList.addAll(it.getIndentString(n + 2)) }
            }

            if (status == null) {
                stringList.add("${indent(n + 1)}status : null")
            } else {
                stringList.add("${indent(n + 1)}status : ")
                stringList.addAll(status.getIndentString(n + 2))
            }

            return stringList
        }
    }


    val eoj: List<Byte>

    val name: LangDict
    val shortName: String
    val propertyList: List<Property>

    private val controller = listOf(0x05.toByte(), 0xFF.toByte(), 0x01)
    private val echonetLitePort = 3610
    val TID = listOf(0x09, 0x29.toByte())

    // epc to edtの形
    // 例： status[0x80] = 0x30
    //           "power" = "on"
    val status: MutableMap<Byte, Byte> = mutableMapOf()

    init {
        val inputStream = assetManager.open("definitions.json")
        val content = inputStream.bufferedReader().readText()
        val serializer = DeviceJson.Companion.Definition.serializer() // シリアライザを取得
        definitions = Json.decodeFromString(serializer, content).definitions // デコード
        inputStream.close()

        if (eoj.size != 3) {
            throw IllegalArgumentException("Echonet: eoj must be 3 bytes")
        }
        this.eoj = eoj.map { it.toByte() }

        // eojを用いてJsonからデータを取得
        val device = DeviceJson.getDeviceFromEoj(this.eoj, this.assetManager)
            ?: throw IllegalArgumentException("Echonet: device not found")
        // mapの初期化
        name = mapOf(
            "ja" to device.className.ja,
            "en" to device.className.en,
        )
        shortName = device.shortName
        propertyList = getProperties(device)
//        println(propertyList.map { it }.joinToString("\n") { it.toString() })
//        println(getIndentPropertyInfo())
    }

    override fun toString(): String {
        val status = status.map { (k, v) -> "$k: $v" }.joinToString(", ")
        return "{ip: $ipAddress, EOJ:0x${eoj.joinToString(" ") { "%02X".format(it) }}, status: $status}"
    }

    /**
     * インデント済みのプロパティ情報が書かれた文字列を取得する
     * @param n 元々のインデントの数（インデントの空白数ではない）
     * @return インデント済みのプロパティ情報
     */
    fun getIndentPropertyInfo(n: Int = 0): String {
        return propertyList.joinToString("\n----------------------------------------------\n") {
            it.getIndentString(n).joinToString("\n")
        }
    }

    /**
     * Deviceに対応するPropertyのリストを取得する
     * @param device Device
     * @return List<Property>
     */
    private fun getProperties(device: DeviceJson.Companion.Device): List<Property> {
        return device.elProperties.map { property ->
            // epcの"0x"を取り除く
            val epc = when (property.epc.substring(0, 2)) {
                "0x" -> property.epc.substring(2).hexStringToByteArray().toList()
                else -> property.epc.hexStringToByteArray().toList()
            }

            val name: LangDict = mapOf(
                "ja" to property.propertyName.ja,
                "en" to property.propertyName.en,
            )

            val descriptions: LangDict = mapOf(
                "ja" to property.descriptions.ja,
                "en" to property.descriptions.en,
            )

            val type = when {
                property.data.ref != null -> {
                    // refがある場合はdefinitionsを参照する
                    // #/definitions/state_ON-OFF_3031のうち、state_ON-OFF_3031を取り出す
                    val refName = property.data.ref.split("/").last()
                    val refType = definitions[refName]?.type
                    when (refType) {
                        "state" -> DataType.STATE
                        else -> DataType.UNIMPLEMENTED
                    }
                }

                property.data.type != null -> {
                    // refがない場合はdata.typeを参照する
                    when (property.data.type) {
                        "state" -> DataType.STATE
                        else -> DataType.UNIMPLEMENTED
                    }
                }

                else -> DataType.UNIMPLEMENTED
            }

            val enumDataList: List<EnumData>? = when (type) {
                DataType.STATE -> {
                    // refがある場合はdefinitionsを参照する
                    // refがない場合はdata.enumを見る
                    val ref = property.data.ref?.split("/")?.last()
                    val enums = definitions[ref]?.enum ?: property.data.enum

                    // enumの整形
                    enums?.map { enum ->
                        val edt = when (enum.edt.substring(0, 2)) {
                            // epcの"0x"を取り除く
                            "0x" -> enum.edt.substring(2).hexStringToByteArray().toList()
                            else -> enum.edt.hexStringToByteArray().toList()
                        }
                        EnumData(
                            edt, enum.name ?: "", mapOf(
                                "ja" to (enum.descriptions?.ja ?: ""),
                                "en" to (enum.descriptions?.en ?: ""),
                            )
                        )
                    }
                }

                else -> null
            }

            Property(
                epc, name, property.shortName, descriptions, property.accessRule, type, enumDataList
            )
        }
    }

    // 既存クラスへの追加メソッドたち
    private fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }
    private fun String.hexStringToByteArray(): ByteArray {
        return this.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * パケットを送信する
     * @return 送信したパケット（失敗時にはnull）
     */
    override fun sendEchonetPacket(elPacket: ELPacketData): DatagramPacket {
        val packet = ELFormat.makePacket(elPacket)
        val sendPacket = DatagramPacket(packet, packet.size, ipAddress, echonetLitePort)
        if (ipAddress.isMulticastAddress) {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (networkInterface in interfaces) {
                if (!networkInterface.supportsMulticast()) {
                    continue
                }

                val multicastSocket = MulticastSocket()
                multicastSocket.networkInterface = networkInterface
                multicastSocket.send(sendPacket)
                multicastSocket.close()
                break
            }
        } else {
            val sendUdpSocket = DatagramSocket()
            sendUdpSocket.send(sendPacket)
            sendUdpSocket.close()
        }
        println("送信完了")
        println("to: $ipAddress")

        return sendPacket
    }

    private fun <T> compareList(list1: List<T>, list2: List<T>): Boolean {
        return list1.zip(list2).all { (a, b) -> a == b }
    }

    fun compareEoj(eoj: List<Number>): Boolean {
        // インスタンスコードがあったら無視する
        val eoj = if (eoj.size == 3) eoj.dropLast(1) else eoj
        if (eoj.size != 2) return false

        val eojByte = eoj.map { it.toByte() }
        return compareList(this.eoj.dropLast(1), eojByte)
    }

    fun epcToString(epc: List<Byte>): String {
        return propertyList.find { compareList(it.epc, epc) }?.let { it.name["ja"] } ?: ""
    }

    fun edtToString(epc: List<Byte>, edt: List<Byte>): String? {
        return propertyList.find { compareList(it.epc, epc) }
            ?.enumList?.find { compareList(it.edt, edt) }
            ?.name
    }

    /**
     * Stringのepc, edtをEchonetLitePacketDataに変換する
     * epc, edtが存在しない場合にはnull
     */
    private fun makePacket(esv: ESV, epc: String, edt: String): ELPacketData? {
        val esvValue = when (esv) {
            ESV.SETI -> 0x60.toByte()
            ESV.SETC -> 0x61.toByte()
            ESV.GET -> 0x62.toByte()
        }
        val epcValue = propertyList.find { it.name["ja"] == epc }?.epc
            ?: propertyList.find { it.name["en"] == epc }?.epc
            ?: return null
        val edtValue = when (edt) {
            "" -> null
            // edtが見つからない場合
            else -> propertyList.find { it.name["ja"] == epc }?.enumList?.find { it.name == edt }?.edt
                ?: return null
        }
        return ELPacketData(
            InetAddress.getLocalHost(),
            TID,
            controller,
            this.eoj,
            esvValue,
            epcValue,
            edtValue
        )
    }

    override fun setI(epc: String, edt: String): ELPacketData? {
        val packet = makePacket(ESV.SETI, epc, edt) ?: return null
        return ELFormat.parsePacket(sendEchonetPacket(packet))
    }

    override fun setC(epc: String, edt: String): ELPacketData? {
        val packet = makePacket(ESV.SETC, epc, edt) ?: return null
        return ELFormat.parsePacket(sendEchonetPacket(packet))
    }

    override fun get(epc: String): ELPacketData? {
        val packet = makePacket(ESV.GET, epc, "") ?: return null
        return ELFormat.parsePacket(sendEchonetPacket(packet))
    }

    override suspend fun asyncSetI(epc: String, edt: String): ELPacketData? {
        return withContext(Dispatchers.IO) {
            setI(epc, edt)
        }
    }

    override suspend fun asyncSetC(epc: String, edt: String): ELPacketData? {
        return withContext(Dispatchers.IO) {
            setC(epc, edt)
        }
    }

    override suspend fun asyncGet(epc: String): ELPacketData? {
        return withContext(Dispatchers.IO) {
            get(epc)
        }
    }
}

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
//    val selfNodeInstanceList = EchonetLiteObject(InetAddress.getByName("224.0.23.0"), listOf(0x0E, 0xF0.toByte(), 0x01))
//    println(selfNodeInstanceList)
//    println()
    // selfNodeInstanceListを取得すると、自分の持っているEOJのリストが返ってくる
    // つまり、ネットワーク内の全てのEchonetLiteオブジェクトのリストが返ってくる
//    println(selfNodeInstanceList.get("selfNodeInstanceList"))
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

//    val socket = DatagramSocket(3610)
//    socket.soTimeout = 100
//    val buf = ByteArray(1024)
//    val packet = DatagramPacket(buf, buf.size)
//
//    for (i in 1..20) {
//        try {
//            socket.receive(packet)
//            val list = EchonetFormat.parseSelfNodeInstanceList(
//                EchonetFormat.parsePacket(
//                    packet, byteArrayOf(0x00, 0x0A)
//                )
//            )
//            println("応答を受け取りました:${list}\n")
//
//        } catch (_: SocketTimeoutException) {
//        }
//    }
//    socket.close()

//    val echonet = EchonetLiteManager()
//    val a = EchonetLiteObject(InetAddress.getByName("192.168.2.50"), listOf(0x02, 0x91, 0x01))
//    println(echonet.waitPacket(a.get("動作状態")))
//    println(echonet.waitPacket(a.setC("動作状態", "false")))
//    val b = EchonetLiteObject(InetAddress.getByName("224.0.23.0"), listOf(0x0E, 0xF0, 0x01))
//    println(echonet.waitPacket(b.get("自ノードインスタンスリストS")))

}
