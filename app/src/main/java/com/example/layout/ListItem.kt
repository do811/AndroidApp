package com.example.layout

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ListItem(
    val id: Long,
    var MainText: String,
    var SubText: String,
    var Switch: Boolean,
    var SwitchState: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString().toString()
    ) {
    }
    

    override fun describeContents(): Int {
        return 0
    }
}
