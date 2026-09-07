package com.example.seatfinder;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class RoomActivity extends AppCompatActivity {

    private Toolbar toolbarRoom;
    private TextView tvToolbarTitle, tvTarget, tvManualRefresh;
    private Spinner spinSort;
    private LinearLayout roomContainer;

    private String currentPath = "";
    private String currentSortOption = "Highest Availability Percentage";
    private String userRole = "student";

    private ArrayList<RoomData> currentRooms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        if (getIntent().getStringExtra("USER_ROLE") != null) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        toolbarRoom = findViewById(R.id.toolbarRoom);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvTarget = findViewById(R.id.tvTarget);
        tvManualRefresh = findViewById(R.id.tvManualRefresh);
        spinSort = findViewById(R.id.spinSort);
        roomContainer = findViewById(R.id.roomContainer);

        View legendOccupancyHigh = findViewById(R.id.legendOccupancyHigh);
        View legendOccupancyMed = findViewById(R.id.legendOccupancyMed);
        View legendOccupancyLow = findViewById(R.id.legendOccupancyLow);
        setOccupancyStatus(legendOccupancyHigh, R.drawable.occupancy_high);
        setOccupancyStatus(legendOccupancyMed, R.drawable.occupancy_med);
        setOccupancyStatus(legendOccupancyLow, R.drawable.occupancy_low);

        setupToolbar();
        setupSortSpinner();

        String path = getIntent().getStringExtra("DB_PATH");
        if (path != null) {
            currentPath = path;
            tvTarget.setText("Viewing: " + path);
            tvToolbarTitle.setText(formatToolbarTitle(path));
            generateRoomsForFloor(path);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbarRoom);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        if (toolbarRoom.getNavigationIcon() != null) {
            toolbarRoom.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }
    }

    private void setupSortSpinner() {
        String[] sortOptions = {
                "Highest Availability Percentage",
                "Most Seats Available",
                "Highest Capacity"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                sortOptions
        );
        spinSort.setAdapter(adapter);
        spinSort.setSelection(0, false);

        spinSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSortOption = sortOptions[position];
                if (!currentPath.isEmpty()) {
                    renderRooms();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_room, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        if (item.getItemId() == R.id.action_refresh) {
            if (!currentPath.isEmpty()) {
                generateRoomsForFloor(currentPath);
                updateManualRefreshTime();
                Toast.makeText(this, "Manual refresh completed", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateManualRefreshTime() {
        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        tvManualRefresh.setText("Last manual refresh: " + time);
    }

    private String formatToolbarTitle(String path) {
        String[] parts = path.split("/");
        if (parts.length == 2) {
            String building = parts[0];
            String floor = parts[1].replace("floor", "Floor ");
            return building + " / " + floor;
        }
        return "SeatFindr";
    }

    private void generateRoomsForFloor(String path) {
        currentRooms.clear();

        if (path.equals("Hall/floor4")) {
            currentRooms.add(new RoomData("Room A (Target)", 3, 0, true));
            currentRooms.add(new RoomData("H-401", 34, 12, false));
            currentRooms.add(new RoomData("H-403", 28, 20, false));
            currentRooms.add(new RoomData("H-405", 15, 10, false));
            currentRooms.add(new RoomData("H-407", 42, 5, false));
            currentRooms.add(new RoomData("H-411", 18, 18, false));
            currentRooms.add(new RoomData("H-420", 65, 25, false));
        } else if (path.equals("Hall/floor5")) {
            currentRooms.add(new RoomData("H-501", 22, 9, false));
            currentRooms.add(new RoomData("H-503", 38, 16, false));
            currentRooms.add(new RoomData("H-505", 45, 22, false));
            currentRooms.add(new RoomData("H-507", 16, 16, false));
            currentRooms.add(new RoomData("H-513", 55, 10, false));
            currentRooms.add(new RoomData("H-520", 80, 40, false));
        } else if (path.equals("Hall/floor6")) {
            currentRooms.add(new RoomData("H-601", 35, 8, false));
            currentRooms.add(new RoomData("H-603", 26, 10, false));
            currentRooms.add(new RoomData("H-604", 40, 18, false));
            currentRooms.add(new RoomData("H-606", 19, 15, false));
            currentRooms.add(new RoomData("H-608", 52, 27, false));
            currentRooms.add(new RoomData("H-611", 24, 2, false));
        } else if (path.equals("Hall/floor7")) {
            currentRooms.add(new RoomData("H-701", 33, 6, false));
            currentRooms.add(new RoomData("H-703", 21, 14, false));
            currentRooms.add(new RoomData("H-705", 48, 5, false));
            currentRooms.add(new RoomData("H-711", 60, 25, false));
        } else if (path.equals("Hall/floor8")) {
            currentRooms.add(new RoomData("H-801", 27, 5, false));
            currentRooms.add(new RoomData("H-804", 31, 10, false));
            currentRooms.add(new RoomData("H-806", 14, 14, false));
            currentRooms.add(new RoomData("H-815", 75, 28, false));
        } else if (path.equals("Hall/floor9")) {
            currentRooms.add(new RoomData("H-901", 18, 4, false));
            currentRooms.add(new RoomData("H-903", 42, 20, false));
            currentRooms.add(new RoomData("H-905", 36, 5, false));
        } else if (path.equals("JMSB/floor4")) {
            currentRooms.add(new RoomData("MB-4.210", 29, 5, false));
            currentRooms.add(new RoomData("MB-4.230", 44, 7, false));
            currentRooms.add(new RoomData("MB-4.260", 17, 12, false));
            currentRooms.add(new RoomData("MB-4.280", 65, 5, false));
            currentRooms.add(new RoomData("MB-4.300", 110, 30, false));
        } else if (path.equals("JMSB/floor5")) {
            currentRooms.add(new RoomData("MB-5.110", 32, 4, false));
            currentRooms.add(new RoomData("MB-5.210", 25, 13, false));
            currentRooms.add(new RoomData("MB-5.250", 55, 17, false));
            currentRooms.add(new RoomData("MB-5.270", 40, 5, false));
        } else if (path.equals("EV/floor4")) {
            currentRooms.add(new RoomData("EV-4.101", 21, 4, false));
            currentRooms.add(new RoomData("EV-4.125", 38, 6, false));
            currentRooms.add(new RoomData("EV-4.150", 16, 11, false));
            currentRooms.add(new RoomData("EV-4.200", 50, 3, false));
            currentRooms.add(new RoomData("EV-4.250", 85, 25, false));
        } else if (path.equals("EV/floor5")) {
            currentRooms.add(new RoomData("EV-5.105", 23, 3, false));
            currentRooms.add(new RoomData("EV-5.125", 34, 5, false));
            currentRooms.add(new RoomData("EV-5.155", 19, 9, false));
            currentRooms.add(new RoomData("EV-5.180", 42, 13, false));
            currentRooms.add(new RoomData("EV-5.220", 60, 2, false));
            currentRooms.add(new RoomData("EV-5.260", 90, 17, false));
        } else {
            currentRooms.add(new RoomData("No Data Available", 0, 0, false));
        }

        renderRooms();
    }

    private void renderRooms() {
        roomContainer.removeAllViews();
        sortRooms(currentRooms);

        for (RoomData room : currentRooms) {
            View roomView = addRoom(room);

            if (room.isLiveTarget) {
                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("buildings/Hall/floor4/RoomA/occupiedSeats");

                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Integer currentOcc = snapshot.getValue(Integer.class);
                        if (currentOcc != null) {
                            room.occupiedSeats = currentOcc;
                            updateStats(roomView, room);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
        }
    }

    private void sortRooms(ArrayList<RoomData> rooms) {
        switch (currentSortOption) {
            case "Most Seats Available":
                Collections.sort(rooms, (r1, r2) ->
                        Integer.compare(r2.getAvailableSeats(), r1.getAvailableSeats()));
                break;

            case "Highest Capacity":
                Collections.sort(rooms, (r1, r2) ->
                        Integer.compare(r2.capacity, r1.capacity));
                break;

            case "Highest Availability Percentage":
            default:
                Collections.sort(rooms, (r1, r2) -> {
                    int percentCompare = Float.compare(
                            r2.getAvailabilityPercent(),
                            r1.getAvailabilityPercent()
                    );
                    if (percentCompare != 0) {
                        return percentCompare;
                    }
                    return Integer.compare(r2.getAvailableSeats(), r1.getAvailableSeats());
                });
                break;
        }
    }

    private View addRoom(RoomData room) {
        View view = getLayoutInflater().inflate(R.layout.item_room, roomContainer, false);
        TextView tvName = view.findViewById(R.id.tvRoomName);
        tvName.setText(room.name);

        updateStats(view, room);

        view.setOnClickListener(v -> showRoomDetailsDialog(room));

        if ("admin".equals(userRole)) {
            view.setOnLongClickListener(v -> {
                showAdminDialog(room);
                return true;
            });
        }

        roomContainer.addView(view);
        return view;
    }

    // --- Custom Concordia Title Builder for Dialogs ---
    private TextView getCustomTitle(String text, boolean isAdmin) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setPadding(64, 48, 64, 48);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        if (isAdmin) {
            title.setBackgroundColor(Color.parseColor("#912338"));
            title.setTextColor(Color.WHITE);
        } else {
            title.setTextColor(Color.parseColor("#912338"));
        }
        return title;
    }

    // --- Applies Maroon Color to Dialog Buttons ---
    private void colorizeDialogButtons(AlertDialog dialog) {
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (pos != null) pos.setTextColor(Color.parseColor("#912338"));
        Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (neg != null) neg.setTextColor(Color.parseColor("#912338"));
    }

    private void showAdminDialog(RoomData room) {
        String toggleText = room.isClosed ? "Reopen Room" : "Close Room";
        String[] options = {toggleText, "Change Occupancy"};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(getCustomTitle("Admin Controls: " + room.name, true))
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        showClosedMessageDialog(room);
                    } else {
                        showChangeOccupancyDialog(room);
                    }
                })
                .create();
        dialog.show();
    }

    private void showClosedMessageDialog(RoomData room) {
        if (room.isClosed) {
            room.isClosed = false;
            renderRooms();
            return;
        }

        String[] reasons = {"Closed", "Maintenance", "Private Event", "Exam in Progress"};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(getCustomTitle("Select Reason", false))
                .setItems(reasons, (d, which) -> {
                    room.isClosed = true;
                    room.closeMessage = reasons[which];
                    renderRooms();
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> colorizeDialogButtons(dialog));
        dialog.show();
    }

    private void showChangeOccupancyDialog(RoomData room) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter amount (0 - " + room.capacity + ")");
        input.setPadding(64, 40, 64, 40);
        input.setTextSize(18);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(getCustomTitle("Override Occupancy", false))
                .setMessage("Update live occupancy for " + room.name + ":")
                .setView(input)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            colorizeDialogButtons(dialog);
            Button btnUpdate = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnUpdate.setOnClickListener(v -> {
                String val = input.getText().toString().trim();

                if (val.isEmpty()) {
                    Toast.makeText(this, "Please enter a number.", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int newOcc = Integer.parseInt(val);
                    if (newOcc < 0) {
                        Toast.makeText(this, "Cannot be negative.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newOcc > room.capacity) {
                        Toast.makeText(this, "Error: Exceeds room capacity of " + room.capacity, Toast.LENGTH_LONG).show();
                        return;
                    }

                    dialog.dismiss();
                    showConfirmOccupancyDialog(room, newOcc);

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number format.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showConfirmOccupancyDialog(RoomData room, int newOcc) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(getCustomTitle("Confirm Update", false))
                .setMessage("Set " + room.name + " occupancy to " + newOcc + "?")
                .setPositiveButton("Yes", (d, which) -> {
                    room.occupiedSeats = newOcc;

                    if (room.isLiveTarget) {
                        FirebaseDatabase.getInstance()
                                .getReference("buildings/Hall/floor4/RoomA/occupiedSeats")
                                .setValue(newOcc);
                    }
                    renderRooms();
                })
                .setNegativeButton("No", null)
                .create();

        dialog.setOnShowListener(d -> colorizeDialogButtons(dialog));
        dialog.show();
    }

    private void updateStats(View view, RoomData room) {
        TextView tvStats = view.findViewById(R.id.tvRoomStats);
        TextView tvName = view.findViewById(R.id.tvRoomName);
        View occupancy = view.findViewById(R.id.occupancyStatus);

        if (room.isClosed) {
            tvStats.setText(room.closeMessage);
            tvStats.setTextColor(Color.parseColor("#9E9E9E"));
            tvName.setTextColor(Color.parseColor("#9E9E9E"));
            // Fades the entire card instead of breaking the background
            view.setAlpha(0.5f);
            setOccupancyStatus(occupancy, R.drawable.occupancy_templ);
            return;
        } else {
            tvStats.setTextColor(Color.parseColor("#444444"));
            tvName.setTextColor(Color.parseColor("#000000"));
            view.setAlpha(1.0f); // Restores full opacity
        }

        int avail = room.capacity - room.occupiedSeats;
        if (avail < 0) avail = 0;

        tvStats.setText("Available: " + avail + " / " + room.capacity);

        if (room.capacity == 0) {
            setOccupancyStatus(occupancy, R.drawable.occupancy_templ);
            return;
        }

        float percent = ((float) avail / room.capacity) * 100f;

        if (percent >= 65) {
            setOccupancyStatus(occupancy, R.drawable.occupancy_low);
        } else if (percent >= 30) {
            setOccupancyStatus(occupancy, R.drawable.occupancy_med);
        } else {
            setOccupancyStatus(occupancy, R.drawable.occupancy_high);
        }
    }

    private String getStatusLabel(RoomData room) {
        if (room.isClosed) return room.closeMessage;
        if (room.capacity == 0) return "N/A";

        int avail = room.capacity - room.occupiedSeats;
        if (avail < 0) avail = 0;

        float percent = ((float) avail / room.capacity) * 100f;

        if (percent >= 65) {
            return "Available";
        } else if (percent >= 30) {
            return "Partially Full";
        } else {
            return "Crowded";
        }
    }

    private void showRoomDetailsDialog(RoomData room) {
        int avail = room.capacity - room.occupiedSeats;
        if (avail < 0) avail = 0;
        String status = getStatusLabel(room);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 40, 64, 40);

        TextView tvCap = new TextView(this);
        tvCap.setText("Total Capacity: " + room.capacity);
        tvCap.setTextSize(18);
        tvCap.setPadding(0, 0, 0, 16);

        TextView tvOcc = new TextView(this);
        tvOcc.setText("Occupied Seats: " + room.occupiedSeats);
        tvOcc.setTextSize(18);
        tvOcc.setPadding(0, 0, 0, 16);

        TextView tvAvail = new TextView(this);
        tvAvail.setText("Available Seats: " + avail);
        tvAvail.setTextSize(18);
        tvAvail.setPadding(0, 0, 0, 32);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(status.toUpperCase());
        tvStatus.setTextSize(20);
        tvStatus.setTypeface(null, Typeface.BOLD);

        // Status color matches the label
        if (status.equals("Available")) tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        else if (status.equals("Partially Full")) tvStatus.setTextColor(Color.parseColor("#FFC107"));
        else if (status.equals("Crowded")) tvStatus.setTextColor(Color.parseColor("#F44336"));
        else tvStatus.setTextColor(Color.parseColor("#9E9E9E"));

        layout.addView(tvCap);
        layout.addView(tvOcc);
        layout.addView(tvAvail);
        layout.addView(tvStatus);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(getCustomTitle(room.name, false))
                .setView(layout)
                .setPositiveButton("Close", null)
                .create();

        dialog.setOnShowListener(d -> colorizeDialogButtons(dialog));
        dialog.show();
    }

    private void setOccupancyStatus(View v, int drawableResId) {
        if (v instanceof android.widget.ImageView){
            ((android.widget.ImageView) v).setImageResource(drawableResId);
        } else {
            v.setBackgroundResource(drawableResId);
        }
    }

    private static class RoomData {
        String name;
        int capacity;
        int occupiedSeats;
        boolean isLiveTarget;
        boolean isClosed = false;
        String closeMessage = "";

        RoomData(String name, int capacity, int occupiedSeats, boolean isLiveTarget) {
            this.name = name;
            this.capacity = capacity;
            this.occupiedSeats = occupiedSeats;
            this.isLiveTarget = isLiveTarget;
        }

        int getAvailableSeats() {
            if (isClosed) return 0;
            return Math.max(capacity - occupiedSeats, 0);
        }

        float getAvailabilityPercent() {
            if (isClosed || capacity == 0) return 0f;
            return (getAvailableSeats() * 100f) / capacity;
        }
    }
}