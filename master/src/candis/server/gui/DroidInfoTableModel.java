package candis.server.gui;

import candis.distributed.droid.StaticProfile;
import candis.server.DroidData;
import candis.server.DroidManager;
import candis.server.DroidManagerEvent;
import candis.server.DroidManagerListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Enrico Joerns
 */
class DroidInfoTableModel extends AbstractTableModel {

	private static final int POS_ID = 0;
	private static final int POS_MODEL = 1;
	private static final int POS_CPU = 2;
	private static final int POS_MEM = 3;
	private boolean DEBUG = true;
//	private String[] columnNames = {"", "Device ID", "Model"};
//	private List<TableData> mTableDataList = new LinkedList();
	private String[][] data = {
		{"ID: ", ""},
		{"Model: ", ""},
		{"CPUs: ", ""},
		{"Memory: ", ""}
	};

	private class TableData {

		TableData(String droidID, ImageIcon icon, String deviceID, String deviceModel) {
			this.droidID = droidID;
			this.icon = icon;
			this.deviceID = deviceID;
			this.deviceModel = deviceModel;
		}
		public String droidID;
		public ImageIcon icon;
		public String deviceID;
		public String deviceModel;
	}

	@Override
	public int getColumnCount() {
		return data[0].length;
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public String getColumnName(int col) {
		return "";
	}

	@Override
	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	/*
	 * JTable uses this method to determine the default renderer/ editor for
	 * each cell. If we didn't implement this method, then the last column
	 * would contain text ("true"/"false"), rather than a check box.
	 */
	@Override
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/*
	 * Don't need to implement this method unless your table's editable.
	 */
	@Override
	public boolean isCellEditable(int row, int col) {
		//Note that the data/cell address is constant,
		//no matter where the cell appears onscreen.
		if (col < 2) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * Don't need to implement this method unless your table's data can
	 * change.
	 */
	@Override
	public void setValueAt(Object value, int row, int col) {
		if (DEBUG) {
			System.out.println("Setting value at " + row + "," + col
							+ " to " + value + " (an instance of "
							+ value.getClass() + ")");
		}

//		data[row][col] = value;
		fireTableCellUpdated(row, col);
		fireTableDataChanged();// DEBUG

//		if (DEBUG) {
		System.out.println("New value of data:");
		printDebugData();
//		}
	}

	private void printDebugData() {
		int numRows = getRowCount();
		int numCols = getColumnCount();

		for (int i = 0; i < numRows; i++) {
			System.out.print("    row " + i + ":");
			for (int j = 0; j < numCols; j++) {
				System.out.print("  " + data[i]);
			}
			System.out.println();
		}
		System.out.println("--------------------------");
	}

	public void update(DroidManager droidmanager, String id) {
		DroidData d = (DroidData) droidmanager.getKnownDroids().get(id);
		data[POS_ID][1] = id;
		data[POS_MODEL][1] = d.getProfile().model;
		data[POS_CPU][1] = String.valueOf(d.getProfile().processors);
		data[POS_MEM][1] = String.valueOf(d.getProfile().memoryMB);
		
		fireTableDataChanged();
	}
}
