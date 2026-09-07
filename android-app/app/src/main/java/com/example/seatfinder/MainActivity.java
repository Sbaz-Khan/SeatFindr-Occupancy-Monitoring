package com.example.seatfinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Spinner spinBldg, spinFlr;
    EditText searchBar;
    Button btnSearch;
    TextView tvMapAddress;
    LinearLayout roomPreviewContainer;

    String selectedAddress = "";
    String userRole = "student";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().getStringExtra("USER_ROLE") != null) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        spinBldg = findViewById(R.id.spinBldg);
        spinFlr = findViewById(R.id.spinFlr);
        searchBar = findViewById(R.id.searchBar);
        btnSearch = findViewById(R.id.btnSearch);
        tvMapAddress = findViewById(R.id.tvMapAddress);
        roomPreviewContainer = findViewById(R.id.roomPreviewContainer);

        String[] bldgs = {"Hall", "JMSB", "EV"};

        spinBldg.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bldgs));

        spinBldg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = bldgs[position];
                String[] floors;

                if (selected.equals("JMSB")) {
                    floors = new String[]{"Floor 4", "Floor 5"};
                    selectedAddress = "1450 Guy St, Montreal, QC H3H 0A1";
                } else if (selected.equals("EV")) {
                    floors = new String[]{"Floor 4", "Floor 5"};
                    selectedAddress = "1515 Ste-Catherine St W, Montreal, QC H3G 2W1";
                } else {
                    floors = new String[]{"Floor 4", "Floor 5", "Floor 6", "Floor 7", "Floor 8", "Floor 9"};
                    selectedAddress = "1455 De Maisonneuve Blvd W, Montreal, QC H3G 1M8";
                }

                spinFlr.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, floors));
                tvMapAddress.setText("Google Maps Address: " + selectedAddress);

                showAvailableRoomPreview(selected, floors[0]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinFlr.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String building = spinBldg.getSelectedItem().toString();
                String floor = spinFlr.getSelectedItem().toString();
                showAvailableRoomPreview(building, floor);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        tvMapAddress.setOnClickListener(v -> {
            if (!selectedAddress.isEmpty()) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(selectedAddress));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Intent browserIntent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(selectedAddress))
                    );
                    startActivity(browserIntent);
                }
            }
        });

        btnSearch.setOnClickListener(v -> {
            String query = searchBar.getText().toString().trim().toLowerCase();

            String finalBldg = spinBldg.getSelectedItem().toString();
            String finalFlr = spinFlr.getSelectedItem().toString();

            if (query.equals("test")) {
                finalBldg = "Hall";
                finalFlr = "Floor 4";
            } else if (!query.isEmpty()) {
                String b = finalBldg;
                String f = finalFlr;

                if (query.contains("ev")) { b = "EV"; f = "Floor 4"; }
                else if (query.contains("jmsb") || query.contains("mb")) { b = "JMSB"; f = "Floor 4"; }
                else if (query.contains("hall") || query.contains("h")) { b = "Hall"; f = "Floor 4"; }

                if (query.contains("4")) f = "Floor 4";
                else if (query.contains("5")) f = "Floor 5";
                else if (query.contains("6")) f = "Floor 6";
                else if (query.contains("7")) f = "Floor 7";
                else if (query.contains("8")) f = "Floor 8";
                else if (query.contains("9")) f = "Floor 9";

                finalBldg = b;
                finalFlr = f;
            }

            searchBar.setText("");
            searchBar.clearFocus();

            int bIdx = finalBldg.equals("EV") ? 2 : (finalBldg.equals("JMSB") ? 1 : 0);
            spinBldg.setSelection(bIdx);

            String[] floors;
            if (finalBldg.equals("JMSB")) {
                floors = new String[]{"Floor 4", "Floor 5"};
                selectedAddress = "1450 Guy St, Montreal, QC H3H 0A1";
            } else if (finalBldg.equals("EV")) {
                floors = new String[]{"Floor 4", "Floor 5"};
                selectedAddress = "1515 Ste-Catherine St W, Montreal, QC H3G 2W1";
            } else {
                floors = new String[]{"Floor 4", "Floor 5", "Floor 6", "Floor 7", "Floor 8", "Floor 9"};
                selectedAddress = "1455 De Maisonneuve Blvd W, Montreal, QC H3G 1M8";
            }

            boolean validFloor = false;
            for (String val : floors) {
                if (val.equals(finalFlr)) {
                    validFloor = true; break;
                }
            }
            if (!validFloor) finalFlr = floors[0];

            spinFlr.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, floors));
            tvMapAddress.setText("Google Maps Address: " + selectedAddress);

            for (int i = 0; i < floors.length; i++) {
                if (floors[i].equals(finalFlr)) {
                    spinFlr.setSelection(i);
                    break;
                }
            }

            showAvailableRoomPreview(finalBldg, finalFlr);

            String path = finalBldg + "/" + finalFlr.toLowerCase().replace(" ", "");
            Intent intent = new Intent(MainActivity.this, RoomActivity.class);
            intent.putExtra("DB_PATH", path);
            intent.putExtra("USER_ROLE", userRole); // Pass role down to Room Activity
            startActivity(intent);
        });
    }

    private void showAvailableRoomPreview(String building, String floor) {
        roomPreviewContainer.removeAllViews();

        List<RoomPreview> rooms = getPreviewRooms(building, floor);

        Collections.sort(rooms, (r1, r2) -> Integer.compare(r2.available, r1.available));

        int count = Math.min(rooms.size(), 3);

        for (int i = 0; i < count; i++) {
            RoomPreview room = rooms.get(i);
            float percent = (room.available * 100f) / room.capacity;

            int statusDrawable;
            if (percent >= 65f) {
                statusDrawable = R.drawable.occupancy_low;
            } else if (percent >= 30f) {
                statusDrawable = R.drawable.occupancy_med;
            } else {
                statusDrawable = R.drawable.occupancy_high;
            }

            View roomView = LayoutInflater.from(this)
                    .inflate(R.layout.item_room, roomPreviewContainer, false);

            TextView tvRoomName = roomView.findViewById(R.id.tvRoomName);
            TextView tvRoomStats = roomView.findViewById(R.id.tvRoomStats);
            View occupancyStatus = roomView.findViewById(R.id.occupancyStatus);

            tvRoomName.setText(room.name);
            tvRoomStats.setText("Available: " + room.available + " / " + room.capacity);

            setOccupancyStatus(occupancyStatus, statusDrawable);

            roomPreviewContainer.addView(roomView);
        }

        if (roomPreviewContainer.getChildCount() == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No rooms found.");
            emptyText.setPadding(8, 16, 8, 16);
            roomPreviewContainer.addView(emptyText);
        }
    }

    private void setOccupancyStatus(View v, int drawableResId) {
        if (v instanceof android.widget.ImageView){
            ((android.widget.ImageView) v).setImageResource(drawableResId);
        } else {
            v.setBackgroundResource(drawableResId);
        }
    }

    private List<RoomPreview> getPreviewRooms(String building, String floor) {
        List<RoomPreview> rooms = new ArrayList<>();

        // Room A TARGET is removed from here entirely so it doesn't display out-of-sync data!

        if (building.equals("Hall") && floor.equals("Floor 4")) {
            rooms.add(new RoomPreview("H-401", 34, 22));
            rooms.add(new RoomPreview("H-403", 28, 8));
            rooms.add(new RoomPreview("H-405", 15, 5));
            rooms.add(new RoomPreview("H-407", 42, 37));
            rooms.add(new RoomPreview("H-411", 18, 0));
            rooms.add(new RoomPreview("H-420", 65, 40));
        } else if (building.equals("Hall") && floor.equals("Floor 5")) {
            rooms.add(new RoomPreview("H-501", 22, 13));
            rooms.add(new RoomPreview("H-503", 38, 22));
            rooms.add(new RoomPreview("H-505", 45, 23));
            rooms.add(new RoomPreview("H-507", 16, 0));
            rooms.add(new RoomPreview("H-513", 55, 45));
            rooms.add(new RoomPreview("H-520", 80, 40));
        } else if (building.equals("Hall") && floor.equals("Floor 6")) {
            rooms.add(new RoomPreview("H-601", 35, 27));
            rooms.add(new RoomPreview("H-603", 26, 16));
            rooms.add(new RoomPreview("H-604", 40, 22));
            rooms.add(new RoomPreview("H-606", 19, 4));
            rooms.add(new RoomPreview("H-608", 52, 25));
            rooms.add(new RoomPreview("H-611", 24, 22));
        } else if (building.equals("Hall") && floor.equals("Floor 7")) {
            rooms.add(new RoomPreview("H-701", 33, 27));
            rooms.add(new RoomPreview("H-703", 21, 7));
            rooms.add(new RoomPreview("H-705", 48, 43));
            rooms.add(new RoomPreview("H-711", 60, 35));
        } else if (building.equals("Hall") && floor.equals("Floor 8")) {
            rooms.add(new RoomPreview("H-801", 27, 22));
            rooms.add(new RoomPreview("H-804", 31, 21));
            rooms.add(new RoomPreview("H-806", 14, 0));
            rooms.add(new RoomPreview("H-815", 75, 47));
        } else if (building.equals("Hall") && floor.equals("Floor 9")) {
            rooms.add(new RoomPreview("H-901", 18, 14));
            rooms.add(new RoomPreview("H-903", 42, 22));
            rooms.add(new RoomPreview("H-905", 36, 31));
        } else if (building.equals("JMSB") && floor.equals("Floor 4")) {
            rooms.add(new RoomPreview("MB-4.210", 29, 24));
            rooms.add(new RoomPreview("MB-4.230", 44, 37));
            rooms.add(new RoomPreview("MB-4.260", 17, 5));
            rooms.add(new RoomPreview("MB-4.280", 65, 60));
            rooms.add(new RoomPreview("MB-4.300", 110, 80));
        } else if (building.equals("JMSB") && floor.equals("Floor 5")) {
            rooms.add(new RoomPreview("MB-5.110", 32, 28));
            rooms.add(new RoomPreview("MB-5.210", 25, 12));
            rooms.add(new RoomPreview("MB-5.250", 55, 38));
            rooms.add(new RoomPreview("MB-5.270", 40, 35));
        } else if (building.equals("EV") && floor.equals("Floor 4")) {
            rooms.add(new RoomPreview("EV-4.101", 21, 17));
            rooms.add(new RoomPreview("EV-4.125", 38, 32));
            rooms.add(new RoomPreview("EV-4.150", 16, 5));
            rooms.add(new RoomPreview("EV-4.200", 50, 47));
            rooms.add(new RoomPreview("EV-4.250", 85, 60));
        } else if (building.equals("EV") && floor.equals("Floor 5")) {
            rooms.add(new RoomPreview("EV-5.105", 23, 20));
            rooms.add(new RoomPreview("EV-5.125", 34, 29));
            rooms.add(new RoomPreview("EV-5.155", 19, 10));
            rooms.add(new RoomPreview("EV-5.180", 42, 29));
            rooms.add(new RoomPreview("EV-5.220", 60, 58));
            rooms.add(new RoomPreview("EV-5.260", 90, 73));
        }

        return rooms;
    }

    private static class RoomPreview {
        String name;
        int capacity;
        int available;

        RoomPreview(String name, int capacity, int available) {
            this.name = name;
            this.capacity = capacity;
            this.available = available;
        }
    }
}