package org.surino.untraceable.view;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import org.springframework.stereotype.Component;
import org.surino.untraceable.controller.repository.PersonRepository;
import org.surino.untraceable.model.Person;
import org.surino.untraceable.model.Status;

import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
@Component
public class PersonPanel extends JPanel {

    private final PersonRepository personRepository;

    private JTextField nomeField;
    private JTextField cognomeField;
    private JTextField indirizzoField;
    private JComboBox<Status> statusCombo;
    private JTextField ricercaField;

    private JTable table;
    private PersonTableModel tableModel;
    private List<Person> tuttePersone;

    public PersonPanel(PersonRepository personRepository) {
        this.personRepository = personRepository;
        initUI();
        caricaPersone();
    }

    private void initUI() {
        setLayout(new MigLayout("fillx, insets 10", "[grow, fill]", "[]10[]10[grow]"));

        // 🔹 PANNELLO FORM
        JPanel formPanel = new JPanel(new MigLayout("insets 0", "[][grow,fill]"));
        nomeField = new JTextField(15);
        cognomeField = new JTextField(15);
        indirizzoField = new JTextField(20);
        statusCombo = new JComboBox<>(Status.values());

        formPanel.add(new JLabel("Nome:"));
        formPanel.add(nomeField, "wrap");
        formPanel.add(new JLabel("Cognome:"));
        formPanel.add(cognomeField, "wrap");
        formPanel.add(new JLabel("Indirizzo:"));
        formPanel.add(indirizzoField, "wrap");
        formPanel.add(new JLabel("Status:"));
        formPanel.add(statusCombo, "wrap");

        // 🔹 PULSANTI
        JButton salvaButton = new JButton("💾 Salva");
        JButton eliminaButton = new JButton("🗑️ Elimina selezionato");
        JButton importaButton = new JButton("📥 Importa CSV");
        JButton esportaButton = new JButton("📤 Esporta CSV");

        salvaButton.addActionListener(e -> salvaPersona());
        eliminaButton.addActionListener(e -> eliminaPersona());
        importaButton.addActionListener(e -> importaCSV());
        esportaButton.addActionListener(e -> esportaCSV());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.add(salvaButton);
        buttonPanel.add(eliminaButton);
        buttonPanel.add(importaButton);
        buttonPanel.add(esportaButton);

        formPanel.add(buttonPanel, "span, growx, wrap");
        add(formPanel, "growx, wrap");

        // 🔹 RICERCA
        JPanel searchPanel = new JPanel(new MigLayout("insets 0", "[][grow,fill]"));
        ricercaField = new JTextField(20);
        searchPanel.add(new JLabel("🔍 Ricerca:"));
        searchPanel.add(ricercaField, "growx");
        add(searchPanel, "growx, wrap");

        ricercaField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtra(); }
            @Override public void removeUpdate(DocumentEvent e) { filtra(); }
            @Override public void changedUpdate(DocumentEvent e) { filtra(); }
        });

        // 🔹 TABELLA
        tableModel = new PersonTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JComboBox<Status> comboBoxEditor = new JComboBox<>(Status.values());
        table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(comboBoxEditor));

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, "grow, push");
    }

    private void salvaPersona() {
        String nome = nomeField.getText().trim();
        String cognome = cognomeField.getText().trim();
        String indirizzo = indirizzoField.getText().trim();
        Status status = (Status) statusCombo.getSelectedItem();

        if (nome.isEmpty() || cognome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome e Cognome sono obbligatori!");
            return;
        }

        personRepository.save(new Person(nome, cognome, indirizzo, status));
        nomeField.setText("");
        cognomeField.setText("");
        indirizzoField.setText("");
        statusCombo.setSelectedIndex(0);
        caricaPersone();
    }

    private void eliminaPersona() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleziona una persona da eliminare!");
            return;
        }

        Person p = tableModel.getPersonAt(row);
        int conferma = JOptionPane.showConfirmDialog(this,
                "Vuoi eliminare " + p.getName() + " " + p.getSurname() + "?",
                "Conferma eliminazione", JOptionPane.YES_NO_OPTION);

        if (conferma == JOptionPane.YES_OPTION) {
            personRepository.delete(p);
            caricaPersone();
        }
    }

    private void caricaPersone() {
        tuttePersone = personRepository.findAll();
        tableModel.setData(tuttePersone);
    }

    private void filtra() {
        if (tuttePersone == null) return;
        String filtro = ricercaField.getText().toLowerCase();

        if (filtro.isEmpty()) {
            tableModel.setData(tuttePersone);
        } else {
            List<Person> filtrate = tuttePersone.stream()
                    .filter(p -> p.getName().toLowerCase().contains(filtro)
                            || p.getSurname().toLowerCase().contains(filtro)
                            || (p.getAddress() != null && p.getAddress().toLowerCase().contains(filtro)))
                    .collect(Collectors.toList());
            tableModel.setData(filtrate);
        }
    }

    private void esportaCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Esporta Persone");
        fileChooser.setFileFilter(new FileNameExtensionFilter("File CSV", "csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

                writer.write("ID;Nome;Cognome;Indirizzo;Status\n");
                for (Person p : personRepository.findAll()) {
                    writer.write(String.format("%d;%s;%s;%s;%s\n",
                            p.getId() == null ? 0 : p.getId(),
                            p.getName(),
                            p.getSurname(),
                            p.getAddress() == null ? "" : p.getAddress(),
                            p.getStatus()));
                }

                JOptionPane.showMessageDialog(this, "Esportazione completata:\n" + file.getAbsolutePath());

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Errore durante l’esportazione: " + e.getMessage());
            }
        }
    }

    private void importaCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Importa Persone da CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("File CSV", "csv"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

                String line;
                boolean firstLine = true;
                DefaultListModel<Person> previewList = new DefaultListModel<>();

                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    String[] parts = line.split(";", -1);
                    if (parts.length < 5) continue;

                    String name = parts[1].trim();
                    String surname = parts[2].trim();
                    String address = parts[3].trim();
                    Status status;
                    try {
                        status = Status.valueOf(parts[4].trim());
                    } catch (Exception e) {
                        status = Status.SCONOSCIUTO; // fallback
                    }

                    previewList.addElement(new Person(name, surname, address, status));
                }

                if (previewList.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nessun record valido trovato nel CSV.");
                    return;
                }

                // Mostra anteprima prima di confermare
                mostraAnteprimaCSV(previewList, file.getName());

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Errore durante la lettura del CSV:\n" + e.getMessage());
            }
        }
    }
    private void mostraAnteprimaCSV(DefaultListModel<Person> previewList, String fileName) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Anteprima Importazione: " + fileName, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        // Tabella anteprima
        String[] columns = {"Nome", "Cognome", "Indirizzo", "Status"};
        Object[][] data = new Object[previewList.size()][columns.length];
        for (int i = 0; i < previewList.size(); i++) {
            Person p = previewList.get(i);
            data[i][0] = p.getName();
            data[i][1] = p.getSurname();
            data[i][2] = p.getAddress();
            data[i][3] = p.getStatus();
        }

        JTable table = new JTable(data, columns);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        // Pulsanti
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("✅ Importa");
        JButton cancelButton = new JButton("❌ Annulla");

        confirmButton.addActionListener(e -> {
            int imported = 0;
            for (int i = 0; i < previewList.size(); i++) {
                personRepository.save(previewList.get(i));
                imported++;
            }
            caricaPersone();
            JOptionPane.showMessageDialog(this, "Importate " + imported + " persone dal file CSV.");
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }


    // 🔹 MODELLO TABELLA
    private class PersonTableModel extends AbstractTableModel {
        private final String[] columnNames = {"ID", "Nome", "Cognome", "Indirizzo", "Status"};
        private List<Person> data;

        public void setData(List<Person> persone) {
            this.data = persone;
            fireTableDataChanged();
        }

        public Person getPersonAt(int row) {
            return data.get(row);
        }

        @Override public int getRowCount() { return data == null ? 0 : data.size(); }
        @Override public int getColumnCount() { return columnNames.length; }
        @Override public String getColumnName(int column) { return columnNames[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Person p = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> p.getId();
                case 1 -> p.getName();
                case 2 -> p.getSurname();
                case 3 -> p.getAddress();
                case 4 -> p.getStatus();
                default -> null;
            };
        }

        @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex > 0;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            Person p = data.get(rowIndex);
            switch (columnIndex) {
                case 1 -> p.setName((String) value);
                case 2 -> p.setSurname((String) value);
                case 3 -> p.setAddress((String) value);
                case 4 -> p.setStatus((Status) value);
            }
            personRepository.save(p);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> Long.class;
                case 4 -> Status.class;
                default -> String.class;
            };
        }
    }
}
