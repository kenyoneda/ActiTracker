package wisdm.cis.fordham.edu.actitracker;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;

import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.TextView;

import java.util.List;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class SensorSettings extends AppCompatActivity {
   TextView viewOne=null;
   private SensorManager mSensorManager;
   @Override
   
   protected void onCreate(Bundle savedInstanceState) 
   	{
    	  super.onCreate(savedInstanceState);
    	  setContentView(R.layout.Sesnor_Setting);
      
     	  viewOne = (TextView) findViewById(R.id.textView2);
      	  viewOne.setVisibility(View.GONE);
      
      	  mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
          List<Sensor> mList= mSensorManager.getSensorList(Sensor.TYPE_ALL);
      
      	  for (int i = 1; i < mList.size(); i++) 
      	      {
         	viewOne.setVisibility(View.VISIBLE);
        	viewOne.append("\n" + mList.get(i).getName() + "\n" + mList.get(i).getVendor() + "\n" + mList.get(i).getVersion());
              }
         }
         
   @Override
   public boolean onCreateOptionsMenu(Menu menu) 
        {
            getMenuInflater().inflate(R.menu.settings_menu, menu;
            return true;
        }
   
   @Override
   public boolean onOptionsItemSelected(MenuItem item)
       {
             int id = item.getItemId();

              if (id == R.id.action_settings) {
              return true;
              }
      return super.onOptionsItemSelected(item);
    }
}