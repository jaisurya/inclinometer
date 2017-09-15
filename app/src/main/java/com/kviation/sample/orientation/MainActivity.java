package com.kviation.sample.orientation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements Orientation.Listener {

  private Orientation mOrientation;
  private AttitudeIndicator mAttitudeIndicator;

  public TextView acc_x;
  public TextView acc_y;
  public TextView acc_z;
  public TextView gyro_x;
  public TextView gyro_y;
  public TextView gyro_z;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    acc_x=(TextView)findViewById(R.id.acc_xaxis);
    acc_y=(TextView)findViewById(R.id.acc_yaxis);
    acc_z=(TextView)findViewById(R.id.acc_zaxis);
    gyro_x=(TextView)findViewById(R.id.gyro_xaxis);
    gyro_y=(TextView)findViewById(R.id.gyro_yaxis);
    gyro_z=(TextView)findViewById(R.id.gyro_zaxis);







    //acc_x.setText();
    mOrientation = new Orientation(this);
    mAttitudeIndicator = (AttitudeIndicator) findViewById(R.id.attitude_indicator);
    acc_x.setText("100");
    acc_y.setText("100");
    acc_z.setText("100");
    gyro_x.setText("100");
    gyro_y.setText("100");
    gyro_z.setText("100");

  }

  @Override
  protected void onStart() {
    super.onStart();
    mOrientation.startListening(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    mOrientation.stopListening();
  }

  @Override
  public void onOrientationChanged(float pitch, float roll) {
    mAttitudeIndicator.setAttitude(pitch, roll);
  }
}
