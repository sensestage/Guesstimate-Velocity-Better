package com.steim.nescivi.android.gvb;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
//import android.widget.Toast;

public class SensorsListActivity extends ListActivity {
	private static final int ACTIVITY_VIEW = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensors_list);
		
		fillData();
		
	}
	
	private void fillData() {
		String values[] = getResources().getStringArray(R.array.sensors_list_values);
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values)); 
	}
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
//        Toast.makeText(this, "Selected: " + position + " " + id, Toast.LENGTH_LONG).show();
     
        // Launch result list for selected sensor type
        Intent i = new Intent(this, ResultsListActivity.class);
        i.putExtra(ResultsListActivity.INTENT_SENSOR_SPECIFIER, position);
        startActivity(i);
    }


}
