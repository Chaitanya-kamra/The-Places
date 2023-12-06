package com.chaitanya.theplaces.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.io.IOException
import java.util.*


class GetAddressFromLatLng(
    private val context: Context,
    private val latitude: Double,
    private val longitude: Double
) {

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    fun getAddress(addressListener: AddressListener) {
        try {
            val addressList: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (addressList != null && addressList.isNotEmpty()) {
                val address: Address = addressList[0]
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(",")
                }
                sb.deleteCharAt(sb.length - 1)
                val resultString = sb.toString()
                addressListener.onAddressFound(resultString)
            } else {
                addressListener.onError()
            }
        } catch (e: IOException) {
            addressListener.onError()
            e.printStackTrace()
        }
    }

    interface AddressListener {
        fun onAddressFound(address: String?)
        fun onError()
    }
}

