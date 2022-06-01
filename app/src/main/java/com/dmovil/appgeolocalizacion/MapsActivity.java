package com.dmovil.appgeolocalizacion;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.dmovil.appgeolocalizacion.databinding.ActivityMapsBinding;

import java.util.Locale;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Se llama al método getLocalizacion.
        getLocalizacion();
    }
    private void getLocalizacion() {
        //Comprueba si se tiene los permisos de ubicación.
        int permiso = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permiso == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                //Si no se tiene los permisos de ubicación mostrara un dialogo para hacer la
                // peticion de los permisos
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Se llama al metodo setOnMarkerClickListener que escucha cuando se toca un objeto Marker.
        googleMap.setOnMarkerClickListener(this);

        //Nuevamente berifica que la aplicación cuente con los permisos de localización
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //El fragmento se establece en mi ubicación.
        mMap.setMyLocationEnabled(true);

        //Crea un objeto locationManager para obtener la ubicación
        LocationManager locationManager = (LocationManager) MapsActivity.this.getSystemService(Context.LOCATION_SERVICE);
        //Se crea un objeto de la clase LocationListener para detectar los cambios de ubicación
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                //Se crea un objeto de la clase LarLng que obtiene la latitud y longitud del objeto
                // location que se pasa como parámetro al método onLocationChanged.
                LatLng miUbicacion = new LatLng(location.getLatitude(), location.getLongitude());
                //Al objeto mMap de la clase GoogleMap se le añade un nuevo Marker con posición
                // en la ubicacion guardada en el objeto miUbicacion y el titulo "Ubicación actual".
                mMap.addMarker(new MarkerOptions()
                                    .position(miUbicacion)
                                    .title("Ubicacion Actual"));
                //El enfoque del mapa se cambia a la ubicación guardada en miUbicacion.
                mMap.moveCamera((CameraUpdateFactory.newLatLng(miUbicacion)));
            }
        };
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        //Este metodo detecta y es llamado cuando se da click o se toca un Marker.
        //Se crea una cadena de texto con las coordenadas de la ubicacion actual
        String formatLatLng = String.format(Locale.getDefault(),
                "Ubicacion Actual\nLatitud = %f\nLongitud= %f", marker.getPosition().latitude, marker.getPosition().longitude);

        //Se muestran las coordenadas
        Toast.makeText(this, formatLatLng, Toast.LENGTH_LONG).show();
        return true;
    }
}