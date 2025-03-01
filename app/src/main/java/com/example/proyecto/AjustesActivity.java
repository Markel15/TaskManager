package com.example.proyecto;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import java.util.Locale;

public class AjustesActivity extends BaseActivity {

    private Switch switchTema;
    private Spinner spinnerIdioma;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        switchTema = findViewById(R.id.switchTema);
        spinnerIdioma = findViewById(R.id.spinnerIdioma);

        // Configura el switch según la preferencia cargada
        checkNightModeActivated();

        switchTema.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    saveNightModeState(true);
                }
                else{
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    saveNightModeState(false);
                }
            }
        });

        // Configurar el spinner con las opciones de idioma
        String[] idiomas = {"Español", "English", "Euskera"};
        // Códigos de idioma correspondientes
        final String[] codigos = {"es", "en", "eu"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, idiomas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdioma.setAdapter(adapter);

        // Recuperar el idioma actual de las preferencias
        String idiomaActual = prefs.getString("idioma", "es");
        int pos = 0;
        for (int i = 0; i < codigos.length; i++) {
            if (codigos[i].equals(idiomaActual)) {
                pos = i;
                break;
            }
        }
        spinnerIdioma.setSelection(pos);

        // Listener para cambiar el idioma al seleccionar una opción
        spinnerIdioma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int posicion, long id) {
                String selectedLang = codigos[posicion];
                if (!selectedLang.equals(idiomaActual)) {
                    setLocale(selectedLang);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void saveNightModeState(boolean b) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("modo_noche", b);
        editor.apply();
    }

    public void checkNightModeActivated(){
        if(prefs.getBoolean("modo_noche",false)){
            switchTema.setChecked(true);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else{
            switchTema.setChecked(false);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // Método para cambiar el idioma de la aplicación
    // Adapatción del código del laboratorio 02 de eGela
    private void setLocale(String langCode) {
        Locale nuevaLoc = new Locale(langCode);
        Locale.setDefault(nuevaLoc);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.setLocale(nuevaLoc);
        config.setLayoutDirection(nuevaLoc);
        Context context = getBaseContext().createConfigurationContext(config);
        getBaseContext().getResources().updateConfiguration(config,context.getResources().getDisplayMetrics());

        // Guardar el idioma seleccionado en las preferencias
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        prefs.edit().putString("idioma", langCode).apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
