package com.chaitanya.theplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaitanya.theplaces.R
import com.chaitanya.theplaces.adapters.PlacesAdapter
import com.chaitanya.theplaces.database.DatabaseHandler
import com.chaitanya.theplaces.databinding.ActivityMainBinding
import com.chaitanya.theplaces.model.PlacesModel
import com.chaitanya.theplaces.utils.SwipeToDeleteCallback
import com.chaitanya.theplaces.utils.SwipeToEditCallback
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object{
        private const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        internal const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                getPlacesListFromLocalDB()
            }else{
                Log.e("Activity", "Cancelled or Back Pressed")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarMain)
        supportActionBar!!.title = getString(R.string.app_name)


        binding.fabAddPlace.setOnClickListener {
            val intent = Intent(this, AddPlaceActivity::class.java)
            startForResult.launch(intent)
        }

        getPlacesListFromLocalDB()
    }

    private fun getPlacesListFromLocalDB() {

        val dbHandler = DatabaseHandler(this)

        val getPlacesList = dbHandler.getPlaceList()

        if (getPlacesList.size > 0) {
            binding.rvPlacesList.visibility = View.VISIBLE
            binding.tvNoRecordsAvailable.visibility = View.GONE
            setupPlacesRecyclerView(getPlacesList)
        } else {
            binding.rvPlacesList.visibility = View.GONE
            binding.tvNoRecordsAvailable.visibility = View.VISIBLE
        }
    }

    private fun setupPlacesRecyclerView(PlacesList: ArrayList<PlacesModel>) {

        binding.rvPlacesList.layoutManager = LinearLayoutManager(this)
        binding.rvPlacesList.setHasFixedSize(true)

        val placesAdapter = PlacesAdapter(this, PlacesList)
        binding.rvPlacesList.adapter = placesAdapter

        placesAdapter.setOnClickListener(object :
                 PlacesAdapter.OnClickListener {
            override fun onClick(position: Int, model: PlacesModel) {
                val intent = Intent(this@MainActivity, PlaceDetailActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAILS, model)
                startActivity(intent)
            }
        })
        val editSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.rvPlacesList.adapter as PlacesAdapter
                val dbHandler = DatabaseHandler(this@MainActivity)

                val getPlacesList = dbHandler.getPlaceList()
                val intent = Intent(this@MainActivity,AddPlaceActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAILS,getPlacesList[viewHolder.adapterPosition] )

                startForResult.launch(intent)
                adapter.notifyItemChanged(viewHolder.adapterPosition)
            }

        }
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(binding.rvPlacesList)

        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val builder = AlertDialog.Builder(this@MainActivity)

                builder.setTitle("Delete Place")

                builder.setMessage("Are you sure you wants to delete Place.")
                builder.setIcon(android.R.drawable.ic_dialog_alert)


                builder.setPositiveButton("Yes") { dialogInterface, _ ->
                    val adapter = binding.rvPlacesList.adapter as PlacesAdapter
                    adapter.removeAt(viewHolder.adapterPosition)

                    getPlacesListFromLocalDB()
                        Toast.makeText(
                            applicationContext,
                            "Record deleted successfully.",
                            Toast.LENGTH_LONG
                        ).show()

                        dialogInterface.dismiss()
                    }
                builder.setNegativeButton("No") { dialogInterface, which ->
                    getPlacesListFromLocalDB()
                    dialogInterface.dismiss()
                }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.setCancelable(false)
                alertDialog.show()


            }
        }
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding.rvPlacesList)

    }
}