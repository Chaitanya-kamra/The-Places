package com.chaitanya.theplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.chaitanya.theplaces.BuildConfig
import com.chaitanya.theplaces.R
import com.chaitanya.theplaces.database.DatabaseHandler
import com.chaitanya.theplaces.databinding.ActivityAddPlaceBinding
import com.chaitanya.theplaces.model.PlacesModel
import com.chaitanya.theplaces.utils.GetAddressFromLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class AddPlaceActivity : AppCompatActivity() , View.OnClickListener {

    companion object {
        private const val IMAGE_DIRECTORY = "ThePlacesImages"
    }

    private lateinit var binding: ActivityAddPlaceBinding

    private var cal = Calendar.getInstance()

    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    private var saveImageToInternalStorage: Uri? = null

    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private var mPlaceDetails: PlacesModel? = null

  private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var progressDialog: ProgressDialog? = null


    private val pickImageLauncher :ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {

            val imageUri = result.data?.data
            if (imageUri != null) {
                val imageBitmap: Bitmap? = try {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
                if (imageBitmap != null) {
                    binding.ivPlaceImage.setImageBitmap(imageBitmap)
                    saveImageToInternalStorage = saveImageToStorage(imageBitmap)
                    Log.e("Image ", "${saveImageToStorage(imageBitmap)}")
                }

            }

        }
    }
    private val captureImageLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap

            saveImageToInternalStorage = saveImageToStorage(imageBitmap)
            Log.e("Image ","${saveImageToStorage(imageBitmap)}")

            binding.ivPlaceImage.setImageBitmap(imageBitmap)
        }
    }

    val autocompleteContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val selectedPlace = Autocomplete.getPlaceFromIntent(data!!)
            binding.etLocation.setText(selectedPlace.address)
            mLatitude = selectedPlace.latLng!!.latitude
            mLongitude = selectedPlace.latLng!!.longitude
        }
    }


    private var requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach {
                val permission = it.key
                val isGranted = it.value
                if (isGranted){
                    when (permission) {
                        Manifest.permission.READ_MEDIA_IMAGES -> {

                            pickImage()

                        }
                        Manifest.permission.CAMERA -> {
                            captureImage()

                        }
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            setLocation()
                        }
                    }
                }
                else{
                    if (permission == Manifest.permission.READ_MEDIA_IMAGES){
                        Toast.makeText(this,"Provide Image Access",Toast.LENGTH_SHORT).show()
                    }
                    if (permission == Manifest.permission.ACCESS_FINE_LOCATION){
                        Toast.makeText(this,"Provide Fine location", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    @SuppressLint("MissingPermission")
    private fun setLocation() {
        showProgressDialog()
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            dismissProgressDialog()
            val mLastLocation: Location? = locationResult.lastLocation
            this@AddPlaceActivity.mLatitude = mLastLocation!!.latitude
            this@AddPlaceActivity.mLongitude = mLastLocation.longitude
            val addressTask =
                GetAddressFromLatLng(this@AddPlaceActivity, mLatitude, mLongitude)

            addressTask.getAddress(object : GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String?) {
                    binding.etLocation.setText(address)
                }

                override fun onError() {
                    Log.e("Error","Error")
                }
            })
        }
    }
    private fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog?.setMessage("Fetching location...")
            progressDialog?.setCancelable(false)
            progressDialog?.show()
        }
    }
    private fun dismissProgressDialog() {
        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
            progressDialog = null
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAddPlace.setNavigationOnClickListener { onBackPressed() }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (!Places.isInitialized()) {
            Places.initialize(this@AddPlaceActivity, BuildConfig.GOOGLE_MAPS_API_KEY)
        }


        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mPlaceDetails =
                intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS) as PlacesModel?
        }


        dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                updateDateInView()
            }
        updateDateInView()
        if (mPlaceDetails != null) {
            supportActionBar?.title = "Edit Place"

            binding.etTitle.setText(mPlaceDetails!!.title)
            binding.etDescription.setText(mPlaceDetails!!.description)
            binding.etDate.setText(mPlaceDetails!!.date)
            binding.etLocation.setText(mPlaceDetails!!.location)
            mLatitude = mPlaceDetails!!.latitude
            mLongitude = mPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mPlaceDetails!!.image)

            binding.ivPlaceImage.setImageURI(saveImageToInternalStorage)

            binding.btnSave.text = "UPDATE"
        }
        binding.etDate.setOnClickListener(this)
        binding.tvAddImage.setOnClickListener(this)
        binding.btnSave.setOnClickListener(this)
        binding.etLocation.setOnClickListener(this)
        binding.tvSelectCurrentLocation.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems =
                    arrayOf("Select photo from gallery", "Capture photo from camera")
                pictureDialog.setItems(
                    pictureDialogItems
                ) { dialog, which ->
                    when (which) {

                        0 -> openGallery()
                        1 -> openCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save -> {

                when {
                    binding.etTitle.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }
                    binding.etDescription.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT)
                            .show()
                    }
                    binding.etLocation.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT)
                            .show()
                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {

                        val placeModel = PlacesModel(
                            if (mPlaceDetails == null) 0 else mPlaceDetails!!.id,
                            binding.etTitle.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding.etDescription.text.toString(),
                            binding.etDate.text.toString(),
                            binding.etLocation.text.toString(),
                            mLatitude,
                            mLongitude
                        )

                        // Here we initialize the database handler class.
                        val dbHandler = DatabaseHandler(this)

                        if (mPlaceDetails == null) {
                            val addPlace = dbHandler.addPlace(placeModel)

                            if (addPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        } else {
                            val updatePlace = dbHandler.updatePlace(placeModel)

                            if (updatePlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }
            R.id.et_location -> {
                try {
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddPlaceActivity)
                    autocompleteContract.launch(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            R.id.tv_select_current_location ->{
                if (!isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        "Your location provider is turned off. Please turn it on.",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {
                    requestCurrentLocation()
                }
            }

        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }


    private fun openGallery() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_MEDIA_IMAGES)){
            showRationalDialogForPermissions()
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermission.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            }
            else{
                requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }


    private fun captureImage() {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        captureImageLauncher.launch(intent)
    }

    private fun pickImage() {
        val pickIntent = Intent(Intent.ACTION_PICK)
        pickIntent.type = "image/*"
        pickIntent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        pickImageLauncher.launch(Intent.createChooser(pickIntent, "Select Image"))
    }


    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding.etDate.setText(sdf.format(cal.time).toString())
    }


    private fun openCamera() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)){
            showRationalDialogForPermissions()
        } else {

            requestPermission.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }
    private  fun requestCurrentLocation(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)){
            showRationalDialogForPermissions()
        } else {
            requestPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    private fun saveImageToStorage(bitmap: Bitmap):Uri{
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val wrapper = ContextWrapper(applicationContext)
        val storageDir = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        val file = File(storageDir, "JPEG_${timeStamp}.jpg")
        try {

            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }


}