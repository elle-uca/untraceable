package org.surino.untraceable.view;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.surino.untraceable.controller.PersonRepository;
import org.surino.untraceable.model.Person;
import org.surino.untraceable.model.Status;

@SuppressWarnings("serial")
public class PersonTableModel extends AbstractTableModel{
	
	private final PersonRepository personRepository;

	public PersonTableModel(PersonRepository personRepository) {
		 this.personRepository = personRepository;
	}

	private final String[] columnNames = {"ID", "Name", "Surname", "Address", "Status"};
	private List<Person> data;

	public void setData(List<Person> people) {
		this.data = people;
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


