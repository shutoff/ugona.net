package net.ugona.plus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public abstract class ArrayAdapter extends BaseAdapter {

    final String DROP_DOWN = "dropdown";
    final String NON_DROP_DOWN = "nondropdown";
    Spinner spinner;
    LayoutInflater inflater;
    ArrayAdapter(Spinner spinner) {
        this.spinner = spinner;
        inflater = LayoutInflater.from(spinner.getContext());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if ((v != null) && !v.getTag().equals(NON_DROP_DOWN))
            v= null;
        if (v == null) {
            v = inflater.inflate(R.layout.list_item, null);
            v.setTag(NON_DROP_DOWN);
        }
        TextView tvName = (TextView) v;
        tvName.setText(getItem(position).toString());
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if ((v != null) && !v.getTag().equals(DROP_DOWN))
            v= null;
        if (v == null) {
            v = inflater.inflate(R.layout.list_dropdown_item, null);
            v.setTag(DROP_DOWN);
        }
        TextView tvName = (TextView) v;
        tvName.setText(getItem(position).toString());
        String fontName = "Exo2-";
        fontName += (position == spinner.getSelectedItemPosition()) ? "Medium" : "Light";
        tvName.setTypeface(Font.getFont(inflater.getContext(), fontName));
        return v;
    }
}
