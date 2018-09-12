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

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mLocationManager: LocationManager
    private lateinit var mAccelerometer: Sensor
    private val SENSOR_TAG = "SENSOR_TAG"

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
            Log.d(SENSOR_TAG, "ACCELEROMETER sensor is changed")
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, this)
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
