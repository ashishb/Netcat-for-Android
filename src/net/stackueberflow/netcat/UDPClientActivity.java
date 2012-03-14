package net.stackueberflow.netcat;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class UDPClientActivity extends Activity {
	private static final String TAG = "NetCat UDP Client activity";
	private TextView tv;
	private EditText input;
	private String host;
	private int port;
	private byte[] fileBytes = null;
	private UDPClientTask networkTask;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connected);

		tv = (TextView)findViewById(R.id.output);
		input = (EditText)findViewById(R.id.input);

		Intent i = getIntent();
		host = i.getStringExtra("host");
		port = i.getIntExtra("port", 0);

		final Uri uri;
		if ( (uri = (Uri)i.getParcelableExtra(Intent.EXTRA_STREAM)) != null) {
			Log.i(TAG, "received file uri: "+uri.toString());
			try {
				fileBytes = IOHelpers.readFile(new File(IOHelpers.getRealPathFromURI(uri, this)));
				appendToOutput("\nSending File "+IOHelpers.getRealPathFromURI(uri, this)+", size: "+fileBytes.length+" bytes");
				input.setFocusable(false);
				input.setHint("Sending file "+IOHelpers.getRealPathFromURI(uri, this));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Log.i(TAG, "host: "+host+", port: "+port);


		Button button = (Button)findViewById(R.id.button1);        

		button.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				networkTask = new UDPClientTask();
				if (fileBytes != null) {
					Log.i(TAG, "Sending file");
					networkTask.execute(fileBytes);
				} else {
					Log.i(TAG, "Sending input");
					networkTask.execute(input.getText().toString().getBytes());
				}
			}
		});
	}

	@Override 
	public void onPause() {
		super.onPause();
		if (networkTask != null)
			networkTask.cancel(true);
	}

    
	private void appendToOutput(String str) {
		tv.append(str);
		tv.setMovementMethod(new ScrollingMovementMethod());
	}
	
	private void appendToOutput(byte[] data) {
		String str;
		try {
			str = new String(data, "UTF8");
			appendToOutput(str);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    
	public class UDPClientTask extends AsyncTask<byte[], byte[], Void> {
		private DatagramSocket socket;

		@Override
		protected Void doInBackground(byte[]... dataArray)  {			
			try {
				socket = new DatagramSocket();		
				Log.i(TAG, "doInBackground: Socket created, maximum Buffer size:" +socket.getSendBufferSize()+" bytes");

			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			byte[] data = dataArray[0];
			Log.i(TAG, "Data size: " +data.length);

			try {
				// Fragment the data into small chunks and send them sequentially:
				int num_packets = (int) Math.ceil(data.length/1024.0);
				Log.i(TAG, "Sending "+num_packets+" packets");
				byte[] tmp = new byte[1024];

				for(int i = 0; i < num_packets; i++) {
					// The last packet might be smaller
					if (i == num_packets -1)
						System.arraycopy(data, i*1024, tmp, 0, (data.length-i*1024)%1024);
					else 
						System.arraycopy(data, i*1024, tmp, 0, 1024);

					DatagramPacket packet = new DatagramPacket(tmp, tmp.length, InetAddress.getByName(host), port);
					socket.send(packet);
					Log.i(TAG, "Sent! "+i);
				}

				DatagramPacket packet = new DatagramPacket(tmp, tmp.length);
				socket.receive(packet);
				publishProgress(packet.getData());
				return null;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "Unknown Host");
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "IO Exception");
				e.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.i(TAG, "doInBackground: Finished");
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(byte[]... values) {
			byte[] data = values[0];
			String str;
			try {
				str = new String(data, "UTF8");
				Log.i(TAG,str);
				appendToOutput(str);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		@Override
		protected void onCancelled() {
			Log.i(TAG, "Cancelled.");
		
		}
		
		@Override
		protected void onPostExecute(Void v) {
			Log.i(TAG, "onPostExecute"); 
		}
	}
}
