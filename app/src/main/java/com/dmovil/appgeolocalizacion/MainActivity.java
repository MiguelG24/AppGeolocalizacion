package com.dmovil.appgeolocalizacion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    //Declaracion de Boton
    private Button irMapa;

    private static final String TAG = MainActivity.class.getSimpleName();

    //Declaracion de Variables estaticas para los intervalos de actualizacion
    private static final String LOCATION_KEY = "location-key";
    public static final long UPDATE_INTERVAL = 1000;
    public static final long UPDATE_FASTEST_INTERVAL = UPDATE_INTERVAL / 2;

    // Location API
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private Location mLastLocation;

    // UI Declaracion de los TextView
    private TextView mLatitude;
    private TextView mLongitude;
    private TextView direction;

    // Códigos de petición
    public static final int REQUEST_LOCATION = 1;
    public static final int REQUEST_CHECK_SETTINGS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //se inicializa el boton
        irMapa = findViewById(R.id.btn_Mapa);

        //Evento del boton para pasar al activity del mapa
        irMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent irAMapa = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(irAMapa);
            }
        });

        // Referencias UI
        mLatitude = findViewById(R.id.tv_latitude);
        mLongitude = findViewById(R.id.tv_longitude);
        direction = findViewById(R.id.tv_direction);

        // Establecer punto de entrada para la API de ubicación
        buildGoogleApiClient();

        // Crear configuración de peticiones
        createLocationRequest();

        // Crear opciones de peticiones
        buildLocationSettingsRequest();

        // Verificar ajustes de ubicación actuales
        checkLocationSettings();

        updateValuesFromBundle(savedInstanceState);
    }

    private synchronized void buildGoogleApiClient() {
        //Se crea la conexion con la API de google y se añade la API la LocationServices.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();
    }

    private void createLocationRequest() {
        //Crea la configuracion de peticiones y establece los intervalos de actualizacion.
        mLocationRequest = new LocationRequest()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(UPDATE_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        //Crea las opciones de peticion y le pasa como parámetro el objeto LocationRequest que se
        //creo en el método anterior
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest)
                .setAlwaysShow(true);
        mLocationSettingsRequest = builder.build();
    }

    private void checkLocationSettings() {
        //Verifica los ajustes de peticion con el metodo setResultCallback de la clase PedingResult
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient, mLocationSettingsRequest
                );

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                Status status = result.getStatus();

                //Comprueba el estatus de la configuración con una instruccion switch
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //Si la ubicacion esta activada llama al método startLocationUpdate
                        Log.d(TAG, "Los ajustes de ubicación satisfacen la configuración.");
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //Si la ubicacion no esta activada muestra un dialogo pidiendo que
                        // se active la ubicacion.
                        try {
                            Log.d(TAG, "Los ajustes de ubicación no satisfacen la configuración. " +
                                    "Se mostrará un diálogo de ayuda.");
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.d(TAG, "El Intent del diálogo no funcionó.");
                            // Sin operaciones
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //Si se rechaza la activación solo envia un mensaje a la consola.
                        Log.d(TAG, "Los ajustes de ubicación no son apropiados.");
                        break;
                }
            }
        });
    }

    private void startLocationUpdates() {
        //Llama al metodo isLocationPermissionGranted para comprobar si siene los permisos antes
        // de hacer la peticion de la ubicacion por medio del metodo requestLocationUpdates de
        // LocationServices.
        if (isLocationPermissionGranted()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } else {
            //Si no tiene los permisos de localización llama al método manageDeniedPermission
            manageDeniedPermission();
        }
    }

    private boolean isLocationPermissionGranted() {
        //Verifica que la aplicacion tenga permisos para usar la localizacion y retorna true si
        // tiene los permisos o false si no los tiene.
        int permission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void manageDeniedPermission() {
        //Comprueba si la apliacion cuenta con los permisos de Ubicacion
        //Si los tiene entonces no hace nada.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

        } else {
            //Si no se tiene los permisos para acceder a la localizacion nuevamente muestra un
            // dialogo que pide acceso a la localizacion.
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    @Override
    protected void onPause() {
        //Detiene las peticiones de ubicacion si la aplicación pasa a segundo plano
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            //Si el objeto de GoogleApiClient no esta conectado llama a stopLocationUpdates.
            stopLocationUpdates();
        }
    }

    private void stopLocationUpdates() {
        //Detiene la actualización de la ubicación
        LocationServices.FusedLocationApi
                .removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onResume() {
        // Reinicia las peticiones de ubicacion.
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            //Si el objeto de GoogleApiClient esta conectado llama a startLocationUpdates.
            startLocationUpdates();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Obtenemos la última ubicación al ser la primera vez
        processLastLocation();
        // Iniciamos las actualizaciones de ubicación
        startLocationUpdates();
    }

    private void processLastLocation() {
        getLastLocation();
        if (mLastLocation != null) {
            updateLocationUI();
        }
    }

    private void getLastLocation() {
        //Llama al metodo isLocationPermissionGranted para comprobar si siene los permisos antes
        // de hacer la peticion de la ubicacion por medio del metodo requestLocationUpdates de
        // LocationServices.
        if (isLocationPermissionGranted()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //Obtiene la ubicacion del dispositivo.
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            //Si no tiene los permisos de localización llama al método manageDeniedPermission
            manageDeniedPermission();
        }
    }

    private void updateLocationUI() {
        //Establece la longitud y latitud en los TextView llamando al metodo getLatitude y
        // getLongitude del objeto mLastLocation de la clase LastLocation y convirtiendo el
        // valor double en una cadena de texto con el metodo valueOf de la clase String.
        mLatitude.setText(String.valueOf(mLastLocation.getLatitude()));
        mLongitude.setText(String.valueOf(mLastLocation.getLongitude()));

        //Obtener la direccion de la calle a partir de la latitud y la longitud
        //En la conficion comprueba que la latitud y la longitud sean diferentes a cero.
        if (mLastLocation.getLatitude() != 0.0 && mLastLocation.getLongitude() != 0.0) {
            try {
                //Crea un objeto de la clase Geocoder para obtener la dirección.
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                //Obtiene la latitud y longitud, y las agrega a una lista de la clase Address.
                List<Address> list = geocoder.getFromLocation(
                        mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                if (!list.isEmpty()) {
                    //Si la lista no esta vacía pasa la direccion obtenida a una cadena de texto
                    //que manda al objeto direccion de TextView.
                    Address dirCalle = list.get(0);
                    direction.setText("Mi direccion es: \n"
                            + dirCalle.getAddressLine(0));
                }
            } catch (IOException e) {
                //Captura una excepción de tipo IOException.
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Protegemos la ubicación actual antes del cambio de configuración
        outState.putParcelable(LOCATION_KEY, mLastLocation);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Este metodo obtiene la respuesta del dialogo lanzado para activar los servicios de ubicacion.
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        //Si el usuario los activa se llama a los métodos processLastLocation y
                        // startLocationUpdates.
                        Log.d(TAG, "El usuario permitió el cambio de ajustes de ubicación.");
                        processLastLocation();
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        //Si el usuario no los activa solo se envía un mensaje por consola.
                        Log.d(TAG, "El usuario no permitió el cambio de ajustes de ubicación");
                        break;
                }
                break;
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Si los permisos de ubicación estan activado llama al método startLocationUpdates
                startLocationUpdates();

            } else {
                Toast.makeText(this, "Permisos no otorgados", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            //Comprueba si existen valores de ubicacion guardados en un objeto Bundle
            if (savedInstanceState.containsKey(LOCATION_KEY)) {
                //Recupera la ubicacion guardada y la actualiza con el método updateLocationUI.
                mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);

                updateLocationUI();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Se llama cuando la conexión con la Api de Google se pierde.
        Log.d(TAG, "Conexión suspendida");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Si la conexión falla manda un mensaje con la clase Toast y muestra el codigo de error.
        Toast.makeText(
                this,
                "Error de conexión con el código:" + connectionResult.getErrorCode(),
                Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        //Se ejecuta cada vez que la ubicacion cambia.
        Log.d(TAG, String.format("Nueva ubicación: (%s, %s)",
                location.getLatitude(), location.getLongitude()));
        //Pasa la nueva ubicacion al objeto mLastLocation y actualiza la ubicación en los TexView.
        mLastLocation = location;
        updateLocationUI();
    }
}