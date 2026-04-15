package com.jvmapp.roadapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout

import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.libraries.places.api.model.Place

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.jvmapp.roadapp.data.adapter.OpinionAdapter
import com.jvmapp.roadapp.data.model.Opinion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class HomeAvtivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMyLocationClickListener {

    // ===============================
    // LOGIN
    private var usuarioActual: String = ""

    // ===============================
    // MAPA
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ===============================
    // RUTA
    private var currentPolyline: Polyline? = null
    private var inicio: String = ""
    private var fin: String = ""
    private var destinoSeleccionado: LatLng? = null

    // ===============================
    // cambiar modo
    private enum class ModoMapa {
        SELECCION_DESTINO,
        SELECCION_OPINION,
        NAVEGACION
    }

    private var modoMapa = ModoMapa.NAVEGACION


    // ===============================
    // OPINIONES
    private var puntoOpinion: LatLng? = null
    private val listaOpiniones = mutableListOf<Opinion>()
    private lateinit var opinionAdapter: OpinionAdapter
    private var marcadorDestino: com.google.android.gms.maps.model.Marker? = null
    private val marcadoresOpiniones = mutableListOf<com.google.android.gms.maps.model.Marker>()

    // ===============================
    // UI
    private lateinit var ivMenu: ImageView
    private lateinit var btnIniciar: Button
    private lateinit var etDestino: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvOpiniones: RecyclerView
    private lateinit var tvUsuario: TextView

    private lateinit var btnAgregarOpcion: Button
    private lateinit var cvOpinion: CardView
    private lateinit var btnGuardarOpinion: Button
    private lateinit var etOpinion: EditText
    private lateinit var ratingBar: RatingBar

    // ===============================
    // CONSTANTES
    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    private val AUTOCOMPLETE_REQUEST_CODE = 1

    // ===============================
    // CICLO DE VIDA
    // ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Inicializa Places (OBLIGATORIO para el buscador)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)




        initView()
        initMap()
        initRecycler()
        initListener()
        cargarOpiniones()
        usuarioActual = cargarUsuario()
        tvUsuario.text = usuarioActual
    }

    // ===============================
    // INIT
    // ===============================

    private fun initView() {
        btnIniciar = findViewById(R.id.btnIniciar)
        etDestino = findViewById(R.id.etDestino)
        ivMenu = findViewById(R.id.ivMenu)
        drawerLayout = findViewById(R.id.drawerLayout)
        rvOpiniones = findViewById(R.id.rvOpiniones)

        btnAgregarOpcion = findViewById(R.id.btnAgregarOpcion)
        cvOpinion = findViewById(R.id.cvOpinion)
        btnGuardarOpinion = findViewById(R.id.btnGuardarOpinion)
        etOpinion = findViewById(R.id.etOpinion)
        ratingBar = findViewById(R.id.ratingBar)
        tvUsuario = findViewById(R.id.tvUsuario)
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initRecycler() {
        opinionAdapter = OpinionAdapter(listaOpiniones) { opinion ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(opinion.ubicacion, 16f)
            )
        }

        rvOpiniones.layoutManager = LinearLayoutManager(this)
        rvOpiniones.adapter = opinionAdapter
    }

    private fun initListener() {

        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(Gravity.END)
        }

        etDestino.setOnClickListener {
            abrirBuscadorLugares()
        }

        btnIniciar.setOnClickListener {

            if (destinoSeleccionado == null) {
                Toast.makeText(this, "Elegí un destino primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarUbicacionActual {
                createRoute()
            }
        }

        btnAgregarOpcion.setOnClickListener {
            drawerLayout.closeDrawers()
            modoMapa = ModoMapa.SELECCION_OPINION
            Toast.makeText(this, "Elegí un punto para opinar", Toast.LENGTH_SHORT).show()
        }

        btnGuardarOpinion.setOnClickListener {
            elegirDestinoOpinion()
        }
    }

    // ===============================
    // GUARDADOS
    // ===============================

    private fun guardarUsuario(usuario: String) {
        val prefs = getSharedPreferences("roadapp_prefs", MODE_PRIVATE)
        prefs.edit().putString("usuario", usuario).apply()
    }

    private fun cargarUsuario(): String {
        val prefs = getSharedPreferences("roadapp_prefs", MODE_PRIVATE)
        return prefs.getString("usuario", "admin") ?: "admin"
    }

    private fun guardarOpiniones() {
        val prefs = getSharedPreferences("roadapp_prefs", MODE_PRIVATE)
        val json = Gson().toJson(listaOpiniones)
        prefs.edit().putString("opiniones", json).apply()
    }

    private fun cargarOpiniones() {
        val prefs = getSharedPreferences("roadapp_prefs", MODE_PRIVATE)
        val json = prefs.getString("opiniones", null)

        if (json != null) {
            val type = object : TypeToken<MutableList<Opinion>>() {}.type
            val opinionesGuardadas: MutableList<Opinion> =
                Gson().fromJson(json, type)

            listaOpiniones.clear()
            listaOpiniones.addAll(opinionesGuardadas)
            opinionAdapter.notifyDataSetChanged()
        }
    }

    private fun dibujarOpinionesEnMapa() {
        marcadoresOpiniones.forEach { it.remove() }
        marcadoresOpiniones.clear()

        for (opinion in listaOpiniones) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(opinion.ubicacion)
                    .title("Opinión de ${opinion.usuario}")
                    .snippet(opinion.comentario)
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE
                        )
                    )
            )

            marker?.let { marcadoresOpiniones.add(it) }
        }
    }




    // ===============================
    // MAPA
    // ===============================

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableLocation()

        map.setOnMapClickListener { latLng ->
            when (modoMapa) {
                ModoMapa.SELECCION_DESTINO -> seleccionarDestino(latLng)
                ModoMapa.SELECCION_OPINION -> seleccionarPuntoOpinion(latLng)
                else -> {}
            }
        }
        modoMapa = ModoMapa.SELECCION_DESTINO
        dibujarOpinionesEnMapa()
    }

    override fun onMyLocationClick(location: Location) {
        inicio = "${location.longitude},${location.latitude}"
    }

    private fun seleccionarDestino(latLng: LatLng) {
        destinoSeleccionado = latLng
        fin = "${latLng.longitude},${latLng.latitude}"

        // borrar SOLO el marcador del destino anterior
        marcadorDestino?.remove()

        marcadorDestino = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Destino")
        )

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 15f)
        )
    }



    // ===============================
    // RUTA
    // ===============================

    private fun guardarUbicacionActual(onSuccess: () -> Unit) {
        if (!isLocationPermissionGranted()) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                inicio = "${location.longitude},${location.latitude}"
                onSuccess()
            }
        }
    }

    private fun createRoute() {
        if (inicio.isEmpty() || fin.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            val response = getRetrofit()
                .create(ApiService::class.java)
                .getRoute(
                    "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImUxNTMyNmIyY2I1OTRlZGRiMWIzNDUzYmI2ZWZmOWEwIiwiaCI6Im11cm11cjY0In0=",
                    inicio,
                    fin
                )

            if (response.isSuccessful && response.body() != null) {
                drawRoute(response.body()!!.features[0].geometry.coordinates)
            }
        }
    }

    private fun drawRoute(coordinates: List<List<Double>>) {
        val polylinePoints = coordinates.map {
            LatLng(it[1], it[0])
        }

        runOnUiThread {
            currentPolyline?.remove()
            currentPolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(polylinePoints)
                    .width(16f)
                    .color(Color.BLUE)
            )
        }
    }

    // ===============================
    // OPINIONES
    // ===============================

    private fun elegirDestinoOpinion() {
        if (puntoOpinion == null || etOpinion.text.isBlank()) return

        val nuevaOpinion = Opinion(
            usuario = usuarioActual,
            estrellas = ratingBar.rating.toInt(),
            comentario = etOpinion.text.toString(),
            ubicacion = puntoOpinion!!
        )

        listaOpiniones.add(0, nuevaOpinion)
        opinionAdapter.notifyItemInserted(0)

        // 🔵 marcador de la opinión
        val marcadorOpinion = map.addMarker(
            MarkerOptions()
                .position(puntoOpinion!!)
                .title("Opinión de $usuarioActual")
                .snippet(nuevaOpinion.comentario)
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE
                    )
                )
        )

        marcadorOpinion?.let {
            marcadoresOpiniones.add(it)
        }

        // limpiar UI
        cvOpinion.visibility = View.GONE
        etOpinion.setText("")
        ratingBar.rating = 0f
        puntoOpinion = null

        // permitir elegir otro destino
        destinoSeleccionado = null
        modoMapa = ModoMapa.SELECCION_DESTINO

        guardarOpiniones()
    }


    private fun mostrarCardOpinion() {
        cvOpinion.visibility = View.VISIBLE
        cvOpinion.bringToFront()
        etOpinion.requestFocus()
    }

    // ===============================
    // DESTINOS
    // ===============================

    private fun abrirBuscadorLugares() {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG
        )

        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            fields
        ).build(this)

        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {

            // 1️⃣ Obtener el lugar seleccionado
            val place = Autocomplete.getPlaceFromIntent(data)

            // 2️⃣ Mostrar nombre en el TextView
            etDestino.text = place.name

            // 3️⃣ Obtener coordenadas
            val latLng = place.latLng
            if (latLng != null) {
                seleccionarDestino(latLng)
            }
        }
    }

    private fun seleccionarPuntoOpinion(latLng: LatLng) {
        puntoOpinion = latLng

        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Opinión acá")
        )

        mostrarCardOpinion()
    }



    // ===============================
    // PERMISOS
    // ===============================

    private fun isLocationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (!::map.isInitialized) return

        if (isLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
            map.setOnMyLocationClickListener(this)
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_LOCATION
        )
    }

    // ===============================
    // RETROFIT
    // ===============================

    private fun getRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}


