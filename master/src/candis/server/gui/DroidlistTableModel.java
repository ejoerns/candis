package candis.server.gui;

import candis.distributed.droid.StaticProfile;
import candis.server.DroidData;
import candis.server.DroidManager;
import candis.server.DroidManagerEvent;
import candis.server.DroidManagerListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
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
		if ((row > mTableDataList.size()) || (row < 0)) {
			return "";
		}
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

	@Override
	public void handle(
					DroidManagerEvent event,
					DroidManager manager) {
		System.out.println("Droid handler in TableModel called with event: " + event);
		synchronized (mTableDataList) {
			mTableDataList.clear();
		}
		for (Map.Entry<String, DroidData> entry : manager.getKnownDroids().entrySet()) {
			StaticProfile profile;
			ImageIcon icon;
			if (manager.getConnectedDroids().containsKey(entry.getKey())) {
				icon = new ImageIcon("res/connected.png", "a pretty but meaningless splat");
			} else {
				if (entry.getValue().getBlacklist()) {
					icon = new ImageIcon("res/blacklisted.png", "a pretty but meaningless splat");
				} else {
					icon = new ImageIcon("res/known.png", "a pretty but meaningless splat");
				}
			}
			profile = manager.getKnownDroids().get(entry.getKey()).getProfile();
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
