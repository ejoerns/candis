package candis.server.gui;

import candis.server.DroidData;
import candis.server.DroidManager;
import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Enrico Joerns
 */
class DroidInfoTableModel extends AbstractTableModel {

	private boolean DEBUG = true;
	private static final int POS_ID = 0;
	private static final int POS_DEV_ID = 1;
	private static final int POS_MODEL = 2;
	private static final int POS_CPU = 3;
	private static final int POS_MEM = 4;
	private static final int POS_BENCH = 5;
	private final String[][] data = {
		{"ID:        ", ""},
		{"Device ID: ", ""},
		{"Model:     ", ""},
		{"CPUs:      ", ""},
		{"Memory:    ", ""},
		{"Benchmark: ", ""}
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

	public void update(DroidManager droidmanager, String id) {
		DroidData ddata = (DroidData) droidmanager.getKnownDroids().get(id);
		if (ddata == null) {
			return;
		}
		synchronized (data) { // might be called from different threads
			data[POS_ID][1] = id;
			data[POS_DEV_ID][1] = ddata.getProfile().id;
			data[POS_MODEL][1] = ddata.getProfile().model;
			data[POS_CPU][1] = String.valueOf(ddata.getProfile().processors);
			data[POS_MEM][1] = String.format("%s Bytes", String.valueOf(ddata.getProfile().memoryMB));
			data[POS_BENCH][1] = String.valueOf(ddata.getProfile().benchmark);
		}
		fireTableDataChanged();
	}
}
