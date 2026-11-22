package com.example.weighttracker4000;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

// Pop-up shown above a point on the Trends graph when the user taps it.
// The text comes from the point itself: TrendsActivity stores it in each Entry's data
// (a date and weight, or a week's start date, average weight and number of entries).
public class ChartMarkerView extends MarkerView {

    private final TextView markerText;

    public ChartMarkerView(Context context) {
        super(context, R.layout.chart_marker);
        markerText = findViewById(R.id.markerText);
    }

    // Called each time a point is tapped: show that point's details
    @Override
    public void refreshContent(Entry entry, Highlight highlight) {
        Object details = entry.getData();
        markerText.setText(details != null ? details.toString() : "");
        super.refreshContent(entry, highlight);
    }

    // Center the pop-up horizontally over the point and draw it just above the point
    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 12f);
    }
}
