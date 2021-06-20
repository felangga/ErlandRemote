package com.lampu;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.lampu.adapter.ItemMoveCallback;
import com.lampu.adapter.MemoryAdapter;
import com.lampu.lib.Storage;
import com.lampu.model.Memory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;


public class MainActivity extends AppCompatActivity {

    private Spinner spnAnimasi, spnAnimasi2;
    private Slider sldBrightness, sldAnimDelay, sldAnimDelay2;
    private Button btnAdd;
    private Button btnRemove;
    private Button btnUpdate;
    private RecyclerView lstDevices;
    private TextView txtBTStatus;
    private ProgressDialog loadingAdd;

    private MemoryAdapter memoryAdapter;
    private ArrayList<Memory> memories;

    private Boolean connected = false, loaded = false;

    private Bluetooth bluetooth;
    private final DeviceCallback deviceCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            txtBTStatus.setText("Connected to " + device.getName());
            Toast.makeText(MainActivity.this, "Connected to " + device.getName(), Toast.LENGTH_LONG).show();

            //save bt address
            SharedPreferences mSettings = MainActivity.this.getSharedPreferences("Settings", MODE_PRIVATE);
            SharedPreferences.Editor editor = mSettings.edit();

            editor.putString("address", device.getAddress());

            editor.commit();

            connected = true;
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            txtBTStatus.setText("Disconnected ");
            bluetooth.connectToDevice(device);
            connected = false;
        }

        @Override
        public void onMessage(byte[] msg) {
            String message = new String(msg);
            try {
                JSONObject parse = new JSONObject(message);

                if (parse.getString("cmd").equalsIgnoreCase("add")) {
                    Memory newMemory = new Memory();
                    newMemory.setName("Unammed");
                    newMemory.setLength(parse.getInt("size") > 100 ? 100 : parse.getInt("size"));
                    ArrayList tmp = new ArrayList();
                    int panjang = parse.getJSONArray("data").length();
                    if (panjang > 100) panjang = 100;
                    for (int i = 0; i < panjang; i++) {
                        tmp.add(parse.getJSONArray("data").getInt(i));
                    }
                    newMemory.setData(tmp);
//                    newMemory.setJsonData(message);

                    memories.add(newMemory);
                    Storage.saveMemory(MainActivity.this, new Gson().toJson(memories));
                    memoryAdapter.notifyDataSetChanged();

                    loadingAdd.dismiss();
                }
                loaded = true;
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Invalid response from device", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.d("ERR", e.getMessage());
                e.printStackTrace();
            }

        }

        @Override
        public void onError(int errorCode) {
        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            connected = false;
            txtBTStatus.setText("Failed to connect " + device.getName() + ", retrying in 3 seconds");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetooth.connectToDevice(device);
                }
            }, 3000);
        }
    };
    private BluetoothDevice device;

    void askPermissions() {
        Dexter.withActivity(this).withPermissions(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                            startActivityForResult(intent, 1);

                        } else {
                            Toast.makeText(MainActivity.this, "This app need some permission to run", Toast.LENGTH_SHORT).show();
                            askPermissions();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        bluetooth.setDeviceCallback(deviceCallback);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askPermissions();
            }
        });

        setupView();
        loadData();


    }

    private void loadData() {


        // load last connected address
        SharedPreferences mSettings = MainActivity.this.getSharedPreferences("Settings", MODE_PRIVATE);
        String cekAddress = mSettings.getString("address", null);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            txtBTStatus.setText("Device not supported Bluetooth");
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled :)
            txtBTStatus.setText("Bluetooth not enabled");
        } else {
            // Bluetooth is enabled
            if (cekAddress != null) {

                bluetooth.onStart();
                bluetooth.connectToAddress(cekAddress);
            }
        }


    }

    private void setupView() {
        btnAdd = (Button) findViewById(R.id.btnAdd);
        txtBTStatus = (TextView) findViewById(R.id.txtBTStatus);
        lstDevices = (RecyclerView) findViewById(R.id.lstDevices);
        loadingAdd = ProgressDialog.show(MainActivity.this, "", "Please point your remote to sensor and press a button", true);
        loadingAdd.dismiss();
        loadingAdd.setCancelable(true);
        loadingAdd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                doSend("cancelTambah");
            }
        });


        lstDevices.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        lstDevices.setLayoutManager(layoutManager);
        String datas = Storage.getMemory(MainActivity.this);
        if (datas != null)
            memories = new Gson().fromJson(datas, new TypeToken<ArrayList<Memory>>() {
            }.getType());
        else
            memories = new ArrayList<Memory>();


        memoryAdapter = new MemoryAdapter(this, memories) {

            @Override
            public void onRowMoved(int fromPosition, int toPosition) {
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(memories, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(memories, i, i - 1);
                    }
                }
                notifyItemMoved(fromPosition, toPosition);
                //memoryAdapter.notifyDataSetChanged();

                Storage.saveMemory(MainActivity.this, new Gson().toJson(memories));
            }

            @Override
            public void onDoneMoved(int fromPosition, int toPosition) {
             //   memoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onRowSelected(MemoryViewHolder myViewHolder) {

            }

            @Override
            public void onRowClear(MemoryViewHolder myViewHolder) {

            }

            @Override
            public void onSelectMemory(int position) {
                try {
                    Gson gson = new Gson();
                    Memory selected = memories.get(position);
                    JsonObject value = new JsonObject();
                    value.addProperty("cmd", "send");
                    value.addProperty("size", selected.getLength());

                    JsonElement element = gson.toJsonTree(selected.getData(), new TypeToken<List<Memory>>() {
                    }.getType());
                    value.add("data", element);

                    bluetooth.send(gson.toJson(value));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Failed to execute memory, please try again", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onMenuClick(Memory memory, View view) {

                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                //Inflating the Popup using xml file
                popup.getMenuInflater()
                        .inflate(R.menu.popup, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.mnu_delete: {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Delete")
                                        .setMessage("Do you want to delete this memory?")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                memories.remove(memory);
                                                Storage.saveMemory(MainActivity.this, new Gson().toJson(memories));
                                                memoryAdapter.notifyDataSetChanged();
                                                dialog.dismiss();
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, null).show();
                                break;
                            }
                            case R.id.mnu_edit: {

                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                                alertDialog.setTitle("Edit Name");

                                final EditText input = new EditText(MainActivity.this);
                                input.setText(memory.getName());

                                LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                                input.setLayoutParams(layoutParams);

                                linearLayout.addView(input);
                                linearLayout.setPadding(60, 0, 60, 0);
                                alertDialog.setView(linearLayout);

                                alertDialog.setPositiveButton("Save",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                memory.setName(input.getText().toString());
                                                memoryAdapter.notifyDataSetChanged();
                                                Storage.saveMemory(MainActivity.this, new Gson().toJson(memories));
                                                dialog.dismiss();
                                            }
                                        });

                                alertDialog.setNegativeButton("Cancel",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });

                                alertDialog.show();
                                break;
                            }
                        }

                        return true;
                    }
                });

                popup.show();
            }
        };

        ItemTouchHelper.Callback callback = new ItemMoveCallback(memoryAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(lstDevices);
        lstDevices.setAdapter(memoryAdapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingAdd.show();
                doSend("tambah");
            }
        });

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (requestCode == 1 && bluetooth != null) {
                device = data.getParcelableExtra("device");
                bluetooth.onStart();
                bluetooth.connectToDevice(device);
                txtBTStatus.setText("Connecting...");
            }
        } catch (Exception e) {

        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (bluetooth.isConnected()) {
            bluetooth.disconnect();
            bluetooth.onStop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (bluetooth != null && bluetooth.isConnected()) {
                bluetooth.disconnect();
                bluetooth.onStop();
            }
        } catch (Exception e) {

        }

    }

    public void onStart() {
        super.onStart();
    }


    public void doSend(String command) {
//        if (!connected || !loaded) {
//            Toast.makeText(MainActivity.this, "Please connect to device first", Toast.LENGTH_LONG).show();
//            return;
//        }

        Gson gson = new Gson();

        JsonObject value = new JsonObject();
        value.addProperty("cmd", command);
        bluetooth.send(gson.toJson(value));


    }
}