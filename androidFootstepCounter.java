import java.lang.Math;
import java.util.Arrays;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class MainActivity extends Activity {	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
		
	public static class PlaceholderFragment extends Fragment {
		//Declaring objects to be displayed in app
		static LineGraphView graph;
		static TextView accelView;
		static Button resetButton;

		public PlaceholderFragment() {
		}
		
		private static TextView buildTextView (TextView tv, LinearLayout layout, View rootView){
			//Simply adds a TextView to the layout
			tv = new TextView(rootView.getContext());
			layout.addView(tv);		
			return tv;
		}
		
		private static AccelEventListener setupSensor (SensorManager manager, int type, TextView tv){
			//Get accelerometer from sensor manager, register it's listener, return the listener
			final Sensor sens = manager.getDefaultSensor(type);
			final AccelEventListener accListener = new AccelEventListener(tv);
			manager.registerListener(accListener, sens, SensorManager.SENSOR_DELAY_GAME);
			return accListener;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			//Initializing the root view for the app
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			
			
			//Initialize the reset button
			resetButton = new Button(rootView.getContext());
			resetButton.setText("RESET PEDOMETER");
			
			//Initializing the layout for the view objects
			LinearLayout mainLayout=(LinearLayout)rootView.findViewById(R.id.linLayout);
			
			//The accelerometer graph is the first object in the layout
			graph = new LineGraphView(rootView.getContext(), 100, Arrays.asList("a", "b", "c"));
			mainLayout.addView(graph);
			graph.setVisibility(View.VISIBLE);
			
			//Initialize the TextViews for the sensor readings
			accelView = buildTextView(accelView, mainLayout, rootView);
			
			//Add the button after the sensor TextViews
			mainLayout.addView(resetButton);
			
			//Initialize the sensor manager for handling all sensor inputs
			final SensorManager sensorManager = (SensorManager) rootView.getContext().getSystemService(SENSOR_SERVICE);	
			
			//Initialize sensor and listener, register both on the sensor manager, store the listener for use in reset button function
			final AccelEventListener accListener = setupSensor(sensorManager, Sensor.TYPE_LINEAR_ACCELERATION, accelView);
			
			//Create a function to do when a button click is received
			View.OnClickListener resetFunction = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//Reset all maximums, and the graph
					graph.purge();
					accListener.reset();
				}
			};		
			//Set the button to run the above function when pressed
			resetButton.setOnClickListener(resetFunction);
			
			
			
			return rootView;
		}
		
	}
	
}

class AccelEventListener implements SensorEventListener{
	//Text output
	TextView output;
	
	//Pedometer "working" variables
	private static boolean purpPeaked = false;
	private static boolean bluePeaked = false;
	private static int state = 0;
	private static int steps = 0;
	private static int purpWait = 0;
	private static int blueWait = 0;
	private static int resetCounter=0;
	private static int flagTimer = 0;
	private float[] smoothed = {0, 0, 0};
	private static float purpMax = 0;
	private static float blueMax = 0;
	private static float currentBlue = 0;
	private static float currentPurp = 0;
	
	//Pedometer "settings" variables
	private static float purpThreshold = 0.2f;
	private static float blueThreshold = 0.4f;
	private static float tooBig = 4.5f;
	private static int flagPenalCycles = 1000;
	private static int resetCycles = 75;
	private static int cap = 5;
	private static int peakWait = 28;

	
	
	public AccelEventListener(TextView outputView){
		//Given a TextView, set that TextView to be the same as the output TextView of this event listener
		output = outputView;
	}
	
	public void reset(){ 
		//Reset working variables so that pedometer works as if just started
		purpPeaked = false;
		bluePeaked = false;
		state = 0;
		steps = 0;
		purpWait = 0;
		blueWait = 0;
		resetCounter = 0;
		flagTimer = 0;
		smoothed[0]= 0;
		smoothed[1] = 0;
		smoothed[2] = 0;
		purpMax = 0;
		blueMax = 0;
		currentBlue = 0;
		currentPurp = 0;
	}
	
	public void toStateZero(){
		//Reset variables to state 0
		flagTimer = 0;
		resetCounter = 0;
		state = 0;
		purpPeaked = false;
		bluePeaked = false;
		purpMax = 0;
		blueMax = 0;
		
	}
	
	public void onSensorChanged(SensorEvent se){
		if (se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
			
			//Apply filtering to sensor data
			smoothed[0] += (se.values[0] - smoothed[0])/cap; 
			smoothed[1] += (se.values[1] - smoothed[1])/cap; 
			smoothed[2] += (se.values[2] - smoothed[2])/cap; 
			
			//If the sensor data is too big, there's shaking going on, so go to the shaking state
			if ((Math.abs(smoothed[0]) > tooBig || Math.abs(smoothed[1]) > tooBig || Math.abs(smoothed[2]) > tooBig)){
				state = -1;
			}
			
			//If shaking isn't happening:
			else{
				
				//These variables are easier to work with
				currentBlue = smoothed[2];
				currentPurp = smoothed[1];
				
				//If the current purple value is greater than the recorded max, make the current value the max
				//Otherwise, wait a bit to make sure that the purple value has maxed before assuming it has
				if (currentPurp > purpMax){
					purpMax = currentPurp;
				}
				else{
					purpWait ++;
					if(purpWait > peakWait){
						//Purple value must be past a certain threshold to be maxed
						if (purpMax>purpThreshold){
							purpPeaked = true;
							purpWait = 0;
						}
					}
				}
				
				
				//If the current blue value is greater than the recorded max, make the current value the max
				//Otherwise, wait a bit to make sure that the blue value has maxed before assuming it has
				if (currentBlue>blueMax){
					blueMax=currentBlue;
				}
				else{
					blueWait ++;
					if(blueWait > peakWait){
						//Blue value must be past a certain threshold to be maxed
						if (blueMax > blueThreshold){ 
							bluePeaked = true;
							blueWait = 0;
						}	
					}
				}
	
				//If purple has supposedly peaked, reset the state of the pedometer if no step occurs in some time
				//Incrementing this variable allows resetting later on if required
				if (purpPeaked){
					
					resetCounter ++;//if it is stuck on state 1 for too long, should revert back to state 0.
					
				}
			}
						
			switch(state){
			case -1:
				//If there is shaking happening, stop everything and wait it out
				blueMax = 0;
				purpMax = 0;
				bluePeaked = false;
				purpPeaked = false;
				while(state == -1){
					flagTimer ++;
					if (flagTimer > flagPenalCycles){
						toStateZero();
					}
				}
				break;
			case 0:
				//State 0, wait for purple to peak
				if (purpPeaked){
					if (purpMax > purpThreshold){
						//If the purple max value has peaked optimally, move onto state 1
						state = 1;
					}
					else{
						purpPeaked = false; //breaking?
						purpMax = 0;
						break;
					}
				}
				break;
			case 1:
				if (resetCounter > resetCycles){
					//If too much time has gone since purple has peaked, go back to state 0;
					toStateZero();
				}
				else if(bluePeaked){
					//If blue peaks optimally, increment step counter and start over;
					if (blueMax > blueThreshold){
						toStateZero();
						steps ++;
					}
				}
				break;
			default:
				break;
			}

			//Write the graph displays and textbox
			MainActivity.PlaceholderFragment.graph.addPoint(smoothed);
			
			String out = "\n\nSTEPS: " + steps;
			output.setText(out);	
		}
	}
	
	public void onAccuracyChanged(Sensor s, int i){
	}
}