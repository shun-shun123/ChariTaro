package shunsuke.saito.jp.charitaro

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private val SENSOR_TAG = "SENSOR_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ジャイロセンサーマネージャの取得
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // センサーの精度が変更された時
        Log.d(SENSOR_TAG, "Accuracy is Changed")
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (sensorEvent?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            Log.d(SENSOR_TAG, "ACCELEROMETER sensor is changed")
        }
    }
}
