package candis.server.gui;

import candis.distributed.droid.StaticProfile;
import candis.server.Connection;
import candis.distributed.DroidData;
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
class DroidlistTableModel extends AbstractTableModel implements DroidManagerListener {

	private boolean DEBUG = true;
	private String[] columnNames = {"", "Device ID", "Model"};
	private List<TableData> mTableDataList = new LinkedList();

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
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return mTableDataList.size();
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public Object getValueAt(int row, int col) {
		TableData td = (TableData) mTableDataList.get(row);
		switch (col) {
			case 0:
				return td.icon;
			case 1:
				return td.deviceID;
			case 2:
				return td.deviceModel;
			default:
				return td.droidID;
		}
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
				System.out.print("  " + mTableDataList.get(i));
			}
			System.out.println();
		}
		System.out.println("--------------------------");
	}

	@Override
	public void handle(
					DroidManagerEvent event,
					Map<String, DroidData> knownDroids,
					Map<String, Connection> connectedDroids) {
		System.out.println("Droid handler in TableModel called with event: " + event);
		synchronized (mTableDataList) {
			mTableDataList.clear();
		}
		for (Map.Entry<String, DroidData> entry : knownDroids.entrySet()) {
			StaticProfile profile;
			ImageIcon icon;
			if (connectedDroids.containsKey(entry.getKey())) {
				icon = new ImageIcon("res/connected.png", "a pretty but meaningless splat");
			} else {
				if (entry.getValue().getBlacklist()) {
					icon = new ImageIcon("res/known.png", "a pretty but meaningless splat");
				} else {
					icon = new ImageIcon("res/blacklisted.png", "a pretty but meaningless splat");
				}
			}
			profile = knownDroids.get(entry.getKey()).getProfile();
			synchronized (mTableDataList) {
				mTableDataList.add(new TableData(
								entry.getKey(),
								icon,
								profile.id,
								profile.model));
			}
		}
		// update table
		fireTableDataChanged();
	}
}
