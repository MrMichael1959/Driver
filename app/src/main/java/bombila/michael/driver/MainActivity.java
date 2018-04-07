package bombila.michael.driver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//**************************************************************************************************
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
//**************************************************************************************************
    String registrationUrl = "http://185.25.119.3/BombilaDriver/registration.php";

    long    driver_id;
    String  driver_phone;
    String  driver_car;
    String  driver_color;
    String  driver_number;
    boolean on_time = false;
    boolean big_route = false;
    int     min_cost = 35;
    double  radius = 0.7;

    ArrayList<Direction> myDirections = new ArrayList<>();

    ImageView ivMenu;

//--------------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//--------------------------------------------------------------------------------------------------
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivMenu = findViewById(R.id.ivMenu); ivMenu.setOnClickListener(this);

        getPreferences();
        if (driver_id == 0L) dialogRegistration();

    }
//-------------------------------------------------------------------------------------
    @Override
    public void onClick(View v) {
//-------------------------------------------------------------------------------------
        switch (v.getId()) {
            case R.id.ivMenu:
                dialogMenu();
                break;
            default:
                break;
        }
    }
//-------------------------------------------------------------------------------------
    void dialogMenu() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.menu, null);
        final LinearLayout llDriver = view.findViewById(R.id.llDriver);
        final LinearLayout llSettings = view.findViewById(R.id.llSettings);
        final LinearLayout llDirections = view.findViewById(R.id.llDirections);
        final LinearLayout llExit = view.findViewById(R.id.llExit);

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
        switchOnTime.setChecked(on_time);
        switchRoute.setChecked(big_route);
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
                        getSharedPreferences("driver_pref", MODE_PRIVATE)
                                .edit()
                                .putBoolean("on_time", on_time)
                                .putBoolean("big_route", big_route)
                                .apply();
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
                dialogSelectDir();
            }
        });
        llAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                dialogAddDir();
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(MainActivity.this), 1);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });
        llDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDeleteDir();
            }
        });
    }
//-------------------------------------------------------------------------------------
    void dialogSelectDir() {
//-------------------------------------------------------------------------------------
    }
//-------------------------------------------------------------------------------------
    void dialogAddDir() {
//-------------------------------------------------------------------------------------
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), 1);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }
//-------------------------------------------------------------------------------------
    void dialogDeleteDir() {
//-------------------------------------------------------------------------------------
    }
//-------------------------------------------------------------------------------------
    void dialogDirections() {
//-------------------------------------------------------------------------------------
        final boolean[] checkedItems = {};
        final String[] addresses = {};

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
                    }
                })
                .setPositiveButton("Выбрать", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        StringBuilder state = new StringBuilder();
                        for (int i = 0; i < addresses.length; i++) {
                            state.append("" + addresses[i]);
                            if (checkedItems[i])
                                state.append(" выбран\n");
                            else
                                state.append(" не выбран\n");
                        }
                        Toast.makeText(getApplicationContext(),
                                state.toString(), Toast.LENGTH_LONG)
                                .show();
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
        else et.setText(String.valueOf(radius));

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
                        radius = Double.parseDouble(et.getText().toString());
                        getSharedPreferences("driver_pref", MODE_PRIVATE)
                                .edit()
                                .putFloat("radius", (float) radius)
                                .apply();
                        dialog.cancel();
                        dialogSettings();
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
                        getSharedPreferences("driver_pref", MODE_PRIVATE)
                                .edit()
                                .putInt("min_cost", min_cost)
                                .apply();
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
                        getSharedPreferences("driver_pref", MODE_PRIVATE)
                                .edit()
                                .putString("driver_number", driver_number)
                                .apply();
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
                        getSharedPreferences("driver_pref", MODE_PRIVATE)
                                .edit()
                                .putString("driver_color", driver_color)
                                .apply();
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
                        getSharedPreferences("driver_pref", MODE_PRIVATE)
                                .edit()
                                .putString("driver_car", driver_car)
                                .apply();
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
                        getSharedPreferences("driver_pref", MODE_PRIVATE)
                                .edit()
                                .putLong("driver_id", driver_id)
                                .putString("driver_phone", driver_phone)
                                .apply();
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
        min_cost      = sp.getInt    ("min_cost", 35);
        radius        = sp.getFloat  ("radius", (float) 0.7);
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
        JSONObject jdir = new JSONObject();
        try {
            for (Direction direction : myDirections) {
                jdir.put("address",  direction.address)
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//-------------------------------------------------------------------------------------
        if (requestCode == 1) {
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
                    myDirections.add(new Direction(address, latLng, locality, false));
//                    String str = directionsToString();
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
        private LatLng  latlng;
        private String  locality;
        private boolean checked;

        Direction(String address, LatLng latlng, String locality, boolean checked) {
            this.address  = address;
            this.latlng   = latlng;
            this.locality = locality;
            this.checked  = checked;
        }
    }
}
//**************************************************************************************************
