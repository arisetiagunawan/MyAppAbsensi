package com.example.myappabsensi.utils.ui_user

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.azhar.captureimage.utils.GPSTracker
import com.example.myappabsensi.BuildConfig
import com.example.myappabsensi.R
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class LaporanFragment : Fragment() {
    private lateinit var tvLocation: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvImageName: TextView
    private lateinit var tvDevice: TextView
    private lateinit var imagePreview: ImageView
    private lateinit var storageReference: StorageReference
    private lateinit var database: DatabaseReference

    private var REQ_CAMERA = 100

    private var gpsTracker: GPSTracker? = null
    private var imageFilePath: String? = null
    private var imageName: String? = null
    private var imageBytes: ByteArray? = null
    private var fileSize: Int = 0
    private var numberFormat: NumberFormat? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_laporan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnCapture = view.findViewById<Button>(R.id.btnCapture)
        imagePreview = view.findViewById(R.id.imagePreview)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvDateTime = view.findViewById(R.id.tvDateTime)
        tvImageName = view.findViewById(R.id.tvImageName)
        tvDevice = view.findViewById(R.id.tvDevice)

        database = FirebaseDatabase.getInstance().reference
        storageReference = FirebaseStorage.getInstance().reference

        gpsTracker = GPSTracker(requireContext())

        btnCapture.setOnClickListener {
            checkCameraPermission()
        }
        updateLocation()
    }

    private fun updateLocation() {
        if (isGPSEnabled()) {
            val latitude = gpsTracker?.getLatitude()
            val longitude = gpsTracker?.getLongitude()
            tvLocation.text = "Menentukan alamat..."
            fetchAddress(latitude, longitude)
        } else {
            tvLocation.text = "Lokasi: GPS tidak tersedia"
        }
    }

    private fun fetchAddress(latitude: Double?, longitude: Double?) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude ?: 0.0, longitude ?: 0.0, 1)

        if (addresses!!.isNotEmpty()) {
            val address = addresses[0]
            val completeAddress = address.getAddressLine(0)
            tvLocation.text = "Lokasi: $completeAddress"
        } else {
            tvLocation.text = "Lokasi tidak ditemukan"
        }
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun checkCameraPermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        takePhotoFromCamera()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Izin diperlukan untuk menggunakan kamera dan lokasi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
            .onSameThread()
            .check()
    }

    private fun takePhotoFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.let {
                val photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    BuildConfig.APPLICATION_ID + ".provider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQ_CAMERA)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir).apply {
            imageFilePath = absolutePath
            imageName = name
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQ_CAMERA) {
                val imageFile = File(imageFilePath)
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                imagePreview.setImageBitmap(bitmap)

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                imageBytes = baos.toByteArray()
                fileSize = imageBytes?.size ?: 0

                tvImageName.text = "Nama Gambar: $imageName"
                tvDevice.text = "Perangkat: ${Build.MODEL}"
                tvDateTime.text = "Waktu: ${getCurrentDateTime()}"

                // Save image to Firebase Storage and update the data in the Realtime Database
                uploadImageToFirebaseStorage(imageBytes, imageName)
            }
        }
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    private fun uploadImageToFirebaseStorage(imageBytes: ByteArray?, imageName: String?) {
        val imageRef = storageReference.child("images/$imageName")

        val uploadTask = imageRef.putBytes(imageBytes!!)
        uploadTask.addOnSuccessListener {
            Toast.makeText(requireContext(), "Gambar berhasil diunggah", Toast.LENGTH_SHORT)
                .show()

            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                saveDataToFirebaseDatabase(imageName, imageUrl)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal mengunggah gambar", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun saveDataToFirebaseDatabase(imageName: String?, imageUrl: String?) {
        val data = HashMap<String, Any>()
        data["image_name"] = imageName ?: ""
        data["image_url"] = imageUrl ?: ""
        data["location"] = tvLocation.text.toString()
        data["date_time"] = getCurrentDateTime()
        data["device"] = Build.MODEL
        data["file_size"] = getFileSizeInMB()

        database.child("userData").child("laporanHarian").push().setValue(data)
    }

    private fun getFileSizeInMB(): String {
        val decimalFormat = DecimalFormat("#.##")
        val fileSizeInKB = fileSize / 1024.0
        val fileSizeInMB = fileSizeInKB / 1024.0
        return "${decimalFormat.format(fileSizeInMB)} MB"
    }
}
