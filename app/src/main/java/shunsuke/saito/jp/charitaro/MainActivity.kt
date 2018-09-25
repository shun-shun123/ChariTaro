package shunsuke.saito.jp.charitaro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var mSensorManager: SensorManager
    private var mLocationManager: LocationManager? = null
    private lateinit var mAccelerometer: Sensor
    private val SENSOR_TAG = "SENSOR_TAG"

    // シェイク検知に必要な定数・変数
    private val SHAKE_TIMEOUT = 300
    private val FORCE_THRESHOLD = 6
    private val SHAKE_COUNT = 2
    private var mLastTime: Long = 0
    private var mShakeCount = 0
    private var preAccel: Float = 1.0F

    private lateinit var realm: Realm
    private var mLastLocationData: LocationData? = null
    private var isSaved: Boolean = false

    private val REQUEST_LOCATION_PERMISSION_CODE: Int = 1

    private fun viewDebug(debug: Boolean = true) {
        textView.isEnabled = debug
        textView.visibility = if (debug) View.VISIBLE else View.INVISIBLE
        textView3.isEnabled = debug
        textView3.visibility = if (debug) View.VISIBLE else View.INVISIBLE
        latitude.isEnabled = debug
        latitude.visibility = if (debug) View.VISIBLE else View.INVISIBLE
        longitude.isEnabled = debug
        longitude.visibility = if (debug) View.VISIBLE else View.INVISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewDebug(false)

        // Realm初期設定
        Realm.init(this)

        delete_button.setOnClickListener {
            realm.executeTransaction {
                mLastLocationData?.deleteFromRealm()
                mLastLocationData = null
            }
            isSaved = false
        }

        // ジャイロセンサーマネージャの取得
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        // 位置情報に対するパーミッションをチェックしてから、可能ならLocationManagerを取得
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // 位置情報拒否られてる時、使わせてくださいとお願いしてみる
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION_CODE)
        }
        // システムにジャイロセンサーListenerを登録する
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        if (!isSaved) {
            imageView.setBackgroundColor(Color.BLACK)
            textView2.text = "端末を振って位置を登録！"
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // ちゃんと位置情報使わせてくれるならこっちもそれ使って頑張る
            mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        }
        realm = Realm.getDefaultInstance()
        mLastLocationData = realm.where(LocationData::class.java).equalTo("isSaved", true).findFirst()
        if (mLastLocationData != null) {
            isSaved = true
        }
        if (isSaved) {
            // すでに保存されている位置情報があればSearchActivityに遷移する
            val intent = Intent(this@MainActivity, SearchActivity::class.java)
            AlertDialog.Builder(this@MainActivity).apply {
                setMessage("自転車を探しますか？")
                setPositiveButton("探す") {dialogInterface, id ->
                    intent.putExtra("Latitude", mLastLocationData?.latitude)
                    intent.putExtra("Longitude", mLastLocationData?.longitude)
                    startActivity(intent)
                }
                setNegativeButton("登録し直す") {dialogInterface, id ->
                    // RealmのDBから以前のデータを削除する
                    realm.executeTransaction {
                        mLastLocationData?.deleteFromRealm()
                        mLastLocationData = null
                    }
                    isSaved = false
                }
            }.show()
        }
        latitude.text = mLastLocationData?.latitude.toString()
        longitude.text = mLastLocationData?.longitude.toString()
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
        mLocationManager?.removeUpdates(this)
        realm.close()
    }
    /*
    SensorEventListenerインタフェースの実装
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // センサーの精度が変更された時
        Log.d(SENSOR_TAG, "Accuracy is Changed")
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (sensorEvent == null) {
            return Unit
        }
        // Sensorのタイプが加速度センサなら以下の処理
        if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val values: FloatArray = sensorEvent.values.clone()
            val accel: Float = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
            val diff: Float = Math.abs(preAccel - accel)
            if (diff > FORCE_THRESHOLD) {
                val now = System.currentTimeMillis()
                if ((now - mLastTime) < SHAKE_TIMEOUT) {
                    // シェイク中
                    mShakeCount++
                    if (mShakeCount > SHAKE_COUNT) {
                        // シェイク検知
                        if (mLastLocationData == null) {
                            mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1.0F, this)
                        }
                        mShakeCount = 0
                    }
                } else {
                    mShakeCount = 0
                }
                mLastTime = now
            }
            preAccel = accel
        }
    }
    /*
    LocationListenerインタフェースの実装
     */
    override fun onProviderDisabled(p0: String?) {

    }

    override fun onProviderEnabled(p0: String?) {

    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

    }

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            return Unit
        }
        latitude.text = location.latitude.toString()
        longitude.text = location.longitude.toString()
        if (mLastLocationData == null && !isSaved) {
            realm.executeTransaction { realm ->
                val obj = realm.createObject(LocationData::class.java)
                obj.isSaved = true
                obj.latitude = location.latitude
                obj.longitude = location.longitude
            }
            isSaved = true
            mLastLocationData = realm.where(LocationData::class.java).equalTo("isSaved", true).findFirst()
        }
        if (isSaved) {
            imageView.setBackgroundColor(Color.BLUE)
            textView2.text = "保存完了！"
            Log.d("DEBUG", "COLOR is changed")
        }
    }
}
