package com.example.proyecto;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cargar idioma desde SharedPreferences
        SharedPreferences preferences = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String idioma = preferences.getString("idioma", "es");
        setLocale(this, idioma);
    }

    public void setLocale(Context context, String idiomaCode) {
        Locale locale = new Locale(idiomaCode);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.setLocale(locale);
        context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
    }
}

