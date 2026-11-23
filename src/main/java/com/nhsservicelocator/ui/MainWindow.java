
package com.nhsservicelocator.ui;

import com.nhsservicelocator.data.HealthcareDataLoader;
import com.nhsservicelocator.data.PostcodeDataLoader;
import com.nhsservicelocator.model.SearchResult;
import com.nhsservicelocator.service.HealthcareSearchService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public final class MainWindow extends JFrame {

    private static final Path HEALTHCARE_FILE =
            Path.of("data", "healthcare_services_2026.csv");

    private static final Path POSTCODE_FILE =
            Path.of("data", "uk_postcode_coordinates_2026.csv");

    private final JTextField postcodeField = new JTextField(12);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> typeSelector = new JComboBox<>();
    private final JComboBox<String> radiusSelector =
            new JComboBox<>(new String[]{"5", "10", "25", "50", "100", "250"});

    private final JButton searchButton = new JButton("Search");
    private final JLabel statusLabel = new JLabel("Loading datasets...");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{
                    "Name", "Service Type", "Distance (km)",
                    "Postcode", "Address", "Phone"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 2 ? Double.class : String.class;
        }
    };

    private final JTable resultsTable = new JTable(tableModel);

    private HealthcareSearchService searchService;
    private List<SearchResult> currentResults = List.of();

    public MainWindow() {
        super("NHS Service Locator");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(850, 500));
        setLocationRelativeTo(null);

        initializeInterface();
        loadDatasets();
    }

    private void initializeInterface() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel("Healthcare Service Locator");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));

        JPanel header = new JPanel(new BorderLayout());
        header.add(heading, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));

        filters.add(new JLabel("Postcode:"));
        filters.add(postcodeField);

        filters.add(new JLabel("Service:"));
        typeSelector.setPreferredSize(new Dimension(200, 28));
        filters.add(typeSelector);

        filters.add(new JLabel("Name:"));
        filters.add(searchField);

        filters.add(new JLabel("Radius (km):"));
        radiusSelector.setSelectedItem("25");
        filters.add(radiusSelector);

        filters.add(searchButton);

        header.add(filters, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        resultsTable.setRowHeight(28);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.getTableHeader().setReorderingAllowed(false);

        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        resultsTable.getColumnModel().getColumn(2).setCellRenderer(
                new DefaultTableCellRenderer() {
                    @Override
                    protected void setValue(Object value) {
                        super.setValue(value == null ? "" : String.format("%.2f", value));
                    }
                });
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(300);
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        root.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(statusLabel, BorderLayout.WEST);

        JButton detailsButton = new JButton("View Details");
        footer.add(detailsButton, BorderLayout.EAST);

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);

        searchButton.setEnabled(false);
        searchButton.addActionListener(event -> search());
        postcodeField.addActionListener(event -> search());
        searchField.addActionListener(event -> search());
        detailsButton.addActionListener(event -> showSelectedService());

        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showSelectedService();
                }
            }
        });
    }

    private void loadDatasets() {
        SwingWorker<HealthcareSearchService, Void> worker =
                new SwingWorker<>() {

            @Override
            protected HealthcareSearchService doInBackground()
                    throws Exception {

                var healthcareLoader = new HealthcareDataLoader();
                var postcodeLoader = new PostcodeDataLoader();

                var services = healthcareLoader.load(HEALTHCARE_FILE);
                var coordinates = postcodeLoader.load(POSTCODE_FILE);

                return new HealthcareSearchService(services, coordinates);
            }

            @Override
            protected void done() {
                try {
                    searchService = get();

                    for (String type : searchService.getServiceTypes()) {
                        typeSelector.addItem(type);
                    }

                    searchButton.setEnabled(true);

                    statusLabel.setText(String.format(
                            "Loaded %,d services and %,d postcodes",
                            searchService.getServiceCount(),
                            searchService.getPostcodeCount()
                    ));

                } catch (Exception exception) {
                    statusLabel.setText("Failed to load datasets");

                    Throwable cause = exception.getCause() != null
                            ? exception.getCause()
                            : exception;

                    JOptionPane.showMessageDialog(
                            MainWindow.this,
                            cause.getMessage(),
                            "Data Loading Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private void search() {
        if (!searchButton.isEnabled()) {
            return;
        }

        String postcode = postcodeField.getText().trim();

        if (postcode.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a UK postcode.",
                    "Missing Postcode",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String serviceType = (String) typeSelector.getSelectedItem();
        String searchTerm = searchField.getText();
        double radius = Double.parseDouble(
                (String) radiusSelector.getSelectedItem());

        searchButton.setEnabled(false);
        statusLabel.setText("Searching...");

        SwingWorker<List<SearchResult>, Void> worker =
                new SwingWorker<>() {

            @Override
            protected List<SearchResult> doInBackground() {
                return searchService.search(
                        postcode,
                        serviceType,
                        searchTerm,
                        radius,
                        500
                );
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);

                try {
                    currentResults = List.copyOf(get());
                    tableModel.setRowCount(0);

                    for (SearchResult result : currentResults) {
                        var service = result.service();

                        tableModel.addRow(new Object[]{
                                service.name(),
                                service.serviceType().replace("|", ", "),
                                result.distanceKm(),
                                service.postcode(),
                                service.address(),
                                service.phone()
                        });
                    }

                    statusLabel.setText(String.format(
                            "%,d results found within %.0f km (maximum 500 displayed)",
                            currentResults.size(),
                            radius
                    ));

                } catch (Exception exception) {
                    Throwable cause = exception.getCause() != null
                            ? exception.getCause()
                            : exception;

                    statusLabel.setText("Search failed");

                    JOptionPane.showMessageDialog(
                            MainWindow.this,
                            cause.getMessage(),
                            "Search Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private void showSelectedService() {
        int selectedRow = resultsTable.getSelectedRow();

        if (selectedRow < 0 || currentResults.isEmpty()) {
            return;
        }

        int modelRow = resultsTable.convertRowIndexToModel(selectedRow);
        SearchResult result = currentResults.get(modelRow);
        var service = result.service();

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        details.setText(
                "Name: " + service.name()
                        + "\n\nType: " + service.serviceType()
                        + "\n\nDistance: " + String.format(
                                "%.2f km", result.distanceKm())
                        + "\n\nAddress: " + service.address()
                        + "\n\nPostcode: " + service.postcode()
                        + "\n\nPhone: " + service.phone()
                        + "\n\nProvider: " + service.provider()
                        + "\n\nWebsite: " + service.website()
                        + "\n\nCQC Record: " + service.cqcUrl()
        );

        JScrollPane detailsPane = new JScrollPane(details);
        detailsPane.setPreferredSize(new Dimension(500, 320));

        Object[] options = {"Close", "Open CQC Record"};

        int selection = JOptionPane.showOptionDialog(
                this,
                detailsPane,
                service.name(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selection == 1 && !service.cqcUrl().isBlank()) {
            try {
                URI uri = URI.create(service.cqcUrl());

                if (!uri.getScheme().equalsIgnoreCase("https")
                        || !uri.getHost().equalsIgnoreCase("www.cqc.org.uk")) {
                    throw new IllegalArgumentException("Invalid CQC URL");
                }

                Desktop.getDesktop().browse(uri);

            } catch (Exception exception) {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to open the CQC website.",
                        "Browser Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
