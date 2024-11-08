package com.fit2081.a2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.fit2081.a2.KeyStore;
import com.fit2081.a2.components.FragmentListCategory;
import com.fit2081.a2.schemas.Category;
import com.fit2081.a2.schemas.Event;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class NewEventUtils {
    private static ArrayList<Event> events = new ArrayList<>();
    private static Gson gson = new Gson();
    public static void onCreateNewEventButtonClick(
            Context context,
            View view,
            EditText etEventId,
            EditText etEventName,
            EditText etEventCategoryId,
            EditText etTicketsAvailable,
            String[] splitMessage,
            Switch isEventActive
    ) {

        String eventId = etEventId.getText().toString().isEmpty() ? generateEventId() : etEventId.getText().toString();
        String[] temporaryMessage = new String[4];
        temporaryMessage[0] = etEventName.getText().toString();
        temporaryMessage[1] = etEventCategoryId.getText().toString();
        temporaryMessage[2] = etTicketsAvailable.getText().toString();
        temporaryMessage[3] = String.valueOf(isEventActive.isChecked());
        boolean isValid = checkValidMessage(context, temporaryMessage);

        if (isValid) {
            splitMessage = temporaryMessage;
            etEventName.setText("");
            etEventCategoryId.setText("");
            etTicketsAvailable.setText("");
            isEventActive.setChecked(false);

            if (splitMessage[3].isEmpty()) {
                splitMessage[3] = "false";
            }

            SharedPreferences sharedPreferences = context.getSharedPreferences(KeyStore.FILE_NAME, context.MODE_PRIVATE);
            String getEventsStr = sharedPreferences.getString(KeyStore.KEY_EVENTS, "");
            Type type = new TypeToken<ArrayList<Event>>() {}.getType();
            events = gson.fromJson(getEventsStr, type);

            if (events == null) {
                events = new ArrayList<>();
            }

            Event event = new Event(eventId, splitMessage[0], splitMessage[1], splitMessage[2], Boolean.parseBoolean(splitMessage[3]));
            events.add(event);

            String eventsStr = gson.toJson(events);
            saveEventsToSharedPreference(context, eventsStr);
            Snackbar.make(view, "Saved Event Successfully", Snackbar.LENGTH_LONG).setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String eventCategoryId = event.getEventCategoryId();
                            Type type = new TypeToken<ArrayList<Category>>() {}.getType();
                            String getCategoriesStr = sharedPreferences.getString(KeyStore.KEY_CATEGORIES, "");
                            ArrayList<Category> categories = gson.fromJson(getCategoriesStr, type);

                            if (categories == null) {
                                categories = new ArrayList<>();
                            }

                            for (Category category: categories) {
                                if (category.getCategoryId().equalsIgnoreCase(eventCategoryId)) {
                                    category.decrementEventCount();
                                    break;
                                }
                            }

                            events.remove(event);
                            String newEventsStr = gson.toJson(events);
                            String newCategoriesStr = gson.toJson(categories);
                            saveEventsToSharedPreference(context, newEventsStr);
                            saveCategoriesToSharedPreference(context, newCategoriesStr);
                            Snackbar.make(view, "Undid Save Event", Snackbar.LENGTH_SHORT).show();
                        }
                    }).show();
        }
    }

    public static boolean checkValidMessage(Context context, String[] splitMessage) {
        boolean isValid = true;
        if (splitMessage.length != 4) {
            isValid = false;
        } else {
            String eventName = splitMessage[0];
            String eventCategoryId = splitMessage[1];
            String ticketsAvailable = splitMessage[2];
            String isEventActive = splitMessage[3];
            if (eventName.isEmpty() || eventCategoryId.isEmpty()) {
                isValid = false;
                Toast.makeText(context, "Event Name and Category ID cannot be empty", Toast.LENGTH_SHORT).show();
            }

            if (!eventName.matches("[a-zA-Z0-9 ]+")  || !eventName.matches(".*[a-zA-Z].*")) {
                isValid = false;
                Toast.makeText(context, "Invalid Event Name", Toast.LENGTH_SHORT).show();
            }

            if (!ticketsAvailable.isEmpty()) {
                try {
                    int ticketsAvailableInt = Integer.parseInt(splitMessage[2]);

                    if (ticketsAvailableInt <= 0) {
                        isValid = false;
                        Toast.makeText(context, "Invalid Event Count", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    isValid = false;
                }
            }

            if (!isEventActive.isEmpty() && !isEventActive.equalsIgnoreCase("true") && !isEventActive.equalsIgnoreCase("false")) {
                isValid = false;
            }

            // Increment event count only if it is valid
            if (isValid) {
                SharedPreferences sharedPref = context.getSharedPreferences(KeyStore.FILE_NAME, context.MODE_PRIVATE);
                String getCategoriesStr = sharedPref.getString(KeyStore.KEY_CATEGORIES, "");
                Type type = new TypeToken<ArrayList<Category>>() {}.getType();
                ArrayList<Category> categories = gson.fromJson(getCategoriesStr, type);
                if (categories == null) {
                    categories = new ArrayList<>();
                }

                boolean isCategoryIdValid = false;
                for (Category category : categories) {
                    if (eventCategoryId.equalsIgnoreCase(category.getCategoryId())) {
                        isCategoryIdValid = true;
                        category.incrementEventCount();
                        break;
                    }
                }

                String newCategoriesStr = gson.toJson(categories);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(KeyStore.KEY_CATEGORIES, newCategoriesStr);
                editor.apply();

                if (!isCategoryIdValid) {
                    isValid = false;
                    Toast.makeText(context, "Category does not exist", Toast.LENGTH_SHORT).show();
                }
            }
        }

        return isValid;
    }

    public static String generateEventId() {
        String eventId = "E";
        for (int i = 0; i < 2; ++i) {
            eventId += (char)('A' + (int)(Math.random() * 26));
        }

        eventId += "-";
        for (int i = 0; i < 5; ++i) {
            eventId += (char)('0' + (int)(Math.random() * 10));
        }

        return eventId;
    }

    private static void saveEventsToSharedPreference(Context context, String events) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(KeyStore.FILE_NAME, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(KeyStore.KEY_EVENTS, events);
        editor.apply();
    }

    private static void saveCategoriesToSharedPreference(Context context, String categories) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(KeyStore.FILE_NAME, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(KeyStore.KEY_CATEGORIES, categories);
        editor.apply();
    }
}
