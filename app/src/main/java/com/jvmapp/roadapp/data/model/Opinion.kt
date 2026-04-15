package com.jvmapp.roadapp.data.model

import com.google.android.gms.maps.model.LatLng

data class Opinion (
    val usuario: String,
    val estrellas: Int,
    val comentario: String,
    val ubicacion: LatLng
)