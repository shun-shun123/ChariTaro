package shunsuke.saito.jp.charitaro

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
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
    private val FORCE_THRESHOLD = 10
    private val SHAKE_COUNT = 3
    private var mLastTime: Long = 0
    private var mShakeCount = 0
    private var preAccel: Float = 1.0F

    private lateinit var realm: Realm
    private var mLastLocationData: LocationData? = null

    private val REQUEST_LOCATION_PERMISSION_CODE: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Realm初期設定
        Realm.init(this)

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
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // ちゃんと位置情報使わせてくれるならこっちもそれ使って頑張る
            mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        }
        realm = Realm.getDefaultInstance()
        mLastLocationData = realm.where(LocationData::class.java).equalTo("isSaved", true).findFirst()
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
        if (sensorEvent?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            var values: FloatArray = sensorEvent.values
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
        latitude.text = location?.latitude.toString()
        longitude.text = location?.longitude.toString()
        if (mLastLocationData == null) {
            realm.executeTransaction { realm ->
                val obj = realm.createObject(LocationData::class.java)
                obj.isSaved = true
                obj.latitude = location?.latitude!!
                obj.longitude = location.longitude
                Log.d(SENSOR_TAG, "Realm Data is Saved")
            }
            mLastLocationData = realm.where(LocationData::class.java).equalTo("isSaved", true).findFirst()
        }
    }
}
