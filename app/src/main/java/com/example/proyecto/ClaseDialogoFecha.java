package com.example.proyecto;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.widget.DatePicker;
import java.util.Calendar;

public class ClaseDialogoFecha extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    /**Basado en el código de la presentación del tema 05_Dialogs_y_Notificaciones de eGela
       Adaptado para hacerlo funcionar acorde a la aplicación desarrollada e implementar el metodo onDateSet que no estaba implementado**/
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Obtener la fecha actual como valor por defecto
        Calendar calendario = Calendar.getInstance();
        int año = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int dia = calendario.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(getActivity(), this, año, mes, dia);
    }

    @Override
    public void onDateSet(DatePicker view, int año, int mes, int dia) {
        // Configura un calendario con la fecha seleccionada
        Calendar calendario = Calendar.getInstance();
        calendario.set(año, mes, dia);
        long fechaSeleccionada = calendario.getTimeInMillis();

        // Comunica la fecha a la actividad, si ésta implementa la interfaz
        if (getActivity() instanceof OnFechaSelectedListener) {
            ((OnFechaSelectedListener) getActivity()).onFechaSeleccionada(fechaSeleccionada);
        }
    }
}
