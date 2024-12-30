package com.alperenhakverdi.weatherapp

import MySingleton
import android.Manifest
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import im.delight.android.location.SimpleLocation
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var tvSehir: TextView
    private lateinit var tvAciklama: TextView
    private lateinit var imgHavaDurumu: ImageView
    private lateinit var tvSicaklik: TextView
    private lateinit var tvTarih: TextView
    private lateinit var tvDerece: TextView
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var spinner: Spinner

    private var location: SimpleLocation? = null
    private var latitude: String? = null
    private var longitude: String? = null

    // Konum izni istemek için launcher
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Konum izni verildiyse işlemi başlat
                getLocationData()
            } else {
                // Konum izni verilmediyse kullanıcıya açıklama yapılabilir
                Toast.makeText(this, "Konum izni verilmedi, lütfen izin verin.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Görsellerin Tanımlanması
        rootLayout = findViewById(R.id.rootLayout)
        tvSehir = findViewById(R.id.tvSehir)
        tvAciklama = findViewById(R.id.tvAciklama)
        tvDerece = findViewById(R.id.tvDerece)
        imgHavaDurumu = findViewById(R.id.imgHavaDurumu)
        tvSicaklik = findViewById(R.id.tvSicaklik)
        tvTarih = findViewById(R.id.tvTarih)
        spinner = findViewById(R.id.spinner)

        // Spinner Verileri ve Adapter
        val sehirler = resources.getStringArray(R.array.Sehirler)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sehirler)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this

        // Lokasyon Ayarları
        location = SimpleLocation(this)
        checkLocationPermission()  // Konum iznini kontrol et

        // Başlangıçta bulunduğunuz şehir verilerini getir
        if (latitude != null && longitude != null) {
            oankiSehriGetir()
        }

        // Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tvTarih)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        location?.beginUpdates()
    }

    override fun onPause() {
        super.onPause()
        location?.endUpdates()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Konum izni verilmemişse, kullanıcıdan izin istemek
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Konum izni zaten verilmişse, veri alınabilir
            getLocationData()
        }
    }

    private fun getLocationData() {
        // Konum verisi almak için gerekli kod buraya gelecek
        if (location!!.hasLocationEnabled()) {
            location?.setListener(object : SimpleLocation.Listener {
                override fun onPositionChanged() {
                    latitude = String.format("%.2f", location?.latitude)
                    longitude = String.format("%.2f", location?.longitude)
                    oankiSehriGetir() // Konum alındıktan sonra şehir verilerini çekiyoruz
                }
            })
        } else {
            SimpleLocation.openSettings(this)
        }
    }

    private fun oankiSehriGetir() {
        if (latitude != null && longitude != null) {
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=11cf9929ed74d5987b34c673f6313cb8&lang=tr&units=metric"
            havaDurumuVerileriniAl(url)
        }
    }

    private fun sehirVerileriniGetir(sehirAdi: String) {
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$sehirAdi&appid=11cf9929ed74d5987b34c673f6313cb8&lang=tr&units=metric"
        havaDurumuVerileriniAl(url)
    }

    private fun havaDurumuVerileriniAl(url: String) {
        val havaDurumuObjeRequest = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            try {
                val main = response.getJSONObject("main")
                val sicaklik = main.getDouble("temp").let { String.format("%.0f", it) }
                tvSicaklik.text = "$sicaklik°C"

                val sehirAdi = response.getString("name")
                tvSehir.text = sehirAdi

                val weather = response.getJSONArray("weather")
                val aciklama = weather.getJSONObject(0).getString("description").capitalize(Locale("tr", "TR"))
                tvAciklama.text = aciklama

                val icon = weather.getJSONObject(0).getString("icon")

                // Arka plan resmini değiştirme
                val backgroundRes = if (icon.last() == 'd') R.drawable.after_noon else R.drawable.night
                rootLayout.background = AppCompatResources.getDrawable(this, backgroundRes)

                // Metin rengini değiştirme
                val textColor = if (icon.last() == 'd') {
                    ContextCompat.getColor(this, R.color.dayTextColor) // Gündüz için metin rengi
                } else {
                    ContextCompat.getColor(this, R.color.nightTextColor) // Gece için metin rengi
                }

                // Status bar rengini değiştirme
                window.statusBarColor = if (icon.last() == 'd') {
                    ContextCompat.getColor(this, R.color.dayStatusBarColor) // Gündüz için status bar rengi
                } else {
                    ContextCompat.getColor(this, R.color.nightStatusBarColor) // Gece için status bar rengi
                }

                // Metin renklerini ayarlama
                tvSehir.setTextColor(textColor)
                tvAciklama.setTextColor(textColor)
                tvSicaklik.setTextColor(textColor)
                tvDerece.setTextColor(textColor)
                tvTarih.setTextColor(textColor)

                val resimDosyaAdi = resources.getIdentifier("icon_${icon.substring(0, icon.length - 1)}", "drawable", packageName)
                imgHavaDurumu.setImageResource(resimDosyaAdi)

                tvTarih.text = tarihYazdir()

            } catch (e: Exception) {
                Log.e("Hata", "Veri işlenirken hata: ${e.message}")
            }
        }, { error: VolleyError ->
            Log.e("Hata", "Hava durumu verisi alınamadı: $error")
        })

        MySingleton.getInstance(this)?.addToRequestQueue(havaDurumuObjeRequest)
    }

    private fun tarihYazdir(): String {
        val takvim = Calendar.getInstance()
        val formatlayici = SimpleDateFormat("EEEE, MMMM yyyy", Locale("tr", "TR"))
        return formatlayici.format(takvim.time)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val secilenSehir = parent?.getItemAtPosition(position).toString()
        if (secilenSehir != "Şehir Seçin") {
            sehirVerileriniGetir(secilenSehir)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
