package shunsuke.saito.jp.charitaro

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
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mLocationManager: LocationManager
    private lateinit var mAccelerometer: Sensor
    private val SENSOR_TAG = "SENSOR_TAG"

    // シェイク検知に必要な定数
    private val SHAKE_TIMEOUT = 300
    private val FORCE_THRESHOLD = 10
    private val SHAKE_COUNT = 3

    // シェイク検知に必要な変数
    private var mLastTime: Long = 0
    private var mShakeCount = 0
    private var preAccel: Float = 1.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ジャイロセンサーマネージャの取得
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        // LocationManagerを取得
        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        // システムにジャイロセンサーListenerを登録する
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
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
                Log.d(SENSOR_TAG, "mShakeCount: $mShakeCount")
                val now = System.currentTimeMillis()
                if ((now - mLastTime) < SHAKE_TIMEOUT) {
                    // シェイク中
                    mShakeCount++
                    if (mShakeCount > SHAKE_COUNT) {
                        // シェイク検知
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1.0F, this)
                        Log.d(SENSOR_TAG, "Shake!!")
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
    }
}
