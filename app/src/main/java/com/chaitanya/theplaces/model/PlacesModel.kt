package com.chaitanya.theplaces.model

import android.os.Parcel
import android.os.Parcelable


data class PlacesModel(

val id: Int,
val title: String,
val image: String,
val description: String,
val date: String,
val location: String,
val latitude: Double,
val longitude: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        title = parcel.readString() ?: "",
        image = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        date = parcel.readString() ?: "",
        location = parcel.readString() ?: "",
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(image)
        parcel.writeString(description)
        parcel.writeString(date)
        parcel.writeString(location)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PlacesModel> {
        override fun createFromParcel(parcel: Parcel): PlacesModel {
            return PlacesModel(parcel)
        }

        override fun newArray(size: Int): Array<PlacesModel?> {
            return arrayOfNulls(size)
        }
    }
}

