import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class EchonetManager {
    var deviceList: List<EchonetObject<Number>> = mutableListOf()
        private set // デバイスリストは外部からの変更を許可しない

    private val timeout = 4000
    private val echonetLitePort = 3610

    val packetList: ArrayDeque<EchonetLitePacketData> = ArrayDeque()

    private var isReading = false

    init {
//        getDeviceList()
    }

    fun getDeviceList() {
        deviceList = mutableListOf()

        val selfNodeInstanceList = EchonetObject(InetAddress.getByName("224.0.23.0"), listOf(0x0E, 0xF0, 0x01))
        selfNodeInstanceList.get("selfNodeInstanceList")

        val socket = DatagramSocket(3610)
        socket.soTimeout = 100
        val buf = ByteArray(1024)
        val packet = DatagramPacket(buf, buf.size)

        for (i in 1..timeout / socket.soTimeout) {
            try {
                socket.receive(packet)
                val list =
                    EchonetFormat.parseSelfNodeInstanceList(EchonetFormat.parsePacket(packet, byteArrayOf(0x00, 0x0A)))
                println("応答を受け取りました:${list}\n")
                deviceList = deviceList.plus(list)
            } catch (_: SocketTimeoutException) {
//                println("timeout")
            }
        }
        socket.close()

        println("検索終了")
        println("検索結果:${deviceList}")
    }


    suspend fun asyncGetDeviceList() {
        withContext(Dispatchers.IO) {
            getDeviceList()
        }
    }

    fun readPacket() {
        this.isReading = true
        val socket = DatagramSocket(echonetLitePort)
        socket.soTimeout = 100
        val buf = ByteArray(1024)
        val packet = DatagramPacket(buf, buf.size)

        while (isReading) {
            try {
                socket.receive(packet)

                val recv = EchonetFormat.parsePacket(packet, byteArrayOf(0x00, 0x0A))
//                println(recv)
                packetList.add(recv)
            } catch (_: SocketTimeoutException) {
            } catch (_: IllegalArgumentException) {
                println("TIDが違います")
            }
        }
        socket.close()
    }

    suspend fun asyncReadPacket() {
        withContext(Dispatchers.IO) {
            readPacket()
        }
    }

    fun stopReadPacket() {
        this.isReading = false
    }

    /**
     * 引数のパケットに対する返答を待つ。こちらから送信したパケットを引数にし、それへの返答を受け取ることを想定。
     * tidの一致, seojとdeojの一致（送受信者逆転）, esvの一致を確認する。
     */
    fun waitPacket(data: EchonetLitePacketData, timeout: Int = 2000): EchonetLitePacketData? {
        var returnValue: EchonetLitePacketData? = null
        val tid = data.tid
        val seoj = data.deoj
        val deoj = data.seoj
        if (data.esv != 0x61.toByte() && data.esv != 0x62.toByte()) {
            throw IllegalArgumentException("Echonet: esv is not 0x61 or 0x71 but ${"%02X".format(data.esv)}")
        }
        val esv = if (data.esv == 0x61.toByte()) 0x71.toByte() else 0x72.toByte()

        val socket = DatagramSocket(echonetLitePort)
        socket.soTimeout = 100
        val buf = ByteArray(1024)
        val packet = DatagramPacket(buf, buf.size)

        for (i in 1..timeout / socket.soTimeout) {
            try {
                socket.receive(packet)

                val recv = EchonetFormat.parsePacket(packet, byteArrayOf(0x00, 0x0A))
//                println(recv)

                if (recv.tid != tid) {
                    println("TIDが違います")
                    continue
                }
                if (recv.seoj != seoj) {
                    println("SEOJが違います")
                    continue
                }
                if (recv.deoj != deoj) {
                    println("DEOJが違います")
                    continue
                }
                if (recv.esv != esv) {
                    println("ESVが違います")
                    continue
                }

                returnValue = recv
                break
            } catch (_: SocketTimeoutException) {
            } catch (_: IllegalArgumentException) {
                println("TIDが違います")
            }
        }

        socket.close()
        return returnValue
    }

    /**
     * Coroutine版のwaitPacket
     * suspend関数のため、呼び出すとlaunch内でのみ処理が止まる。
     * @param data: EchonetLitePacketData
     * @param timeout: Int
     * @return EchonetLitePacketData?
     */
    suspend fun asyncWaitPacket(data: EchonetLitePacketData, timeout: Int = 2000): EchonetLitePacketData? {
        return withContext(Dispatchers.IO) {
            waitPacket(data, timeout)
        }
    }
}

fun main() {
    val manager = EchonetManager()
    // runBlocking はCoroutineBuilderの一つで、None-Coroutineの世界からCoroutineの世界へのエントリポイント
    // None-Coroutineの世界でlaunchなどのCoroutineBuilderは使えない
//    runBlocking {
//        val task = launch {
//            println("end")
//        }
//        println("Hello, World!")
//        task.join() // taskが終わるまで待つ
//        println("Goodbye, World!")
//    } // Hello, World! -> end -> Goodbye, World!


//    EchonetManager()

    // ip: /192.168.2.50, EOJ:02 91 01
//    val monoLite = EchonetObject(InetAddress.getByName("192.168.2.50"), listOf(0x02, 0x91, 0x01))
//    monoLite.get("power")
//
//    val timeout = 2000
//    val socket = DatagramSocket(3610)
//    socket.soTimeout = 100
//    val buf = ByteArray(1024)
//    val packet = DatagramPacket(buf, buf.size)
//
//    for (i in 1..timeout / socket.soTimeout) {
//        try {
//            socket.receive(packet)
//
//            val data = EchonetFormat.parsePacket(packet, byteArrayOf(0x00, 0x0A))
//            println("応答を受け取りました:${data}\n")
//            monoLite.status[0x80.toByte()] = data.edt?.get(0) ?: 0x00.toByte()
//
//        } catch (_: SocketTimeoutException) {
//        } catch (_: IllegalArgumentException) {
//            println("TIDが違います")
//        }
//    }
//
//    monoLite.get("liteLevel")
//
//    for (i in 1..timeout / socket.soTimeout) {
//        try {
//            socket.receive(packet)
//            val data = EchonetFormat.parsePacket(packet, byteArrayOf(0x00, 0x0A))
//            println("応答を受け取りました:${data}\n")
//            monoLite.status[0xB0.toByte()] = data.edt?.get(0) ?: 0x00.toByte()
//
//        } catch (_: SocketTimeoutException) {
//        } catch (_: IllegalArgumentException) {
//            println("TIDが違います")
//        }
//    }
//    socket.close()
//
//    monoLite.printStatus()


    val monoLite = EchonetObject(InetAddress.getByName("192.168.2.50"), listOf(0x02, 0x91, 0x01))
    println("aaaaa")
    println(manager.waitPacket(monoLite.setC("power", "off")))

}

// コルーチンに関する資料
// https://zenn.dev/bookstore/articles/kotlin-coroutine-getting-started
// https://qiita.com/kawmra/items/d024f9ab32ffe0604d39
// https://qiita.com/naoi/items/9892db4cec2e9c0f6114
