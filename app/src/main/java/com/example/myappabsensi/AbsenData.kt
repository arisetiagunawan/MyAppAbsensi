package com.example.myappabsensi

data class AbsenData(
    val id: String,
    val nama: String,
    val tanggal: String,
    val lokasi: String,
    val imageUrl: String,
    val keterangan: String
) {
    constructor(id: String, nama: String, tanggal: String, lokasi: String, imageUrl: String) :
            this(id, nama, tanggal, lokasi, imageUrl, "")
}
