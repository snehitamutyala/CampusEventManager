package org.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main extends Application {

    private static final String DATA_FILE = "data/events.dat";
    private static final String ACTIVITY_FILE = "data/activity.dat";
    private static final String REPORT_FILE = "data/event_report.txt";

    private final List<Event> events = new ArrayList<>();
    private final List<Activity> activities = new ArrayList<>();

    private final ListView<Event> eventList = new ListView<>();
    private final ListView<String> participantList = new ListView<>();
    private final ListView<String> activityList = new ListView<>();

    private final ComboBox<Event> registrationEvent = new ComboBox<>();
    private final ComboBox<Event> participantEvent = new ComboBox<>();

    private Label totalEventsLabel;
    private Label totalStudentsLabel;
    private Label availableSeatsLabel;
    private Label fullEventsLabel;

    private Stage primaryStage;

    private String currentUsername;
    private String currentRole;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    // =========================================
    // EVENT CLASS
    // =========================================

    static class Event implements Serializable {

        private static final long serialVersionUID = 1L;

        String name;
        String date;
        String venue;
        int capacity;

        List<Student> students = new ArrayList<>();

        Event(String name, String date, String venue, int capacity) {
            this.name = name;
            this.date = date;
            this.venue = venue;
            this.capacity = capacity;
        }

        @Override
        public String toString() {
            return name + " (" + students.size() + "/" + capacity + ")";
        }
    }

    // =========================================
    // STUDENT CLASS
    // =========================================

    static class Student implements Serializable {

        private static final long serialVersionUID = 1L;

        String id;
        String name;

        Student(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " - " + id;
        }
    }

    // =========================================
    // ACTIVITY CLASS
    // =========================================

    static class Activity implements Serializable {

        private static final long serialVersionUID = 1L;

        String action;
        String description;
        String time;

        Activity(String action, String description) {
            this.action = action;
            this.description = description;
            this.time = LocalDateTime.now().format(TIME_FORMAT);
        }

        @Override
        public String toString() {
            return "[" + time + "] " + action + " - " + description;
        }
    }

    // =========================================
    // ROLE MANAGEMENT
    // =========================================

    private boolean isAdmin() {
        return "ADMIN".equals(currentRole);
    }

    private boolean isStudent() {
        return "STUDENT".equals(currentRole);
    }

    private boolean requireAdmin() {

        if (!isAdmin()) {
            showError("Access denied! Admin permission required.");
            return false;
        }

        return true;
    }

    // =========================================
    // ALERT MESSAGES
    // =========================================

    private void showMessage(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Campus Event Manager");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Campus Event Manager");
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================================
    // DATA STORAGE
    // =========================================

    private void saveData() {

        try {
            Files.createDirectories(Paths.get("data"));

            try (ObjectOutputStream out =
                         new ObjectOutputStream(
                                 new FileOutputStream(DATA_FILE))) {

                out.writeObject(events);
            }

        } catch (IOException e) {
            showError("Unable to save events: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {

        File file = new File(DATA_FILE);

        if (!file.exists()) return;

        try (ObjectInputStream in =
                     new ObjectInputStream(
                             new FileInputStream(file))) {

            Object data = in.readObject();

            if (data instanceof List<?>) {
                events.clear();
                events.addAll((List<Event>) data);
            }

        } catch (IOException | ClassNotFoundException e) {
            showError("Unable to load events: " + e.getMessage());
        }
    }

    // =========================================
    // ACTIVITY STORAGE
    // =========================================

    private void saveActivities() {

        try {
            Files.createDirectories(Paths.get("data"));

            try (ObjectOutputStream out =
                         new ObjectOutputStream(
                                 new FileOutputStream(ACTIVITY_FILE))) {

                out.writeObject(activities);
            }

        } catch (IOException e) {
            showError("Unable to save activities: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadActivities() {

        File file = new File(ACTIVITY_FILE);

        if (!file.exists()) return;

        try (ObjectInputStream in =
                     new ObjectInputStream(
                             new FileInputStream(file))) {

            Object data = in.readObject();

            if (data instanceof List<?>) {
                activities.clear();
                activities.addAll((List<Activity>) data);
            }

        } catch (IOException | ClassNotFoundException e) {
            showError("Unable to load activities: " + e.getMessage());
        }
    }

    private void logActivity(String action, String description) {

        activities.add(0, new Activity(action, description));

        if (activities.size() > 500) {
            activities.remove(activities.size() - 1);
        }

        saveActivities();
        refreshActivityList();
    }

    private void refreshActivityList() {

        activityList.setItems(
                FXCollections.observableArrayList(
                        activities.stream()
                                .map(Activity::toString)
                                .collect(Collectors.toList())
                )
        );
    }

    // =========================================
    // DATE VALIDATION
    // =========================================

    private boolean isValidDate(String date) {

        try {
            LocalDate parsed = LocalDate.parse(date, DATE_FORMAT);
            return !parsed.isBefore(LocalDate.now());

        } catch (Exception e) {
            return false;
        }
    }

    // =========================================
    // REFRESH ALL DATA
    // =========================================

    private void refreshLists() {

        Event selectedRegistration = registrationEvent.getValue();
        Event selectedParticipant = participantEvent.getValue();

        eventList.setItems(
                FXCollections.observableArrayList(events)
        );

        registrationEvent.setItems(
                FXCollections.observableArrayList(events)
        );

        participantEvent.setItems(
                FXCollections.observableArrayList(events)
        );

        if (selectedRegistration != null &&
                events.contains(selectedRegistration)) {
            registrationEvent.setValue(selectedRegistration);
        } else {
            registrationEvent.setValue(null);
        }

        if (selectedParticipant != null &&
                events.contains(selectedParticipant)) {
            participantEvent.setValue(selectedParticipant);
        } else {
            participantEvent.setValue(null);
        }

        updateDashboard();
        refreshActivityList();
    }

    // =========================================
    // DASHBOARD STATISTICS
    // =========================================

    private void updateDashboard() {

        int totalStudents = 0;
        int availableSeats = 0;
        int fullEvents = 0;

        for (Event event : events) {

            int registered = event.students.size();

            totalStudents += registered;

            availableSeats += Math.max(
                    0, event.capacity - registered
            );

            if (registered >= event.capacity) {
                fullEvents++;
            }
        }

        if (totalEventsLabel != null) {
            totalEventsLabel.setText(String.valueOf(events.size()));
        }

        if (totalStudentsLabel != null) {
            totalStudentsLabel.setText(String.valueOf(totalStudents));
        }

        if (availableSeatsLabel != null) {
            availableSeatsLabel.setText(String.valueOf(availableSeats));
        }

        if (fullEventsLabel != null) {
            fullEventsLabel.setText(String.valueOf(fullEvents));
        }
    }

    // =========================================
    // DASHBOARD STATISTIC CARDS
    // =========================================

    private VBox createStatCard(
            String heading,
            Label value,
            String color) {

        Label title = new Label(heading);

        title.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #33445C;"
        );

        value.setStyle(
                "-fx-font-size: 32px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #17365D;"
        );

        VBox card = new VBox(15, title, value);

        card.setPadding(new Insets(22));
        card.setMinHeight(125);
        card.setMaxWidth(Double.MAX_VALUE);

        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 14;"
        );

        return card;
    }

    // =========================================
    // EVENT OVERVIEW CARD
    // =========================================

    private VBox createEventOverviewCard(Event event) {

        Label name = new Label(event.name);

        name.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #17365D;"
        );

        int registered = event.students.size();

        int available = Math.max(
                0, event.capacity - registered
        );

        double progressValue = event.capacity > 0
                ? Math.min(1.0, (double) registered / event.capacity)
                : 0;

        ProgressBar progress = new ProgressBar(progressValue);
        progress.setMaxWidth(Double.MAX_VALUE);

        Label info = new Label(
                registered + " / " + event.capacity +
                        " students registered"
        );

        Label seats = new Label(
                available + " seats available"
        );

        String status = registered >= event.capacity
                ? "FULL"
                : "AVAILABLE";

        Label statusLabel = new Label(status);

        statusLabel.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-text-fill: " +
                        (status.equals("FULL") ? "#C0392B" : "#16834A") + ";"
        );

        Region spacer = new Region();

        HBox top = new HBox(name, spacer, statusLabel);
        top.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(10, top, info, progress, seats);

        card.setPadding(new Insets(15));
        card.setMaxWidth(Double.MAX_VALUE);

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #DCE3EC;" +
                        "-fx-border-radius: 12;"
        );

        return card;
    }

    // =========================================
    // DASHBOARD SCREEN
    // =========================================

    private VBox createDashboardScreen() {

        Label heading = new Label("Dashboard");

        heading.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #17365D;"
        );

        Label welcome = new Label(
                "Welcome, " + currentUsername +
                        " (" + currentRole + ")!"
        );

        totalEventsLabel = new Label("0");
        totalStudentsLabel = new Label("0");
        availableSeatsLabel = new Label("0");
        fullEventsLabel = new Label("0");

        GridPane grid = new GridPane();

        grid.setHgap(18);
        grid.setVgap(18);
        grid.setMaxWidth(Double.MAX_VALUE);

        grid.add(createStatCard("Total Events",
                totalEventsLabel, "#DCEBFF"), 0, 0);

        grid.add(createStatCard("Total Registrations",
                totalStudentsLabel, "#DDF5E5"), 1, 0);

        grid.add(createStatCard("Available Seats",
                availableSeatsLabel, "#FFF0D5"), 0, 1);

        grid.add(createStatCard("Full Events",
                fullEventsLabel, "#EDE2FF"), 1, 1);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        c2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(c1, c2);

        VBox eventCards = new VBox(12);

        Label overview = new Label("Event Overview");

        overview.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;"
        );

        if (events.isEmpty()) {

            eventCards.getChildren().add(
                    new Label("No events created yet.")
            );

        } else {

            for (Event event : events) {
                eventCards.getChildren().add(
                        createEventOverviewCard(event)
                );
            }
        }

        VBox box = new VBox(
                18, heading, welcome, grid, overview, eventCards
        );

        if (isAdmin()) {

            Label recentHeading = new Label("Recent Activities");

            recentHeading.setStyle(
                    "-fx-font-size: 20px;" +
                            "-fx-font-weight: bold;"
            );

            VBox recent = new VBox(8);

            int count = Math.min(5, activities.size());

            if (count == 0) {

                recent.getChildren().add(
                        new Label("No activities recorded yet.")
                );

            } else {

                for (int i = 0; i < count; i++) {

                    Label item = new Label(
                            activities.get(i).toString()
                    );

                    item.setWrapText(true);
                    recent.getChildren().add(item);
                }
            }

            box.getChildren().addAll(recentHeading, recent);
        }

        box.setPadding(new Insets(25));
        box.setFillWidth(true);

        box.setStyle("-fx-background-color: #F3F6FA;");

        updateDashboard();

        return box;
    }

    // =========================================
    // EVENT MANAGEMENT
    // =========================================

    private VBox createEventScreen() {

        if (!requireAdmin()) return new VBox();

        Label heading = new Label("Event Management");

        heading.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        TextField name = new TextField();
        name.setPromptText("Enter event name");

        TextField date = new TextField();
        date.setPromptText("DD-MM-YYYY");

        TextField venue = new TextField();
        venue.setPromptText("Enter venue");

        TextField capacity = new TextField();
        capacity.setPromptText("Enter maximum capacity");

        TextField search = new TextField();
        search.setPromptText("Search event");

        Button add = new Button("Create Event");
        Button update = new Button("Update Event");
        Button delete = new Button("Delete Event");
        Button searchButton = new Button("Search");
        Button showAll = new Button("Show All");

        eventList.setPrefHeight(350);

        add.setOnAction(e -> {

            if (!requireAdmin()) return;

            String n = name.getText().trim();
            String d = date.getText().trim();
            String v = venue.getText().trim();
            String c = capacity.getText().trim();

            if (n.isEmpty() || d.isEmpty() ||
                    v.isEmpty() || c.isEmpty()) {
                showMessage("Please fill all event details.");
                return;
            }

            if (!isValidDate(d)) {
                showMessage(
                        "Enter a valid future date in DD-MM-YYYY format."
                );
                return;
            }

            try {

                int cap = Integer.parseInt(c);

                if (cap <= 0) {
                    showMessage("Capacity must be greater than zero.");
                    return;
                }

                for (Event event : events) {
                    if (event.name.equalsIgnoreCase(n)) {
                        showMessage("Event already exists.");
                        return;
                    }
                }

                events.add(new Event(n, d, v, cap));

                saveData();
                logActivity("EVENT CREATED", n);
                refreshLists();

                name.clear();
                date.clear();
                venue.clear();
                capacity.clear();

                showMessage("Event created successfully!");

            } catch (NumberFormatException ex) {
                showMessage("Enter a valid capacity.");
            }
        });

        eventList.setOnMouseClicked(e -> {

            Event selected =
                    eventList.getSelectionModel().getSelectedItem();

            if (selected != null) {
                name.setText(selected.name);
                date.setText(selected.date);
                venue.setText(selected.venue);
                capacity.setText(String.valueOf(selected.capacity));
            }
        });

        update.setOnAction(e -> {

            if (!requireAdmin()) return;

            Event selected =
                    eventList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showMessage("Select an event to update.");
                return;
            }

            String n = name.getText().trim();
            String d = date.getText().trim();
            String v = venue.getText().trim();
            String c = capacity.getText().trim();

            if (n.isEmpty() || d.isEmpty() ||
                    v.isEmpty() || c.isEmpty()) {
                showMessage("Please fill all details.");
                return;
            }

            if (!isValidDate(d)) {
                showMessage("Enter a valid future date.");
                return;
            }

            try {

                int cap = Integer.parseInt(c);

                if (cap <= 0 || cap < selected.students.size()) {
                    showMessage(
                            "Capacity cannot be less than registered students."
                    );
                    return;
                }

                for (Event event : events) {

                    if (event != selected &&
                            event.name.equalsIgnoreCase(n)) {
                        showMessage("Another event has this name.");
                        return;
                    }
                }

                String oldName = selected.name;

                selected.name = n;
                selected.date = d;
                selected.venue = v;
                selected.capacity = cap;

                saveData();

                logActivity(
                        "EVENT UPDATED",
                        oldName + " changed to " + n
                );

                refreshLists();

                name.clear();
                date.clear();
                venue.clear();
                capacity.clear();

                showMessage("Event updated successfully!");

            } catch (NumberFormatException ex) {
                showMessage("Enter a valid capacity.");
            }
        });

        delete.setOnAction(e -> {

            if (!requireAdmin()) return;

            Event selected =
                    eventList.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showMessage("Select an event to delete.");
                return;
            }

            Alert confirm =
                    new Alert(Alert.AlertType.CONFIRMATION);

            confirm.setTitle("Delete Event");
            confirm.setHeaderText("Delete " + selected.name + "?");

            confirm.setContentText(
                    "Its registrations will also be deleted."
            );

            confirm.showAndWait().ifPresent(response -> {

                if (response == ButtonType.OK) {

                    String deletedName = selected.name;

                    events.remove(selected);

                    saveData();
                    logActivity("EVENT DELETED", deletedName);
                    refreshLists();

                    participantList.getItems().clear();

                    name.clear();
                    date.clear();
                    venue.clear();
                    capacity.clear();

                    showMessage("Event deleted successfully!");
                }
            });
        });

        searchButton.setOnAction(e -> {

            String keyword =
                    search.getText().trim().toLowerCase();

            if (keyword.isEmpty()) {
                showMessage("Enter an event name to search.");
                return;
            }

            List<Event> filtered = new ArrayList<>();

            for (Event event : events) {
                if (event.name.toLowerCase().contains(keyword)) {
                    filtered.add(event);
                }
            }

            eventList.setItems(
                    FXCollections.observableArrayList(filtered)
            );

            if (filtered.isEmpty()) {
                showMessage("No matching events found.");
            }
        });

        showAll.setOnAction(e -> {
            search.clear();
            refreshLists();
        });

        HBox buttons = new HBox(10, add, update, delete);

        HBox searchBox = new HBox(
                10, search, searchButton, showAll
        );

        VBox box = new VBox(
                10,
                heading,
                new Label("Event Name"), name,
                new Label("Event Date"), date,
                new Label("Venue"), venue,
                new Label("Maximum Capacity"), capacity,
                buttons,
                new Label("Search Events"), searchBox,
                new Label("Available Events"), eventList
        );

        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: #F3F6FA;");

        return box;
    }

    // =========================================
    // STUDENT EVENT VIEW - STAGE 9 UPDATED
    // =========================================

    private VBox createStudentEventsScreen() {

        Label heading = new Label("Available Campus Events");

        heading.setStyle(
                "-fx-font-size: 25px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #17365D;"
        );

        TextField search = new TextField();

        search.setPromptText(
                "Search by event name, date or venue"
        );

        search.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> filter = new ComboBox<>();

        filter.getItems().addAll(
                "All Events",
                "Available",
                "Full"
        );

        filter.setValue("All Events");
        filter.setMaxWidth(Double.MAX_VALUE);

        ListView<String> list = new ListView<>();

        list.setPrefHeight(450);

        list.setCellFactory(lv -> new ListCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);

                if (empty || item == null) {

                    setText(null);
                    setGraphic(null);

                } else {

                    Label label = new Label(item);
                    label.setWrapText(true);

                    VBox card = new VBox(label);

                    card.setPadding(new Insets(12));

                    card.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-border-color: #DCE3EC;" +
                                    "-fx-border-radius: 10;"
                    );

                    setGraphic(card);
                    setText(null);
                }
            }
        });

        Runnable refreshEvents = () -> {

            String keyword = search.getText()
                    .trim().toLowerCase();

            String selectedFilter = filter.getValue();

            list.getItems().clear();

            for (Event event : events) {

                String status =
                        event.students.size() >= event.capacity
                                ? "FULL"
                                : "AVAILABLE";

                boolean matchesSearch =
                        event.name.toLowerCase().contains(keyword) ||
                                event.date.toLowerCase().contains(keyword) ||
                                event.venue.toLowerCase().contains(keyword);

                boolean matchesFilter =
                        selectedFilter.equals("All Events") ||
                                (selectedFilter.equals("Available")
                                        && status.equals("AVAILABLE")) ||
                                (selectedFilter.equals("Full")
                                        && status.equals("FULL"));

                if (matchesSearch && matchesFilter) {

                    String details =
                            "Event: " + event.name +
                                    "\nDate: " + event.date +
                                    "\nVenue: " + event.venue +
                                    "\nCapacity: " + event.capacity +
                                    "\nRegistered: " + event.students.size() +
                                    "\nSeats Available: " +
                                    Math.max(0, event.capacity -
                                            event.students.size()) +
                                    "\nStatus: " + status;

                    list.getItems().add(details);
                }
            }
        };

        search.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        refreshEvents.run()
        );

        filter.setOnAction(
                e -> refreshEvents.run()
        );

        refreshEvents.run();

        list.setPlaceholder(
                new Label("No matching events found.")
        );

        VBox box = new VBox(
                15,
                heading,
                new Label("Search Events"),
                search,
                new Label("Filter by Availability"),
                filter,
                new Label("Event List"),
                list
        );

        box.setPadding(new Insets(25));

        box.setStyle(
                "-fx-background-color: #F3F6FA;"
        );

        return box;
    }

    // =========================================
    // STUDENT REGISTRATION
    // =========================================

    private VBox createRegistrationScreen() {

        Label heading = new Label("Student Registration");

        heading.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        registrationEvent.setPromptText("Select an event");

        TextField id = new TextField();
        id.setPromptText("Enter student ID");

        TextField name = new TextField();
        name.setPromptText("Enter student name");

        Button register = new Button("Register Student");

        register.setOnAction(e -> {

            String studentId = id.getText().trim();
            String studentName = name.getText().trim();

            Event selected = registrationEvent.getValue();

            if (studentId.isEmpty() ||
                    studentName.isEmpty() ||
                    selected == null) {
                showMessage("Please fill all details.");
                return;
            }

            if (studentId.contains(",") ||
                    studentId.contains("\n") ||
                    studentName.contains("\n") ||
                    studentName.contains(",")) {
                showMessage("Please enter valid student details.");
                return;
            }

            for (Student student : selected.students) {

                if (student.id.equalsIgnoreCase(studentId)) {
                    showMessage(
                            "Student already registered for this event."
                    );
                    return;
                }
            }

            if (selected.students.size() >= selected.capacity) {
                showMessage("Sorry! This event is full.");
                return;
            }

            selected.students.add(
                    new Student(studentId, studentName)
            );

            saveData();

            logActivity(
                    "STUDENT REGISTERED",
                    studentName + " registered for " + selected.name
            );

            refreshLists();

            registrationEvent.setValue(selected);

            id.clear();
            name.clear();

            showMessage("Student registered successfully!");
        });

        VBox box = new VBox(
                15,
                heading,
                new Label("Select Event"),
                registrationEvent,
                new Label("Student ID"),
                id,
                new Label("Student Name"),
                name,
                register
        );

        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: #F3F6FA;");

        return box;
    }

    // =========================================
    // PARTICIPANTS
    // =========================================

    private VBox createParticipantsScreen() {

        if (!requireAdmin()) return new VBox();

        Label heading = new Label("Registered Participants");

        heading.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        participantEvent.setPromptText("Select an event");

        participantList.setPrefHeight(400);

        Button view = new Button("View Participants");

        Button export = new Button(
                "Export Participants to CSV"
        );

        view.setOnAction(e -> displayParticipants());

        participantEvent.setOnAction(
                e -> displayParticipants()
        );

        export.setOnAction(e ->
                exportParticipantsToCSV(
                        participantEvent.getValue()
                )
        );

        VBox box = new VBox(
                15,
                heading,
                new Label("Select Event"),
                participantEvent,
                new HBox(10, view, export),
                new Label("Participant List"),
                participantList
        );

        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: #F3F6FA;");

        return box;
    }

    private void displayParticipants() {

        participantList.getItems().clear();

        Event selected = participantEvent.getValue();

        if (selected == null) return;

        if (selected.students.isEmpty()) {

            participantList.getItems().add(
                    "No students registered yet."
            );

            return;
        }

        int number = 1;

        for (Student student : selected.students) {
            participantList.getItems().add(
                    number++ + ". " + student
            );
        }
    }

    // =========================================
    // CSV EXPORT
    // =========================================

    private String csvValue(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void exportParticipantsToCSV(Event event) {

        if (!requireAdmin()) return;

        if (event == null) {
            showMessage("Please select an event.");
            return;
        }

        StringBuilder csv = new StringBuilder();

        csv.append("Student Name,Student ID\n");

        for (Student student : event.students) {

            csv.append(csvValue(student.name))
                    .append(",")
                    .append(csvValue(student.id))
                    .append("\n");
        }

        try {

            Files.createDirectories(Paths.get("data"));

            String fileName = event.name.replaceAll(
                    "[^a-zA-Z0-9-_]", "_"
            );

            Path file = Paths.get(
                    "data",
                    fileName + "_participants.csv"
            );

            Files.write(
                    file,
                    csv.toString().getBytes(StandardCharsets.UTF_8)
            );

            logActivity("CSV EXPORTED", event.name);

            showMessage(
                    "CSV exported successfully!\n" +
                            file.toAbsolutePath()
            );

        } catch (IOException e) {
            showError("CSV export failed: " + e.getMessage());
        }
    }

    // =========================================
    // REPORT GENERATION
    // =========================================

    private String generateReport() {

        StringBuilder report = new StringBuilder();

        report.append("CAMPUS EVENT MANAGEMENT REPORT\n");
        report.append("====================================\n\n");

        report.append("Total Events: ")
                .append(events.size())
                .append("\n\n");

        int totalStudents = 0;
        int totalAvailableSeats = 0;
        int totalFullEvents = 0;

        for (Event event : events) {

            int registered = event.students.size();

            int available = Math.max(
                    0, event.capacity - registered
            );

            totalStudents += registered;
            totalAvailableSeats += available;

            if (registered >= event.capacity) {
                totalFullEvents++;
            }

            report.append("Event Name: ")
                    .append(event.name).append("\n");

            report.append("Date: ")
                    .append(event.date).append("\n");

            report.append("Venue: ")
                    .append(event.venue).append("\n");

            report.append("Capacity: ")
                    .append(event.capacity).append("\n");

            report.append("Registered: ")
                    .append(registered).append("\n");

            report.append("Available Seats: ")
                    .append(available).append("\n");

            report.append("\nPARTICIPANTS:\n");

            if (event.students.isEmpty()) {

                report.append(
                        "No participants registered.\n"
                );

            } else {

                int number = 1;

                for (Student student : event.students) {
                    report.append(number++)
                            .append(". ")
                            .append(student.name)
                            .append(" - ")
                            .append(student.id)
                            .append("\n");
                }
            }

            report.append(
                    "------------------------------------\n\n"
            );
        }

        report.append("Total Registrations: ")
                .append(totalStudents).append("\n");

        report.append("Total Available Seats: ")
                .append(totalAvailableSeats).append("\n");

        report.append("Total Full Events: ")
                .append(totalFullEvents).append("\n");

        return report.toString();
    }

    // =========================================
    // REPORT SCREEN
    // =========================================

    private VBox createReportsScreen() {

        if (!requireAdmin()) return new VBox();

        Label heading = new Label("Reports and Summary");

        heading.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        TextArea reportArea = new TextArea();

        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefHeight(450);

        Button generate = new Button("Generate Report");
        Button save = new Button("Save Report");

        generate.setOnAction(e -> {

            if (!requireAdmin()) return;

            reportArea.setText(generateReport());

            logActivity(
                    "REPORT GENERATED",
                    "Event summary report generated"
            );
        });

        save.setOnAction(e -> {

            if (!requireAdmin()) return;

            try {

                Files.createDirectories(Paths.get("data"));

                Files.write(
                        Paths.get(REPORT_FILE),
                        generateReport().getBytes(StandardCharsets.UTF_8)
                );

                logActivity("REPORT SAVED", REPORT_FILE);

                showMessage("Report saved successfully!");

            } catch (IOException ex) {
                showError("Error saving report: " + ex.getMessage());
            }
        });

        VBox box = new VBox(
                15,
                heading,
                new HBox(10, generate, save),
                reportArea
        );

        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: #F3F6FA;");

        return box;
    }

    // =========================================
    // ACTIVITY TIMELINE
    // =========================================

    private VBox createActivityScreen() {

        if (!requireAdmin()) return new VBox();

        Label heading = new Label("Recent Activity Timeline");

        heading.setStyle(
                "-fx-font-size: 25px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #17365D;"
        );

        Label description = new Label(
                "Track all important application activities."
        );

        Button refresh = new Button("Refresh Activities");

        Label totalLabel = new Label(
                "Total Activities: " + activities.size()
        );

        refresh.setOnAction(e -> {

            refreshActivityList();

            totalLabel.setText(
                    "Total Activities: " + activities.size()
            );
        });

        activityList.setItems(
                FXCollections.observableArrayList(
                        activities.stream()
                                .map(Activity::toString)
                                .collect(Collectors.toList())
                )
        );

        activityList.setPrefHeight(450);

        activityList.setCellFactory(list -> new ListCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);

                if (empty || item == null) {

                    setText(null);
                    setGraphic(null);

                } else {

                    Label content = new Label(item);
                    content.setWrapText(true);

                    VBox card = new VBox(content);

                    card.setPadding(new Insets(12));

                    card.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-background-radius: 9;" +
                                    "-fx-border-color: #DCE3EC;" +
                                    "-fx-border-radius: 9;"
                    );

                    setGraphic(card);
                    setText(null);
                }
            }
        });

        activityList.setPlaceholder(
                new Label("No activities recorded yet.")
        );

        VBox box = new VBox(
                15,
                heading,
                description,
                totalLabel,
                refresh,
                activityList
        );

        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: #F3F6FA;");

        return box;
    }

    // =========================================
    // LOGIN SCREEN
    // =========================================

    private void showLoginScreen() {

        Label title = new Label("CAMPUS EVENT MANAGER");

        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;"
        );

        Label subtitle = new Label(
                "Secure Login and Role-Based Access"
        );

        subtitle.setStyle("-fx-text-fill: #D5DFEF;");

        VBox header = new VBox(8, title, subtitle);

        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(30));

        header.setStyle("-fx-background-color: #172B4D;");

        Label loginTitle = new Label("Welcome Back!");

        loginTitle.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #17365D;"
        );

        TextField username = new TextField();
        username.setPromptText("Enter username");
        username.setMaxWidth(300);

        PasswordField password = new PasswordField();
        password.setPromptText("Enter password");
        password.setMaxWidth(300);

        Label error = new Label();

        error.setStyle(
                "-fx-text-fill: #C0392B;" +
                        "-fx-font-weight: bold;"
        );

        Button login = new Button("Login");

        login.setDefaultButton(true);
        login.setPrefWidth(300);
        login.setPrefHeight(40);

        login.setStyle(
                "-fx-background-color: #245B91;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;"
        );

        login.setOnAction(e -> {

            String user = username.getText().trim();
            String pass = password.getText();

            if (user.equals("admin") &&
                    pass.equals("admin123")) {

                currentUsername = "admin";
                currentRole = "ADMIN";

                logActivity("LOGIN", "Admin logged in");

                showMainApplication();

            } else if (user.equals("student") &&
                    pass.equals("student123")) {

                currentUsername = "student";
                currentRole = "STUDENT";

                logActivity("LOGIN", "Student logged in");

                showMainApplication();

            } else {

                error.setText(
                        "Invalid username or password."
                );

                password.clear();
            }
        });

        Label demoTitle = new Label("Demo Login Credentials");
        demoTitle.setStyle("-fx-font-weight: bold;");

        Label adminCredentials = new Label(
                "Admin: admin / admin123"
        );

        Label studentCredentials = new Label(
                "Student: student / student123"
        );

        VBox demoBox = new VBox(
                7,
                demoTitle,
                adminCredentials,
                studentCredentials
        );

        demoBox.setAlignment(Pos.CENTER);
        demoBox.setPadding(new Insets(15));

        demoBox.setStyle(
                "-fx-background-color: #EAF1FA;" +
                        "-fx-background-radius: 10;"
        );

        VBox loginCard = new VBox(
                18,
                loginTitle,
                new Label("Username"),
                username,
                new Label("Password"),
                password,
                error,
                login,
                demoBox
        );

        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(35));
        loginCard.setMaxWidth(400);

        loginCard.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;"
        );

        VBox center = new VBox(loginCard);

        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(30));

        center.setStyle("-fx-background-color: #F3F6FA;");

        BorderPane root = new BorderPane();

        root.setTop(header);
        root.setCenter(center);

        Scene scene = new Scene(root, 1000, 700);

        primaryStage.setTitle("Campus Event Manager - Login");
        primaryStage.setScene(scene);

        primaryStage.setMinWidth(750);
        primaryStage.setMinHeight(550);

        primaryStage.show();
    }

    // =========================================
    // NAVIGATION BUTTON
    // =========================================

    private Button createNavButton(String text) {

        Button button = new Button(text);

        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(45);

        button.setAlignment(Pos.CENTER_LEFT);

        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 15 10 15;"
        );

        return button;
    }

    // =========================================
    // SCROLLABLE SCREEN
    // =========================================

    private ScrollPane createScrollableScreen(Node screen) {

        ScrollPane scrollPane = new ScrollPane();

        scrollPane.setContent(screen);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);

        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollPane.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        scrollPane.setPannable(true);

        scrollPane.setStyle(
                "-fx-background: #F3F6FA;" +
                        "-fx-background-color: #F3F6FA;"
        );

        return scrollPane;
    }

    // =========================================
    // SCREEN NAVIGATION
    // =========================================

    private void showScreen(
            BorderPane root,
            Button active,
            Button[] buttons,
            Node screen) {

        root.setCenter(createScrollableScreen(screen));

        for (Button button : buttons) {

            button.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 14px;" +
                            "-fx-padding: 10 15 10 15;" +
                            "-fx-alignment: CENTER_LEFT;"
            );
        }

        active.setStyle(
                "-fx-background-color: #416B9D;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 15 10 15;" +
                        "-fx-background-radius: 8;" +
                        "-fx-alignment: CENTER_LEFT;"
        );
    }

    // =========================================
    // LOGOUT
    // =========================================

    private void logout() {

        String username = currentUsername;

        logActivity(
                "LOGOUT",
                username + " logged out"
        );

        currentUsername = null;
        currentRole = null;

        totalEventsLabel = null;
        totalStudentsLabel = null;
        availableSeatsLabel = null;
        fullEventsLabel = null;

        showLoginScreen();
    }

    // =========================================
    // MAIN APPLICATION WINDOW
    // =========================================

    private void showMainApplication() {

        totalEventsLabel = null;
        totalStudentsLabel = null;
        availableSeatsLabel = null;
        fullEventsLabel = null;

        Label title = new Label("CAMPUS EVENT MANAGER");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;"
        );

        Label subtitle = new Label(
                "Student and Event Management System"
        );

        subtitle.setStyle("-fx-text-fill: #D5DFEF;");

        Label userInfo = new Label(
                "Logged in: " + currentUsername +
                        " | " + currentRole
        );

        userInfo.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        VBox titleBox = new VBox(5, title, subtitle, userInfo);

        Button logoutButton = new Button("Logout");

        logoutButton.setStyle(
                "-fx-background-color: #C0392B;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20 10 20;"
        );

        logoutButton.setOnAction(e -> logout());

        HBox header = new HBox(titleBox, logoutButton);

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18));

        header.setStyle("-fx-background-color: #172B4D;");

        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button dashboardButton = createNavButton("Dashboard");
        Button eventButton = createNavButton("Events");
        Button registrationButton = createNavButton("Registration");
        Button participantButton = createNavButton("Participants");
        Button reportButton = createNavButton("Reports");
        Button activityButton = createNavButton("Activity Timeline");

        List<Button> navButtons = new ArrayList<>();

        navButtons.add(dashboardButton);
        navButtons.add(eventButton);
        navButtons.add(registrationButton);

        if (isAdmin()) {
            navButtons.add(participantButton);
            navButtons.add(reportButton);
            navButtons.add(activityButton);
        }

        Button[] buttons = navButtons.toArray(new Button[0]);

        Label menuTitle = new Label("MAIN MENU");

        menuTitle.setStyle(
                "-fx-text-fill: #AFC1D8;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
        );

        VBox sidebar = new VBox(
                12,
                menuTitle,
                dashboardButton,
                eventButton,
                registrationButton
        );

        if (isAdmin()) {

            sidebar.getChildren().addAll(
                    participantButton,
                    reportButton,
                    activityButton
            );

        } else {

            Label studentInfo = new Label(
                    "Student Access\nView events and register."
            );

            studentInfo.setWrapText(true);

            studentInfo.setStyle(
                    "-fx-text-fill: #D5DFEF;" +
                            "-fx-font-size: 12px;"
            );

            sidebar.getChildren().add(studentInfo);
        }

        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);

        sidebar.setStyle("-fx-background-color: #203858;");

        BorderPane root = new BorderPane();

        root.setTop(header);
        root.setLeft(sidebar);

        root.setStyle("-fx-background-color: #F3F6FA;");

        // DASHBOARD NAVIGATION

        dashboardButton.setOnAction(e -> {

            refreshLists();

            showScreen(
                    root,
                    dashboardButton,
                    buttons,
                    createDashboardScreen()
            );
        });

        // EVENTS NAVIGATION

        eventButton.setOnAction(e -> {

            refreshLists();

            Node screen;

            if (isAdmin()) {
                screen = createEventScreen();
            } else {
                screen = createStudentEventsScreen();
            }

            showScreen(
                    root,
                    eventButton,
                    buttons,
                    screen
            );
        });

        // REGISTRATION NAVIGATION

        registrationButton.setOnAction(e -> {

            refreshLists();

            showScreen(
                    root,
                    registrationButton,
                    buttons,
                    createRegistrationScreen()
            );
        });

        // ADMIN NAVIGATION

        if (isAdmin()) {

            participantButton.setOnAction(e -> {

                refreshLists();

                showScreen(
                        root,
                        participantButton,
                        buttons,
                        createParticipantsScreen()
                );
            });

            reportButton.setOnAction(e -> {

                refreshLists();

                showScreen(
                        root,
                        reportButton,
                        buttons,
                        createReportsScreen()
                );
            });

            activityButton.setOnAction(e -> {

                refreshLists();

                showScreen(
                        root,
                        activityButton,
                        buttons,
                        createActivityScreen()
                );
            });
        }

        // INITIAL DASHBOARD

        refreshLists();

        showScreen(
                root,
                dashboardButton,
                buttons,
                createDashboardScreen()
        );

        Scene scene = new Scene(root, 1100, 750);

        primaryStage.setTitle(
                "Campus Event Manager - " + currentRole
        );

        primaryStage.setScene(scene);

        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(600);

        primaryStage.show();
    }

    // =========================================
    // APPLICATION START
    // =========================================

    @Override
    public void start(Stage stage) {

        primaryStage = stage;

        loadData();
        loadActivities();

        showLoginScreen();
    }

    // =========================================
    // MAIN METHOD
    // =========================================

    public static void main(String[] args) {
        launch(args);
    }
}