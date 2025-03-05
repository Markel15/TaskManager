package com.example.proyecto;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Date;

public class CalendarFragment extends Fragment {

    // Interfaz para notificar la fecha seleccionada
    public interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }

    private OnDateSelectedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDateSelectedListener) {
            listener = (OnDateSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " debe implementar OnDateSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        CalendarView calendarView = view.findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth);
                Date selectedDate = cal.getTime();
                if (listener != null) {
                    listener.onDateSelected(selectedDate);
                }
            }
        });
        Date defaultDate = new Date(calendarView.getDate());
        if (listener != null) {
            listener.onDateSelected(defaultDate);
        }
        return view;
    }
}
