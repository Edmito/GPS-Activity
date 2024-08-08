package com.example.gps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var textViewLocation1: TextView
    private lateinit var textViewLocation2: TextView
    private lateinit var textViewDistance: TextView
    private lateinit var buttonGetLocation1: Button
    private lateinit var buttonGetLocation2: Button
    private lateinit var buttonShowOnMap1: Button
    private lateinit var buttonShowOnMap2: Button
    private lateinit var spinnerUnits: Spinner
    private var latitude1: Double? = null
    private var longitude1: Double? = null
    private var latitude2: Double? = null
    private var longitude2: Double? = null
    private var distanceInMeters: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        textViewLocation1 = findViewById(R.id.textViewLocation1)
        textViewLocation2 = findViewById(R.id.textViewLocation2)
        textViewDistance = findViewById(R.id.textViewDistance)
        buttonGetLocation1 = findViewById(R.id.buttonGetLocation1)
        buttonGetLocation2 = findViewById(R.id.buttonGetLocation2)
        buttonShowOnMap1 = findViewById(R.id.buttonShowOnMap1)
        buttonShowOnMap2 = findViewById(R.id.buttonShowOnMap2)
        spinnerUnits = findViewById(R.id.spinnerUnits)

        // Configurar Spinner com Adapter
        ArrayAdapter.createFromResource(
            this,
            R.array.units_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerUnits.adapter = adapter
        }

        // Listener para alterar a unidade de medida
        spinnerUnits.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateDistanceText()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Não é necessário fazer nada aqui
            }
        }

        buttonGetLocation1.setOnClickListener {
            if (checkAndRequestPermissions()) {
                getLastLocation(1)
            }
        }

        buttonGetLocation2.setOnClickListener {
            if (checkAndRequestPermissions()) {
                getLastLocation(2)
            }
        }

        buttonShowOnMap1.setOnClickListener {
            showOnMap(latitude1, longitude1)
        }

        buttonShowOnMap2.setOnClickListener {
            showOnMap(latitude2, longitude2)
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    private fun getLastLocation(locationNumber: Int) {
        if (checkAndRequestPermissions()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        if (locationNumber == 1) {
                            latitude1 = it.latitude
                            longitude1 = it.longitude
                            textViewLocation1.text = "Localização 1: $latitude1, $longitude1"
                            buttonShowOnMap1.visibility = Button.VISIBLE
                        } else {
                            latitude2 = 37.422009 // Simulação para Localização 2
                            longitude2 = -122.0855 // Simulação para Localização 2
                            textViewLocation2.text = "Localização 2: $latitude2, $longitude2"
                            buttonShowOnMap2.visibility = Button.VISIBLE
                        }
                        calculateDistance()
                    } ?: run {
                        if (locationNumber == 1) {
                            textViewLocation1.text = "Localização 1: Indisponível"
                            buttonShowOnMap1.visibility = Button.GONE
                        } else {
                            textViewLocation2.text = "Localização 2: Indisponível"
                            buttonShowOnMap2.visibility = Button.GONE
                        }
                    }
                }
        }
    }

    private fun showOnMap(latitude: Double?, longitude: Double?) {
        latitude?.let { lat ->
            longitude?.let { lon ->
                val geoUri = "geo:$lat,$lon?q=$lat,$lon(Localização)"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            }
        }
    }

    private fun calculateDistance() {
        if (latitude1 != null && longitude1 != null && latitude2 != null && longitude2 != null) {
            distanceInMeters = distanceInMeters(latitude1!!, longitude1!!, latitude2!!, longitude2!!)
            updateDistanceText()
        }
    }

    private fun updateDistanceText() {
        if (distanceInMeters != null) {
            val selectedUnit = spinnerUnits.selectedItem.toString()
            val distanceText = when (selectedUnit) {
                "Quilômetros" -> String.format("Distância: %.2f quilômetros", distanceInMeters!! / 1000)
                else -> String.format("Distância: %.2f metros", distanceInMeters)
            }
            textViewDistance.text = distanceText
        }
    }

    private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Raio da Terra em metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation(1)
            } else {
                textViewLocation1.text = "Permissão de localização negada"
                buttonShowOnMap1.visibility = Button.GONE
            }
        }
    }
}
