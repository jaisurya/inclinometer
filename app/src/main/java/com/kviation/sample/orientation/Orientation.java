
package com.kviation.sample.orientation;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.Math;

import static android.R.attr.x;
import static com.kviation.sample.orientation.R.id.acc_xaxis;


public class Orientation implements SensorEventListener {


  public interface Listener {
    void onOrientationChanged(float pitch, float roll);
  }

  private static final int SENSOR_DELAY_MICROS = 50 * 1000; // 50ms
    public static final double EPSILON = 0.0000001;

  private final WindowManager mWindowManager;

  private final SensorManager mSensorManager;
    public float[] linear_acceleration = new float[3];
    private float[] gravity = new float[3];

    //gyroscope
    private static final float NS2S = 1.0f / 1000000000.0f;
    public final float[] deltaRotationVector = new float[4];
    private float timestamp;



    @Nullable
  //private final Sensor mRotationSensor;
  private final Sensor mAccelerometerSensor;
  private final Sensor mGyroscopeSensor;

  private int mLastAccuracy;
  private Listener mListener;

  public Orientation(Activity activity) {
    mWindowManager = activity.getWindow().getWindowManager();
    mSensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);

    // Can be null if the sensor hardware is not available
    //mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);

  }

  public void startListening(Listener listener) {
//    if (mListener == listener) {
//      return;
//    }
//    mListener = listener;
//    if (mRotationSensor == null) {
//      LogUtil.w("Rotation vector sensor not available; will not provide orientation data.");
//      return;
//    }
//    mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS);
    if (mListener == listener) {
      return;
    }
    mListener = listener;
    if (mAccelerometerSensor == null) {
      LogUtil.w("Acceleration  sensor not available; will not provide orientation data.");
      return;
    }
    if (mGyroscopeSensor == null) {
      LogUtil.w("Gyroscope sensor not available; will not provide orientation data.");
      return;
    }
    mSensorManager.registerListener(this, mAccelerometerSensor, SENSOR_DELAY_MICROS);
    mSensorManager.registerListener(this, mGyroscopeSensor, SENSOR_DELAY_MICROS);

  }

  public void stopListening() {
    mSensorManager.unregisterListener(this);
    mListener = null;
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    if (mLastAccuracy != accuracy) {
      mLastAccuracy = accuracy;
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (mListener == null) {
      return;
    }
    if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
      return;
    }
      final int worldAxisForDeviceAxisX;
    final int worldAxisForDeviceAxisY;
      float[] rotationMatrix = new float[9];

    // Remap the axes as if the device screen was the instrument panel,
    // and adjust the rotation matrix for the device orientation.
    switch (mWindowManager.getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_0:
      default:
        worldAxisForDeviceAxisX = SensorManager.AXIS_X;
        worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
        break;
      case Surface.ROTATION_90:
        worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
        worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
        break;
      case Surface.ROTATION_180:
        worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
        worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
        break;
      case Surface.ROTATION_270:
        worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
        worldAxisForDeviceAxisY = SensorManager.AXIS_X;
        break;
    }
      float[] adjustedRotationMatrix = new float[9];
    SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
        worldAxisForDeviceAxisY, adjustedRotationMatrix);
    if (event.sensor == mAccelerometerSensor) {
        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

       //acc_x.setText(linear_acceleration[0]);


    }
      float Roll = (float)(Math.atan2(linear_acceleration[1], linear_acceleration[2]) * 180/Math.PI);
      float Pitch = (float)(Math.atan2(linear_acceleration[0], Math.sqrt(linear_acceleration[1]*linear_acceleration[1] + linear_acceleration[2]*linear_acceleration[2])) * 180/Math.PI);
      mListener.onOrientationChanged(Pitch, Roll);
      if (event.sensor == mGyroscopeSensor) {
         // updateOrientation(event.values);
          // This timestep's delta rotation to be multiplied by the current rotation
          // after computing it from the gyro sample data.
          if (timestamp != 0) {
              final float dT = (event.timestamp - timestamp) * NS2S;
              // Axis of the rotation sample, not normalized yet.
              float axisX = event.values[0];
              float axisY = event.values[1];
              float axisZ = event.values[2];

              // Calculate the angular speed of the sample
              float omegaMagnitude = (float)Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

              // Normalize the rotation vector if it's big enough to get the axis
              // (that is, EPSILON should represent your maximum allowable margin of error)
              if (omegaMagnitude > EPSILON) {
                  axisX /= omegaMagnitude;
                  axisY /= omegaMagnitude;
                  axisZ /= omegaMagnitude;
              }

              // Integrate around this axis with the angular speed by the timestep
              // in order to get a delta rotation from this sample over the timestep
              // We will convert this axis-angle representation of the delta rotation
              // into a quaternion before turning it into the rotation matrix.
              float thetaOverTwo = omegaMagnitude * dT / 2.0f;
              float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
              float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
              deltaRotationVector[0] = sinThetaOverTwo * axisX;
              deltaRotationVector[1] = sinThetaOverTwo * axisY;
              deltaRotationVector[2] = sinThetaOverTwo * axisZ;
              deltaRotationVector[3] = cosThetaOverTwo;
          }
          timestamp = event.timestamp;
          float[] deltaRotationMatrix = new float[9];
          SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
          // User code should concatenate the delta rotation we computed with the current rotation
          // in order to get the updated rotation.
          // rotationCurrent = rotationCurrent * deltaRotationMatrix;
      }

      }



//  @SuppressWarnings("SuspiciousNameCombination")
//  private void updateOrientation(float[] rotationVector) {
//    float[] rotationMatrix = new float[9];
//    SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
//
//    final int worldAxisForDeviceAxisX;
//    final int worldAxisForDeviceAxisY;
//
//    // Remap the axes as if the device screen was the instrument panel,
//    // and adjust the rotation matrix for the device orientation.
//    switch (mWindowManager.getDefaultDisplay().getRotation()) {
//      case Surface.ROTATION_0:
//      default:
//        worldAxisForDeviceAxisX = SensorManager.AXIS_X;
//        worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
//        break;
//      case Surface.ROTATION_90:
//        worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
//        worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
//        break;
//      case Surface.ROTATION_180:
//        worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
//        worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
//        break;
//      case Surface.ROTATION_270:
//        worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
//        worldAxisForDeviceAxisY = SensorManager.AXIS_X;
//        break;
//    }
//
//    float[] adjustedRotationMatrix = new float[9];
//    SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
//        worldAxisForDeviceAxisY, adjustedRotationMatrix);
//
//    // Transform rotation matrix into azimuth/pitch/roll
//    float[] orientation = new float[3];
//    SensorManager.getOrientation(adjustedRotationMatrix, orientation);
//
//    // Convert radians to degrees
//    float pitch = orientation[1] * -57;
//    float roll = orientation[2] * -57;
//
//    mListener.onOrientationChanged(pitch, roll);
//  }
}
