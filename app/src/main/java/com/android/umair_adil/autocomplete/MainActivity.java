package com.android.umair_adil.autocomplete;

import android.app.SearchManager;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private String TAG = MainActivity.class.getSimpleName();
    Context context;
    Button addressButton;
    TextView latLongTV;
    AutoCompleteTextView text;
    List<String> suggestions;
    RequestQueue queue;
    String url;
    CursorAdapter suggestionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        queue = Volley.newRequestQueue(this);

        text = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView1);
        text.setOnItemSelectedListener(this);
        text.setOnItemClickListener(this);

        latLongTV = (TextView) findViewById(R.id.latLongTV);

        addressButton = (Button) findViewById(R.id.addressButton);
        addressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                String address = text.getText().toString();
                GeocodingLocation locationAddress = new GeocodingLocation();
                locationAddress.getAddressFromLocation(address,
                        getApplicationContext(), new GeocoderHandler());
            }
        });

        suggestions = new ArrayList<String>();
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count != 0) {

                } else {

                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                getLocations(s.toString());
            }
        });

        text.setThreshold(1);


    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
                               long arg3) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        text.setText(suggestionAdapter.getItem(arg2).toString());
        GeocodingLocation locationAddress = new GeocodingLocation();
        locationAddress.getAddressFromLocation(suggestionAdapter.getItem(arg2).toString(),
                getApplicationContext(), new GeocoderHandler());

    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
            latLongTV.setText(locationAddress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));

        suggestionAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                new int[]{android.R.id.text1},
                0);

        searchView.setSuggestionsAdapter(suggestionAdapter);
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                searchView.setQuery(suggestions.get(position), false);
                searchView.clearFocus();
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                getLocations(newText);
                return false;
            }
        });


        return true;
    }

    private void getLocations(String search) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://maps.google.com/maps/api/geocode/json?address=" + search + "&sensor=false",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.i(TAG, "Response: " + response);

                        suggestions.clear();
                        try {
                            JsonElement jelement = new JsonParser().parse(response);
                            JsonObject jobject = jelement.getAsJsonObject();
                            JsonArray results = jobject.getAsJsonArray("results");
                            for (int i = 0; i < results.size(); i++) {
                                jobject = results.get(i).getAsJsonObject();
                                String result = jobject.get("formatted_address").toString();
                                Log.i(TAG, "Address: " + result);
                                suggestions.add(result);
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        String[] columns = {
                                BaseColumns._ID,
                                SearchManager.SUGGEST_COLUMN_TEXT_1,
                                SearchManager.SUGGEST_COLUMN_INTENT_DATA
                        };

                        MatrixCursor cursor = new MatrixCursor(columns);

                        for (int i = 0; i < suggestions.size(); i++) {
                            String[] tmp = {Integer.toString(i), suggestions.get(i), suggestions.get(i)};
                            cursor.addRow(tmp);
                        }
                        suggestionAdapter.swapCursor(cursor);
                        text.setAdapter(suggestionAdapter);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error);
            }
        });
        queue.add(stringRequest);
    }
}