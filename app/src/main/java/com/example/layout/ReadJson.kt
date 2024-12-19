package com.example.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

// こっちのがいい説
// https://echonet.jp/web_api_guideline/

@Serializable
data class StringJaEn(
    val ja: String,
    val en: String,
)

class DeviceJson {
    companion object {

        @Serializable
        data class Definition(
            val definitions: Map<String, Data>,
        )

        @Serializable
        data class Device(
            val eoj: String,
            val validRelease: ValidRelease,
            val className: StringJaEn,
            val shortName: String,
            val elProperties: List<Epc>,
        )

        @Serializable
        data class Epc(
            val epc: String,
            val validRelease: ValidRelease,
            val propertyName: StringJaEn,
            val shortName: String,
            val accessRule: AccessRule,
            val descriptions: StringJaEn,
            val data: Data,
            val note: StringJaEn? = null,
            val remark: StringJaEn? = null,
            val atomic: String? = null,
        )

        @Serializable
        data class ValidRelease(
            val from: String,
            val to: String,
        )

        @Serializable
        data class AccessRule(
            val get: String,
            val set: String,
            val inf: String,
        ) {
            fun getIndentString(n: Int): List<String> {
                return listOf(
                    "    ".repeat(n) + "get: $get",
                    "    ".repeat(n) + "set: $set",
                    "    ".repeat(n) + "inf: $inf",
                )
            }
        }

        @Serializable
        data class Data(
            @SerialName("\$ref") val ref: String? = null,

            // [null, array, state, object, bitmap, date, date-time, numericValue, time]
            val type: String? = null,

            val properties: List<Property>? = null,
            val oneOf: List<Data>? = null,
            val coefficient: List<String>? = null,
            val overflowCode: Boolean? = null,
            val underflowCode: Boolean? = null,


            // type=state
            @Serializable() val enum: List<Enum>? = null,
            val size: Int? = null,

            // type=array
            val itemSize: Int? = null,
            val minItems: Int? = null,
            val maxItems: Int? = null,
            val items: Data? = null,

            // type=bitmap
            val bitmaps: List<Bitmap>? = null,

            //type=number(definitions.jsonにのみ存在する)
            // Definitions.companion.Definition == Dataなような気がする。
            val format: String? = null,
            val minimum: Long? = null,
            val maximum: Long? = null,
            val unit: String? = null,
            val multipleOf: Double? = null,
            val multiple: Double? = null,


            // type=level
            val base: String? = null,

            // type=raw
            val minSize: Int? = null,
            val maxSize: Int? = null,

            // type=time
            val maximumOfHour: Int? = null,
        ) {
            init {
                if (ref != null) {
                    if (DataTypeList["ref"] == null) {
                        DataTypeList["ref"] = 1
                    } else {
                        DataTypeList["ref"] = DataTypeList["ref"]!! + 1
                    }
                }
                if (type != null) {
                    if (DataTypeList[type] == null) {
                        DataTypeList[type] = 1
                    } else {
                        DataTypeList[type] = DataTypeList[type]!! + 1
                    }
                }
            }
        }

        val DataTypeList = mutableMapOf<String, Int>()

        @Serializable
        data class Property(
            val elementName: StringJaEn,
            val shortName: String,
            val element: Data,
        )

        @Serializable
        data class Element(
            @SerialName("\$ref") val ref: String? = null,
            val oneOf: List<Data>? = null,
        )


        @Serializable
        data class Enum(
            val edt: String,
            val name: String? = null,
            val descriptions: StringJaEn? = null,

            // Data.type=numericValue
            val numericValue: Double? = null,

            // Definitionsにのみ存在する
            val readOnly: Boolean? = null,
        )

        @Serializable
        data class Bitmap(
            val name: String,
            val descriptions: StringJaEn,
            val position: Position,
            val value: Data,
        )

        @Serializable
        data class Position(
            val index: Int,
            val bitMask: String,
        )

        /**
         * EOJからデバイス情報を取得する
         * インスタンスコードはあってもなくても良い
         * @param eoj EOJ 例：[0x02, 0x90, 0x01]
         * @return Device
         * @throws IllegalArgumentException eojが2または3バイトでない場合
         */
        fun getDeviceFromEoj(
            eoj: List<Byte>,
            assetManager: android.content.res.AssetManager
        ): Device? {
//            println("opening:${"0x${eoj.joinToString("") { "%02X".format(it) }}.json"}")
            if (eoj.size == 3) {
                // インスタンスコードがある場合は削除
                return getDeviceFromEoj(eoj.dropLast(1), assetManager)
            } else if (eoj.size != 2) {
                throw IllegalArgumentException("Echonet: eoj must be 2 or 3 bytes")
            }

            val eojHex = eoj.joinToString("") { "%02X".format(it) }
            return getDeviceFromEoj(eojHex, assetManager)
        }

        /**
         * EOJからデバイス情報を取得する
         * @param eoj EOJ 例：0290
         * @return Device
         * @throws IllegalArgumentException ファイルが見つからない場合
         */
        fun getDeviceFromEoj(eoj: String, assetManager: android.content.res.AssetManager): Device? {
//            val file = Device::class.java.getResource(PATH + "0x${eoj}.json")
//                ?: throw IllegalArgumentException("File not found: 0x${eoj}.json")

            try {
                val inputStream = assetManager.open("0x${eoj}.json")

                val content = inputStream.bufferedReader().readText()
                val serializer = Device.serializer() // シリアライザを取得
                return Json.decodeFromString(serializer, content)
            } catch (e: Exception) {
                return null
            }
        }
    }
}
//
//class DefinitionJson {
//    companion object {
//
//        @Serializable
//        data class Root(
//            val definitions: Map<String, Definition>,
//        )
//
//        @Serializable
//        data class Definition(
//// Definition == DeviceJson.Dataなような気がする。
//            val type: String,
//
//            // type=number
//            val format: String? = null,
//            val minimum: Long? = null,
//            val maximum: Long? = null,
//            val unit: String? = null,
//            val multipleOf: Double? = null,
//            val multiple: Double? = null,
//
//            // type=state
//            @Serializable(with = EnumOrIntSerializer::class)
//            val enum: List<DeviceJson.Companion.EnumOrInt>? = null,
//            val size: Int? = null,
//
//            // type=level
//            val base: String? = null,
//
//            // type=raw
//            val minSize: Int? = null,
//            val maxSize: Int? = null,
//
//            // type=time
//            val maximumOfHour: Int? = null,
//
//            // type=object
//            val properties: List<DeviceJson.Companion.Property>? = null,
//        )
//
//        @Serializable
//        data class Data(
//            val edt: String? = null,
//            val name: String? = null,
//            val descriptions: StringJaEn? = null,
//            val readOnly: Boolean? = null,
//        )
//    }
//}

fun main() {
    // resources/test.jsonを読み込む
//    val json = Eoj::class.java.getResource("/0x0290.json")?.readText()
//    if (json == null) {
//        println("json is null")
//        return
//    }
//    val obj = Json.decodeFromString<Eoj>(json)
//    println(json)
//    println(obj)

//     /mraData/devices内のすべてのファイルをチェック
//    val dir = File("C:\\myprogram\\Kotlin\\EchonetLite\\src\\main\\resources\\mraData\\devices")
//    // フォルダ内のファイル名一覧を表示
//    println(dir.listFiles()!!.joinToString("\n") { it.name })
//
//    for (file in dir.listFiles()!!) {
//        if (file.isFile) {
//            val content = file.readText()
//            val obj = Json.decodeFromString<DeviceJson.Companion.Device>(content)
//            println(obj)
//        }
//    }
//    println(DeviceJson.DataTypeList)

//    println(getDeviceFromEoj("0290"))
//    println(getDeviceFromEoj(listOf(0x02.toByte(), 0x90.toByte())))

    val file =
        File("C:\\myprogram\\Kotlin\\EchonetLite\\src\\main\\resources\\EchonetLiteData\\definitions.json")
    val content = file.readText()
//    val obj = Json.decodeFromString<DefinitionJson.Companion.Root>(content)
    val obj = Json.decodeFromString<DeviceJson.Companion.Definition>(content)
    println(obj)
}
