package com.example.layout

import java.net.DatagramPacket

interface IEchonetLiteObject {
    /**
     * Echonetのパケットを作成し、送信する
     * @param esv
     * @param epc
     * @param edt これはnullでもよい
     * @return 送信したパケット
     */
    fun sendEchonetPacket(esv: ESV, epc: String, edt: String): DatagramPacket

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
