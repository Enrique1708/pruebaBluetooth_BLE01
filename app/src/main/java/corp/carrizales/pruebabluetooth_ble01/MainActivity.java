package corp.carrizales.pruebabluetooth_ble01;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final GattClient mGattClient = new GattClient();
    public static final String MAC_ADDRESS = "B8:27:EB:D2:FE:02";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGattClient.onCreate(this, MAC_ADDRESS, new GattClient.OnCounterReadListener() {
            @Override
            public void onCounterRead(final int value) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //mButton.setText(Integer.toString(value));
                    }
                });
            }

            @Override
            public void onConnected(final boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //mButton.setEnabled(success);
                        if (!success) {
                            Toast.makeText(MainActivity.this, "Connection error", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGattClient.onDestroy();
    }
}
