package bombila.michael.driver;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//**************************************************************************************************
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
//**************************************************************************************************
    String registrationUrl = "http://185.25.119.3/BombilaDriver/registration.php";

    boolean WORKING = false;
    int DIRECTION_CODE = 1;
    int MY_PLACE_CODE  = 2;
    int REQUEST_ACCESS_FINE_LOCATION = 111;

    int order_id = 0;
    boolean clickOrder = false;

    long    driver_id;
    String  driver_phone;
    String  driver_car;
    String  driver_color;
    String  driver_number;

    boolean pilot = false;
    boolean on_time = false;
    boolean big_route = false;
    boolean my_place = false;       Place myPlace;
    int     min_cost = 35;
    double  radius = 0.7;
    double  dir_radius = 0.0;

    String  locality = "";
    double  currlatitude = 0.0;
    double  currlongitude = 0.0;

    ArrayList<Direction> myDirections = new ArrayList<>();

    ImageView ivMenu;
    TextView  tvLocality;
    TextView  tvAddress;
    TextView  tvOrdersInfo;

    ListView lv;
    ArrayList<Map<String, String>> dataLv = new ArrayList<>();
    SimpleAdapter sAdapter = null;

    Daemon daemon = new Daemon();
    TextToSpeech mTTS;

    private LocationManager locationManager;
//**************************************************************************************************
    private LocationListener locationListener = new LocationListener() {
//**************************************************************************************************
        @Override
        public void onLocationChanged(Location location) {
            if (location==null || my_place) return;

            currlatitude = location.getLatitude();
            currlongitude = location.getLongitude();

            String addr = getAddress();
            if(addr != null) {
                tvAddress.setText(addr);
            }
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                if(addr == null) {
                    String gps = "GPS: " + String.valueOf(currlatitude) + ", "
                            + String.valueOf(currlongitude);
                    tvAddress.setText(gps);
                }
            }
            if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
                if (addr == null) {
                    String net = "NET: " + String.valueOf(currlatitude) + ", "
                            + String.valueOf(currlongitude);
                    tvAddress.setText(net);
                }
            }
        }
        @Override
        public void onProviderDisabled(String provider) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };
//--------------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//--------------------------------------------------------------------------------------------------
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivMenu       = findViewById(R.id.ivMenu);       ivMenu.setOnClickListener(this);
        tvLocality   = findViewById(R.id.tvLocality);
        tvAddress    = findViewById(R.id.tvAddress);
        tvOrdersInfo = findViewById(R.id.tvOrdersInfo);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean hasPermissionLocation = (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermissionLocation) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }

        String[] from = {"address", "distance", "time", "fot", "price", "route"};
        int[] to = {R.id.tvOrderAddress, R.id.tvOrderDistance, R.id.tvOrderTime,
                R.id.tvOrderBochka, R.id.tvOrderPrice, R.id.tvOrderRoute};
        lv = findViewById(R.id.lv);
        sAdapter = new SimpleAdapter(this, dataLv, R.layout.item, from, to);
        lv.setAdapter(sAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, String> m = dataLv.get(position);
                order_id = Integer.parseInt(m.get("order_id"));
                clickOrder = true;
            }
        });

        getPreferences();
        if (driver_id == 0L) {
            dialogRegistration();
            return;
        }
        if (my_place) {
            try {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                startActivityForResult(builder.build(MainActivity.this), MY_PLACE_CODE);
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        }

        mTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(new Locale("ru"));
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MainActivity.this, "Этот язык не поддерживается",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

//        daemon.execute();

    }
//--------------------------------------------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult
                    (int requestCode, String[] permissions, int[] grantResults) {
//--------------------------------------------------------------------------------------------------
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
                finish();
                startActivity(getIntent());
            } else {
                Toast.makeText(this,
                        "В настройках этого приложения включите разрешение для " +
                                "определения вашего местоположения",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
//--------------------------------------------------------------------------------------------------
    @Override
    protected void onResume() {
//--------------------------------------------------------------------------------------------------
        super.onResume();
        locationListenerON();
    }
//--------------------------------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
//--------------------------------------------------------------------------------------------------
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
            mTTS = null;
        }
        daemon.cancel(false);
        super.onDestroy();
    }
//--------------------------------------------------------------------------------------------------
    @Override
    protected void onPause() {
//--------------------------------------------------------------------------------------------------
        super.onPause();
        locationListenerOFF();
    }
//-------------------------------------------------------------------------------------
    @Override
    public void onClick(View v) {
//-------------------------------------------------------------------------------------
        switch (v.getId()) {
            case R.id.ivMenu:
//daemon.execute();
                dialogMenu();
                break;
            default:
                break;
        }
    }
//--------------------------------------------------------------------------------------------------
    void locationListenerON(){
//--------------------------------------------------------------------------------------------------
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000 * 10, 10, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000 * 10, 10, locationListener);
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }
//--------------------------------------------------------------------------------------------------
    void locationListenerOFF(){
//--------------------------------------------------------------------------------------------------
        try {
            locationManager.removeUpdates(locationListener);
        } catch(SecurityException e) { e.printStackTrace(); }
    }
//--------------------------------------------------------------------------------------------------
    LatLng getLatLng(String addr){
//--------------------------------------------------------------------------------------------------
        Geocoder coder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        LatLng latlng = null;
        try {
            addresses = coder.getFromLocationName(addr, 1);
            if (addresses==null || addresses.size()==0) { return null; }
            latlng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return latlng;
    }
//--------------------------------------------------------------------------------------------------
    String getAddress() {
//--------------------------------------------------------------------------------------------------
        if(currlatitude==0.0 || currlongitude==0.0) { return null; }
        Geocoder coder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        String address = null;
        try {
            addresses = coder.getFromLocation(currlatitude, currlongitude, 1);
            if (addresses==null || addresses.size()==0) { return null; }
            address = addresses.get(0).getAddressLine(0);
            locality = addresses.get(0).getLocality();
            tvLocality.setText(locality);
            String[] arr = address.split(", ");
            address = arr[0] + ", " + arr[1];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }
//-------------------------------------------------------------------------------------
    void dialogMenu() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.menu, null);
        final LinearLayout llDriver = view.findViewById(R.id.llDriver);
        final LinearLayout llSettings = view.findViewById(R.id.llSettings);
        final LinearLayout llDirections = view.findViewById(R.id.llDirections);
        final LinearLayout llToWork  = view.findViewById(R.id.llToWork);
        final LinearLayout llExit = view.findViewById(R.id.llExit);
        if (WORKING) llToWork.setVisibility(View.GONE);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Меню")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog = add.create();
        dialog.show();

        llDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogDriverInfo();
            }
        });
        llSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogSettings();
            }
        });
        llDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogDirAction();
            }
        });
        llToWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                WORKING = true;
                daemon.execute();
            }
        });
        llExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogExit();
            }
        });

    }
//-------------------------------------------------------------------------------------
    void dialogDriverInfo() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.driver, null);
        final LinearLayout llDriverPhone = view.findViewById(R.id.llDriverPhone);
        final LinearLayout llDriverCar = view.findViewById(R.id.llDriverCar);
        final LinearLayout llDriverColor = view.findViewById(R.id.llDriverColor);
        final LinearLayout llDriverNumber = view.findViewById(R.id.llDriverNumber);

        final TextView tvDriverPhone = view.findViewById(R.id.tvDriverPhone);
        final TextView tvDriverCar = view.findViewById(R.id.tvDriverCar);
        final TextView tvDriverColor = view.findViewById(R.id.tvDriverColor);
        final TextView tvDriverNumber = view.findViewById(R.id.tvDriverNumber);

        tvDriverPhone.setText(driver_phone);
        tvDriverCar.setText(driver_car);
        tvDriverColor.setText(driver_color);
        tvDriverNumber.setText(driver_number);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Данные водителя")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog = add.create();
        dialog.show();

        llDriverPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogPhone();
            }
        });
        llDriverCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogCar();
            }
        });
        llDriverColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogColor();
            }
        });
        llDriverNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogNumber();
            }
        });

    }
//-------------------------------------------------------------------------------------
    void dialogSettings() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.settings, null);
        final LinearLayout llCost = view.findViewById(R.id.llCost);
        final LinearLayout llRadius = view.findViewById(R.id.llRadius);
        final TextView tvCost = view.findViewById(R.id.tvCost);
        final TextView tvRadius = view.findViewById(R.id.tvRadius);
        final Switch switchOnTime = view.findViewById(R.id.switchOnTime);
        final Switch switchRoute = view.findViewById(R.id.switchRoute);
        final Switch switchMyPlace = view.findViewById(R.id.switchMyPlace);
        switchOnTime.setChecked(on_time);
        switchRoute.setChecked(big_route);
        switchMyPlace.setChecked(my_place);
        String s = "Мин. сумма: " + String.valueOf(min_cost) + "грн.";
        tvCost.setText(s);
        String str = String.format("%.1f", radius);
        s = "Радиус захвата: " + str + "км";
        tvRadius.setText(s);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Настройки")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog = add.create();
        dialog.show();

        switchOnTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) on_time = true;
                else on_time = false;
            }
        });
        switchRoute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) big_route = true;
                else big_route = false;
            }
        });
        switchMyPlace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    try {
//  locationListenerOFF() in onActivityResult()
                        startActivityForResult(builder.build(MainActivity.this), MY_PLACE_CODE);
                    } catch (GooglePlayServicesRepairableException e) {
                        e.printStackTrace();
                    } catch (GooglePlayServicesNotAvailableException e) {
                        e.printStackTrace();
                    }
                } else {
                    locationListenerON();
                    my_place = false;
                }
            }
        });
        llCost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogMinCost();
            }
        });
        llRadius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                dialogRadius();
            }
        });

    }
//-------------------------------------------------------------------------------------
    void dialogDirAction() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.dir_action, null);
        final LinearLayout llSelect = view.findViewById(R.id.llSelect);
        final LinearLayout llAdd = view.findViewById(R.id.llAdd);
        final LinearLayout llDelete = view.findViewById(R.id.llDelete);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Что сделать?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog = add.create();
        dialog.show();

        llSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                dialog.cancel();
                dialogSelectDir();
            }
        });
        llAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                dialog.cancel();
                dialogDirRadius();
            }
        });
        llDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                dialog.cancel();
                dialogDeleteDir();
            }
        });
    }
//-------------------------------------------------------------------------------------
    void dialogDeleteDir() {
//-------------------------------------------------------------------------------------
        final int SIZE = myDirections.size();
        final boolean[] deletedItems = new boolean[SIZE];
        final String[]  addresses    = new String[SIZE];
        for (int i=0; i<SIZE; i++) {
            String r = String.format("%.1f", myDirections.get(i).radius);
            String[] arr = myDirections.get(i).address.split(", ");
            String addr  = arr[0] + ", " + arr[1] + " [" + r + "км]";
            addresses[i] = addr;
            deletedItems[i] = false;
        }

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add     .setCancelable(false)
                .setTitle("Мои направления")
                .setMultiChoiceItems(addresses, deletedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                deletedItems[which] = isChecked;
                            }
                        })
                .setNeutralButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        for (int i = 0; i < SIZE; i++) {
                            deletedItems[i] = false;;
                        }
                        dialogDeleteDir();
                    }
                })
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        int del = 0;
                        for (int i = 0; i < SIZE; i++) {
                            if (deletedItems[i]) {
                                myDirections.remove(del);
                                del = i - 1;
                            }
                            del++;
                        }
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogSelectDir() {
//-------------------------------------------------------------------------------------
        final int SIZE = myDirections.size();
        final boolean[] checkedItems = new boolean[SIZE];
        final String[]  addresses    = new String[SIZE];
        for (int i=0; i<SIZE; i++) {
            String r = String.format("%.1f", myDirections.get(i).radius);
            String[] arr = myDirections.get(i).address.split(", ");
            String addr  = arr[0] + ", " + arr[1] + " [" + r + "км]";
            addresses[i] = addr;
            checkedItems[i] = myDirections.get(i).checked;
        }

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add     .setCancelable(false)
                .setTitle("Мои направления")
                .setMultiChoiceItems(addresses, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                checkedItems[which] = isChecked;
                            }
                        })
                .setNeutralButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        for (int i = 0; i < SIZE; i++) {
                            myDirections.get(i).setChecked(false);
                        }
                        dialogSelectDir();
                    }
                })
                .setPositiveButton("Выбрать", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        for (int i = 0; i < SIZE; i++) {
                            myDirections.get(i).setChecked(checkedItems[i]);
                        }
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogExit() {
//-------------------------------------------------------------------------------------
        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setCancelable(false)
            .setMessage("Вы действительно хотите выйти из приложения?")
            .setTitle("Выход")
            .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    putPreferences();
                    finish();
                }
            });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogRadius() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.et, null);
        final EditText et = (EditText) view.findViewById(R.id.et);

        if (radius == 0) et.setText("");
        else et.setText(String.format("%.1f", radius));

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Радиус захвата")
                .setNegativeButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        radius = 0;
                        dialogRadius();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String s = et.getText().toString().replace(",", ".");
                        radius = Double.parseDouble(s);
                        dialog.cancel();
                        dialogSettings();
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogDirRadius() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.et, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        et.setText("");

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Радиус направления")
                .setNegativeButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        dialogDirRadius();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        try {
                            String s = et.getText().toString().replace(",", ".");
                            dir_radius = Double.parseDouble(s);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
      //                      dialogDirAction();
                            return;
                        }
                        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                        try {
                            startActivityForResult(builder.build(MainActivity.this), DIRECTION_CODE);
                        } catch (GooglePlayServicesRepairableException e) {
                            e.printStackTrace();
                        } catch (GooglePlayServicesNotAvailableException e) {
                            e.printStackTrace();
                        }
      //                  dialog.cancel();
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogMinCost() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.et, null);
        final EditText et = (EditText) view.findViewById(R.id.et);

        if (min_cost == 0) et.setText("");
        else et.setText(String.valueOf(min_cost));

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Минимальная сумма")
                .setNegativeButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        min_cost = 0;
//                        et.setText("");
                        dialogMinCost();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        min_cost = Integer.parseInt(et.getText().toString());
                        dialog.cancel();
                        dialogSettings();
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogNumber() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.et, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        et.setText(driver_number);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Введите номер авто")
                .setNegativeButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        et.setText(driver_number = "");
                        dialogNumber();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        driver_number = et.getText().toString();
                        dialog.cancel();
                        dialogDriverInfo();
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogColor() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.et, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        et.setText(driver_color);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Введите цвет авто")
                .setNegativeButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        et.setText(driver_color = "");
                        dialogColor();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        driver_color = et.getText().toString();
                        dialog.cancel();
                        dialogDriverInfo();
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogCar() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.et, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        et.setText(driver_car);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Введите марку авто")
                .setNegativeButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        et.setText(driver_car = "");
                        dialogCar();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        driver_car = et.getText().toString();
                        dialog.cancel();
                        dialogDriverInfo();
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogPhone() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.et, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        et.setText(driver_phone);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Введите номер телефона")
                .setNegativeButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        et.setText(driver_phone = "");
                        dialogPhone();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        driver_phone = et.getText().toString()
                                .replace("+","")
                                .replace("-","")
                                .replace("(","")
                                .replace(")","")
                                .replace(" ","");
                        driver_phone = "+" + driver_phone;
                        if (driver_phone.length() < 12) {
                            dialogPhone();
                            return;
                        }
                        getSharedPreferences("driver_pref", MODE_PRIVATE)
                                .edit()
                                .putString("driver_phone", driver_phone)
                                .apply();
                        dialog.cancel();
                        dialogDriverInfo();
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogRegistration() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.registration, null);
        final EditText etPhone = (EditText) view.findViewById(R.id.etPhone);

        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setCancelable(false)
                .setTitle("Регистрация")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        driver_phone = etPhone.getText().toString()
                                .replace("+","")
                                .replace("-","")
                                .replace("(","")
                                .replace(")","")
                                .replace(" ","");
                        driver_phone = "+" + driver_phone;

                        if (driver_phone.length() < 12) {
                            dialogRegistration();
                            return;
                        }

                        try {
                            String s = new HttpPost()
                                    .execute(registrationUrl, driver_phone).get();
                            driver_id = new JSONObject(s).getLong("id");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
        dialog = builder.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void getPreferences() {
//-------------------------------------------------------------------------------------
        SharedPreferences sp = getSharedPreferences("driver_pref", MODE_PRIVATE);
        driver_id     = sp.getLong   ("driver_id", 0L);
        driver_phone  = sp.getString ("driver_phone", "");
        driver_car    = sp.getString ("driver_car", "");
        driver_color  = sp.getString ("driver_color", "");
        driver_number = sp.getString ("driver_number", "");
        on_time       = sp.getBoolean("on_time", false);
        big_route     = sp.getBoolean("big_route", false);
        my_place      = sp.getBoolean("my_place", false);
        min_cost      = sp.getInt    ("min_cost", 35);
        radius        = sp.getFloat  ("radius", (float) 0.7);
        myDirections  = directionsFromString(sp.getString("my_directions", "[]"));
    }
//-------------------------------------------------------------------------------------
    void putPreferences() {
//-------------------------------------------------------------------------------------
        getSharedPreferences("driver_pref", MODE_PRIVATE)
                .edit()
                .putLong("driver_id", driver_id)
                .putString("driver_phone", driver_phone)
                .putString("driver_car", driver_car)
                .putString("driver_color", driver_color)
                .putString("driver_number", driver_number)
                .putBoolean("on_time", on_time)
                .putBoolean("big_route", big_route)
                .putBoolean("my_place", my_place)
                .putInt("min_cost", min_cost)
                .putFloat("radius", (float) radius)
                .putString("my_directions", directionsToString())
                .apply();
    }
//-------------------------------------------------------------------------------------
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//-------------------------------------------------------------------------------------
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            dialogExit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
//-------------------------------------------------------------------------------------
    void sleep(int seconds) {
//-------------------------------------------------------------------------------------
        try { TimeUnit.SECONDS.sleep(seconds); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
//-------------------------------------------------------------------------------------
    String directionsToString() {
//-------------------------------------------------------------------------------------
        JSONArray  jarr = new JSONArray();
        try {
            for (Direction direction : myDirections) {
                JSONObject jdir = new JSONObject();
                jdir.put("address",  direction.address)
                    .put("radius",   direction.radius)
                    .put("latitude", direction.latlng.latitude)
                    .put("longitude",direction.latlng.longitude)
                    .put("locality", direction.locality)
                    .put("checked",  direction.checked);
                jarr.put(jdir);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jarr.toString();
    }
//-------------------------------------------------------------------------------------
    ArrayList<Direction> directionsFromString(String str) {
//-------------------------------------------------------------------------------------
        JSONArray jarr = null;
        ArrayList<Direction> dirs = new ArrayList<>();
        try {
            jarr = new JSONArray(str);
            for (int i=0; i<jarr.length(); i++) {
                JSONObject jobj = jarr.getJSONObject(i);
                String address = jobj.getString("address");
                double radius  = jobj.getDouble("radius");
                LatLng latLng  = new LatLng(jobj.getDouble("latitude"),
                                            jobj.getDouble("longitude"));
                String locality = jobj.getString("locality");
                boolean checked = jobj.getBoolean("checked");
                Direction dir = new Direction(address, radius, latLng, locality, checked);
                dirs.add(dir);
            }
        } catch (JSONException e) { e.printStackTrace(); }

        return dirs;
    }
//-------------------------------------------------------------------------------------
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//-------------------------------------------------------------------------------------
        if (resultCode == RESULT_CANCELED) {
            my_place = false;
            return;
        }
        if (requestCode == DIRECTION_CODE) {
            if (resultCode == RESULT_OK) {
                Place pp = PlacePicker.getPlace(this, data);
                String address = pp.getAddress().toString();
                LatLng latLng = new LatLng(pp.getLatLng().latitude, pp.getLatLng().longitude);
                String locality;
                try {
                    List<Address> addresses = new Geocoder(this, Locale.getDefault())
                            .getFromLocation(latLng.latitude, latLng.longitude,1);
                    if (addresses.size() == 0 || address.split(", ").length < 2) {
                        Toast.makeText(this, "Адрес не определен.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    locality = addresses.get(0).getLocality();
                    myDirections.add(new Direction(address, dir_radius, latLng, locality, false));
                    dialogDirAction();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (requestCode == MY_PLACE_CODE) {
            if (resultCode == RESULT_OK) {
                myPlace = PlacePicker.getPlace(this, data);
                currlatitude = myPlace.getLatLng().latitude;
                currlongitude = myPlace.getLatLng().longitude;
                String address = myPlace.getAddress().toString();
                LatLng latLng = new LatLng(currlatitude, currlongitude);
//                String locality;
                try {
                    List<Address> addresses = new Geocoder(this, Locale.getDefault())
                            .getFromLocation(latLng.latitude, latLng.longitude,1);
                    if (addresses.size() == 0 || address.split(", ").length < 2) {
                        Toast.makeText(this, "Адрес не определен.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String[] arr = address.split(", ");
                    address = arr[0] + ", " + arr[1];
                    tvAddress.setText(address);
                    locality = addresses.get(0).getLocality();
                    tvLocality.setText(locality);
                    my_place = true;
                    locationListenerOFF();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
//*************************************************************************************
    public class HttpPost extends AsyncTask<String,Void,String> {
//*************************************************************************************
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... args) {
            String resultString;
            String pars = "";
            for (int i=1; i<args.length; i++) {
                if (i == args.length-1) {
                    pars += "par" + String.valueOf(i) + "=" + args[i];
                } else {
                    pars += "par" + String.valueOf(i) + "=" + args[i] + "&";
                }
            }
            try {
                URL url = new URL(args[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                byte[] data = pars.getBytes("UTF-8");
                os.write(data); os.flush(); os.close();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
                data = baos.toByteArray();
                baos.flush(); baos.close(); is.close();
                resultString = new String(data, "UTF-8");
                conn.disconnect();
            } catch (MalformedURLException e) { resultString = "MalformedURLException:" + e.getMessage();
            } catch (IOException e) { resultString = "IOException:" + e.getMessage();
            } catch (Exception e) { resultString = "Exception:" + e.getMessage();
            }
            return resultString;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
//*************************************************************************************
    public class Direction {
//*************************************************************************************
        private String  address;
        private double  radius;
        private LatLng  latlng;
        private String  locality;
        private boolean checked;

        Direction(String address, double radius, LatLng latlng, String locality, boolean checked) {
            this.address  = address;
            this.radius   = radius;
            this.latlng   = latlng;
            this.locality = locality;
            this.checked  = checked;
        }
        void setChecked(boolean checked) { this.checked = checked; }
    }
//**************************************************************************************************
    private class  Daemon extends AsyncTask<Void, String, Void> {
//**************************************************************************************************
        String     _getDriverUrl        = "http://185.25.119.3/BombilaDriver/get_driver.php";
        String     _get_free_ordersUrl  = "http://185.25.119.3/BombilaDriver/get_free_orders.php";
        JSONObject _driver = null;
        JSONArray  _orders = null;
        JSONObject _order  = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected Void doInBackground(Void... values) {
            String sdriver = toScript(_getDriverUrl, String.valueOf(driver_id));
            if (sdriver.equals("error")) {
                publishProgress("error");
                return null;
            }
            try {
                _driver = new JSONObject(sdriver);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            while (true) {
                if(isCancelled()) {
                    break;
                }
                if (currlatitude == 0.0 || currlongitude == 0.0 || locality == null) {
                    continue;
                }

//===> BombilaClient
                publishProgress("error");

                if (_order == null) {
                    _getOrders();
                    _bombila();
                    publishProgress("show_orders");
                }
                else {
//                    if (__getOrderStatus().equals("delete")) {
//                        __order = null;
//                        publishProgress("get_orders");
//                        continue;
//                    }
//                    __orderStatus();
//                    sleep(__delay);
                    continue;
                }

                sleep(2);
            }

//            finish();
            return null;
        }
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if(values[0].equals("error")) {
String s = String.valueOf(currlatitude) + "  " + String.valueOf(currlongitude);
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
//                Toast.makeText(MainActivity.this,"Ошибка регистрации.",
//                        Toast.LENGTH_SHORT).show();
            }
            if(values[0].equals("show_orders")) {
                _showOrders();
            }
            if(values[0].equals("accept")) {
                mTTS.speak("Заказ от Бомбилы.", TextToSpeech.QUEUE_FLUSH, null);
                Toast.makeText(MainActivity.this,"Заказ от Бомбилы.",
                        Toast.LENGTH_LONG).show();
            }
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        String toScript(String... args) {
            String resultString;
            String pars = "";
            for (int i=1; i<args.length; i++) {
                if (i == args.length-1) {
                    pars += "par" + String.valueOf(i) + "=" + args[i];
                } else {
                    pars += "par" + String.valueOf(i) + "=" + args[i] + "&";
                }
            }
            try {
                URL url = new URL(args[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                byte[] data = pars.getBytes("UTF-8");
                os.write(data); os.flush(); os.close();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
                data = baos.toByteArray();
                baos.flush(); baos.close(); is.close();
                resultString = new String(data, "UTF-8");
                conn.disconnect();
            } catch (MalformedURLException e) { resultString = "MalformedURLException:" + e.getMessage();
            } catch (IOException e) { resultString = "IOException:" + e.getMessage();
            } catch (Exception e) { resultString = "Exception:" + e.getMessage();
            }
            return resultString;
        }

        void _showOrders() {
            dataLv.clear();
            int length = _orders.length();
            tvOrdersInfo.setText("Заказов: " +  String.valueOf(length));

            Map<String, String> m;

            try {
                for (int i = 0; i < length; i++) {
                    JSONObject obj = _orders.getJSONObject(i);

                    String order_id = obj.getString("id");

                    JSONObject data = obj.getJSONObject("data");
                    String local = data.getJSONArray("localities").getString(0);
                    JSONArray addresses = data.getJSONArray("addresses");
                    String[] arr = addresses.getString(0).split(", ");
                    String address = arr[0] + ", " + arr[1];
                    if (!locality.equals(local)) address += " (" + local + ")";
                    String route = "";
                    for (int k=1; k<addresses.length(); k++) {
                        local = data.getJSONArray("localities").getString(k);
                        arr = addresses.getString(k).split(", ");
                        String dir = arr[0] + ", " + arr[1];
                        if (!locality.equals(local)) dir += " (" + local + ")";
                        route += "=>" + dir + " ";
                    }
                    String time = data.getString("on_time");
                    if (!time.equals("")) {
                        time = "[" + time + "]";
                    }
                    String price = data.getString("cost_total") + "грн.";

                    double latitude  = data.getJSONArray("coordes")
                                           .getJSONArray(0)
                                           .getDouble(0);
                    double longitude = data.getJSONArray("coordes")
                                           .getJSONArray(0)
                                           .getDouble(1);
                    double d = new BigDecimal(_getDistance(currlatitude, currlongitude, latitude, longitude)).
                            setScale(2, RoundingMode.UP).doubleValue();
                    String dist = String.valueOf(d);

                    String distance = "(" + dist + ")";
                    if (d < 10.00) dist = "0" + dist;

                    String fot = "";
                    if (!locality.equals(local)) fot = "[Межгород]";

                    m = new HashMap<>();
                    m.put("order_id", order_id);
                    m.put("address", address);
                    m.put("dist", dist);
                    m.put("distance", distance);
                    m.put("time", time);
                    m.put("fot", fot);
                    m.put("price", price);
                    m.put("route", route);
//                    m.put("route", route.replace('&', ' '));
                    dataLv.add(m);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Collections.sort(dataLv, new Comparator<Map<String, String>>() {
                @Override
                public int compare(Map<String, String> m1, Map<String, String> m2) {
                    String d1 = m1.get("dist");
                    String d2 = m2.get("dist");
                    return d1.compareTo(d2);
                }
            });
            sAdapter.notifyDataSetChanged();
        }

        void _bombila() {
    //        if (!pilot) return;
            try {
                for (int i=0; i<_orders.length(); i++) {
                    JSONObject order = _orders.getJSONObject(i);
                    JSONObject data = order.getJSONObject("data");
                    JSONArray ltlns = data.getJSONArray("coordes");

                    if (data.getInt("cost_total") < min_cost) continue;
                    if (!on_time && !data.getString("on_time").equals("")) continue;
                    if (!big_route && ltlns.length()>2) continue;
                    double _latitude = ltlns.getJSONArray(0).getDouble(0);
                    double _longitude = ltlns.getJSONArray(0).getDouble(1);
                    double  d = _getDistance(currlatitude, currlongitude, _latitude, _longitude);
    //                if (d > radius) continue;

                    double _end_latitude = ltlns.getJSONArray(ltlns.length()-1).getDouble(0);
                    double _end_longitude = ltlns.getJSONArray(ltlns.length()-1).getDouble(1);
                    for (Direction direction : myDirections) {
    //                    if (!direction.checked) continue;
                        double dir_latitude = direction.latlng.latitude;
                        double dir_longitude = direction.latlng.longitude;
                        d = _getDistance(_end_latitude, _end_longitude, dir_latitude, dir_longitude);
                        if (d > direction.radius) continue;
                        break;
                    }
                    boolean b = _accept(order.getInt("id"));
                    if (b) break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public boolean _accept(long order_id) {
 /*
            String driver = login + " " + password;
            String id = String.valueOf(order_id);
            driver_info = getSharedPreferences("bombila_pref", MODE_PRIVATE)
                    .getString("driver_info", "");
            String res = toScript("http://185.25.119.3/BombilaClient/accept_order.php",
                    driver, id, driver_info);

            if (res.equals("error")) return false;

            try {
                __order = new JSONObject(res);
                __status = __order.getString("status");
                __accept_time = Calendar.getInstance().getTimeInMillis();
                JSONObject data = new JSONObject(__order.getString("data"));
                data.put("accept_time", __accept_time);
                __order.remove("data");
                __order.put("data", data);
                boolean b = __updateOrder(data.toString(), __status);
            } catch (JSONException e) {
                e.printStackTrace();
            }
*/
            return true;
        }

        void _getOrders() {
//            if (!pilot || !__checkDriverInfo()) return;

            String result = toScript(_get_free_ordersUrl);
            try {
                JSONObject jresult = new JSONObject(result);
                JSONArray arr_id    = jresult.getJSONArray("id");
                JSONArray arr_phone = jresult.getJSONArray("phone");
                JSONArray arr_data  = jresult.getJSONArray("data");
                JSONArray arr_status = jresult.getJSONArray("status");
                JSONArray orders = new JSONArray();
                for (int i=0; i<arr_id.length(); i++) {
                    JSONObject jobj  = new JSONObject();
                    jobj.put("id", arr_id.getLong(i));
                    jobj.put("phone", arr_phone.getString(i));
                    jobj.put("data", new JSONObject(arr_data.getString(i)));
                    jobj.put("status", arr_status.getString(i));

                    orders.put(jobj);
                }
                _orders = orders;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        double _getDistance(double s1, double d1, double s2, double d2) {
            return 111.2*Math.sqrt(Math.pow(s1-s2,2)+Math.pow((d1-d2)*Math.cos(Math.PI*s1/180),2));
        }
    }
}
//**************************************************************************************************
