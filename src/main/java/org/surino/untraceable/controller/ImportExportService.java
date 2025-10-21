package org.surino.untraceable.controller;


import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.springframework.stereotype.Service;
import org.surino.untraceable.model.Person;
import org.surino.untraceable.model.Status;

@Service
public class ImportExportService {

    private final PersonRepository personRepository;

    public ImportExportService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    // 🔹 EXPORT
    public void exportToCSV(JFrame parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export People");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

                writer.write("ID;Name;Surname;Address;Status\n");
                for (Person p : personRepository.findAll()) {
                    writer.write(String.format("%s,%s,%s,%s\n",
                            p.getName(),
                            p.getSurname(),
                            p.getAddress() == null ? "" : p.getAddress(),
                            p.getStatus()));
                }

                JOptionPane.showMessageDialog(parent,
                        "Export completed:\n" + file.getAbsolutePath(),
                        "Export CSV",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent,
                        "Error during export: " + e.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 🔹 IMPORT
    public List<Person> importFromCSV(JFrame parent, String dialogTitle) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

        if (fileChooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return new ArrayList<>();
        }

        File file = fileChooser.getSelectedFile();
        List<Person> importedPeople = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = line.split(",", -1);
                if (parts.length < 4) continue;

                String name = parts[0].trim();
                String surname = parts[1].trim();
                String address = parts[2].trim();
                Status status;
                try {
                    status = Status.valueOf(parts[3].trim());
                } catch (Exception e) {
                    status = Status.SCONOSCIUTO;
                }

                importedPeople.add(new Person(name, surname, address, status));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "Error reading the CSV:\n" + e.getMessage(),
                    "Import Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        return importedPeople;
    }

    // 🔹 Import con progress bar e gestione duplicati avanzata
    public void importWithDuplicatesCheck(JFrame parent, List<Person> importedPeople) {
        if (importedPeople == null || importedPeople.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No valid records found in the CSV.");
            return;
        }

        JDialog progressDialog = createProgressDialog(parent, importedPeople.size());
        JProgressBar progressBar = (JProgressBar) progressDialog.getContentPane().getComponent(1);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                List<Person> existingPeople = personRepository.findAll();
                int imported = 0;
                int skipped = 0;
                int duplicateMode = -1; // -1 = chiedi ogni volta, 0 = importa tutti, 1 = salta tutti

                for (int i = 0; i < importedPeople.size(); i++) {
                    Person candidate = importedPeople.get(i);

                    List<Person> duplicates = existingPeople.stream()
                            .filter(p -> p.getName().equalsIgnoreCase(candidate.getName())
                                    && p.getSurname().equalsIgnoreCase(candidate.getSurname()))
                            .collect(Collectors.toList());

                    if (!duplicates.isEmpty()) {
                        if (duplicateMode == -1) {
                            Object[] options = {
                                    "Import anyway", 
                                    "Skip duplicate", 
                                    "Apply to all (Import all)", 
                                    "Apply to all (Skip all)"
                            };

                            String dupInfo = duplicates.stream()
                                    .map(p -> "- " + p.getName() + " " + p.getSurname() +
                                            (p.getAddress() != null && !p.getAddress().isEmpty()
                                                    ? " (" + p.getAddress() + ")"
                                                    : ""))
                                    .collect(Collectors.joining("\n"));

                            int choice = JOptionPane.showOptionDialog(
                                    parent,
                                    "Duplicate detected for:\n" +
                                            candidate.getName() + " " + candidate.getSurname() +
                                            "\n\nExisting entries:\n" + dupInfo,
                                    "Duplicate Found",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.WARNING_MESSAGE,
                                    null,
                                    options,
                                    options[0]
                            );

                            switch (choice) {
                                case 0 -> { /* Import only this one */ }
                                case 1 -> { skipped++; publish(i + 1); continue; }
                                case 2 -> duplicateMode = 0;
                                case 3 -> { duplicateMode = 1; skipped++; publish(i + 1); continue; }
                                default -> { skipped++; publish(i + 1); continue; }
                            }
                        } else if (duplicateMode == 1) {
                            skipped++;
                            publish(i + 1);
                            continue;
                        }
                    }

                    personRepository.save(candidate);
                    imported++;
                    publish(i + 1);
                }

                JOptionPane.showMessageDialog(parent,
                        "Import completed.\nImported: " + imported + "\nSkipped (duplicates): " + skipped,
                        "Import Result",
                        JOptionPane.INFORMATION_MESSAGE);
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latest = chunks.get(chunks.size() - 1);
                progressBar.setValue(latest);
            }

            @Override
            protected void done() {
                progressDialog.dispose();
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    // 🔹 Utility: crea finestra con progress bar
    private JDialog createProgressDialog(JFrame parent, int max) {
        JDialog dialog = new JDialog(parent, "Importing...", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 120);
        dialog.setLocationRelativeTo(parent);

        JLabel label = new JLabel("Importing people, please wait...");
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JProgressBar progressBar = new JProgressBar(0, max);
        progressBar.setStringPainted(true);

        dialog.add(label, BorderLayout.NORTH);
        dialog.add(progressBar, BorderLayout.CENTER);

        return dialog;
    }
}
