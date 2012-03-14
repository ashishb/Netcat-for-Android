package net.stackueberflow.netcat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

// TODO: Handle errors, esp. not connected send in TCP
// TODO: Make output saveable to a file
// TODO: Output Text should be spannable
// TODO: Fix output being shifted up when input on screen keyboard appears 
// TODO: Put AsynTasks into one Activity
// TODO: enhance UI

public class NetCatActivity extends Activity {
	private static final String TAG = "NetCat activity";
	private EditText host, port;
	private Uri fileUri;
	private ImageView fileInfoIcon;
	private TextView fileInfoText;
	private int client_mode = 1;
	private int tcp_mode = 1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
              
        final RadioButton bClient = (RadioButton)findViewById(R.id.client);
        final RadioButton bServer = (RadioButton)findViewById(R.id.server);
        final RadioButton bTCP = (RadioButton)findViewById(R.id.tcp);
        final RadioButton bUDP = (RadioButton)findViewById(R.id.udp);
        
        fileInfoIcon = (ImageView)findViewById(R.id.fileIcon);
        fileInfoText = (TextView)findViewById(R.id.fileInfoText);
        fileInfoIcon.setVisibility(View.INVISIBLE);
        fileInfoText.setVisibility(View.INVISIBLE);
        
        host = (EditText)findViewById(R.id.host);
        port = (EditText)findViewById(R.id.port);
        
        bClient.setOnClickListener(new RadioButton.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		if(bClient.isChecked()) {
        			Log.i(TAG, "Client mode");
        			host.setFocusable(true);
        			client_mode = 1;
        		}
        	}
        });
        
        bServer.setOnClickListener(new RadioButton.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		if(bServer.isChecked()) {
        			Log.i(TAG, "Server mode");       
        			host.setFocusable(false);
        			client_mode = 0;
        		}
        	}
        });
        
        bTCP.setOnClickListener(new RadioButton.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		if(bTCP.isChecked()) {
        			Log.i(TAG, "TCP mode");  
        			tcp_mode = 1;
        		}
        	}
        });
        
        bUDP.setOnClickListener(new RadioButton.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		if(bUDP.isChecked()) {
        			Log.i(TAG, "UDP mode");      			
        			tcp_mode = 0;
        		}
        	}
        });
        
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
        	try {
				handleSendData(intent);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    private void handleSendData(Intent i) throws IOException {
//    	String sharedText = i.getStringExtra(Intent.EXTRA_TEXT);
//        if (sharedText != null) {
//        	Toast.makeText(getApplicationContext(), sharedText, Toast.LENGTH_SHORT).show();
//        	SendDataToNetwork(sharedText.getBytes());
//        }
    	fileUri = (Uri) i.getParcelableExtra(Intent.EXTRA_STREAM);
        if (fileUri != null) {
        	//Toast.makeText(getApplicationContext(), fileUri.toString(), Toast.LENGTH_SHORT).show();
        	fileInfoIcon.setVisibility(View.VISIBLE);
        	fileInfoText.setText("Sending file" +fileUri.toString());
        	fileInfoText.setVisibility(View.VISIBLE);
        	//File file = new File(uri.getPath());
        	//SendDataToNetwork(getBytesFromFile(file));
        }
    }    

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	MenuInflater  inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
            	if (( client_mode == 0 && !port.getText().toString().isEmpty()) || ( client_mode == 1 && !host.getText().toString().isEmpty())) {
            		Intent intent = new Intent();            		
            		if (client_mode == 1) {
            			if (tcp_mode == 1)
            				intent.setClass(getApplicationContext(), TCPClientActivity.class);
            			else 
            				intent.setClass(getApplicationContext(), UDPClientActivity.class);
            			intent.putExtra("host", host.getText().toString());
            		} else {
            			if (tcp_mode == 1)
            				intent.setClass(getApplicationContext(), TCPServerActivity.class);
            			else 
            				intent.setClass(getApplicationContext(), UDPServerActivity.class);
            		}
            	            			
            		intent.putExtra("port", Integer.parseInt(port.getText().toString()));
            		if (fileUri != null) {
            			Log.i(TAG, "Adding file uri to intent");
            			intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            		}
            		startActivity(intent);          

            		return true;
            	} else {
            		Toast toast = Toast.makeText(getApplicationContext(), "Host/port must be set", Toast.LENGTH_SHORT);
            		toast.show();
            		return false;
            	}
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
}