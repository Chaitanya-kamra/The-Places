package com.chaitanya.theplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.chaitanya.theplaces.databinding.ActivityPlaceDetailBinding
import com.chaitanya.theplaces.model.PlacesModel
import java.lang.String
import java.util.Locale

class PlaceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var placeDetailModel: PlacesModel? = null

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            placeDetailModel =
                intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS) as PlacesModel?
        }
        if (placeDetailModel != null) {

            setSupportActionBar(binding.toolbarPlaceDetail)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = placeDetailModel.title

            binding.toolbarPlaceDetail.setNavigationOnClickListener {
                onBackPressed()
            }
            binding.ivPlaceImage.setImageURI(Uri.parse(placeDetailModel.image))
            binding.tvDescription.text =placeDetailModel.description
            binding.tvLocation.text = placeDetailModel.location
        }

        binding.btnViewOnMap.setOnClickListener {
            val intent = Intent(this@PlaceDetailActivity, MapsActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, placeDetailModel)
            startActivity(intent)
        }
        binding.GoToLocation.setOnClickListener {
            if (placeDetailModel != null) {
                val uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f", placeDetailModel.latitude, placeDetailModel.longitude,placeDetailModel.latitude, placeDetailModel.longitude)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                startActivity(intent)
            }
        }
        binding.shareLocation.setOnClickListener {
            if (placeDetailModel != null) {

                val uri = "https://maps.google.com/?q="+placeDetailModel.latitude+","+placeDetailModel.longitude
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, uri)
                startActivity(Intent.createChooser(sharingIntent, "Share in..."))
            }
        }

    }

 }

