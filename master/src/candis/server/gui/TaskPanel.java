package candis.server.gui;

import candis.distributed.JobDistributionIOHandler;
import candis.server.JobDistributionIOServer;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * Holds available Tasks that can be selected.
 *
 * @author Enrico Joerns
 */
public class TaskPanel {

	private final JPanel mHolder;
	private final JobDistributionIOServer mJobDistIO;
	private final Map<String, TaskPanelElement> mTaskPanels = new HashMap<String, TaskPanelElement>();
	private final MouseAdapter mClickListener = new TaskElementClickListener();
//	private final MouseListener taskElementPopupCL = new TaskElementPopupClickListener();
	private String mSelectedTaskID;

	/**
	 * Creates new form TaskPanel.
	 */
	public TaskPanel(JPanel holder, JobDistributionIOServer jDistIO) {
		mHolder = holder;
		mJobDistIO = jDistIO;
		mJobDistIO.addHandler(new JobDistListener());
	}

	public void addTask(String id) {
		// add new task element
		TaskPanelElement tpe = new TaskPanelElement(id, mJobDistIO.getCDBLoader().getTaskName(id));
		if (mJobDistIO.getControl() == null) {
			System.out.println("mJobDistIO.getControl() null");
		}
		else {
			tpe.setParameterCounter(mJobDistIO.getControl().getParametersLeft());
		}
		mTaskPanels.put(id, tpe);
		tpe.addMouseListener(mClickListener);

		if (mSelectedTaskID == null) {
			mSelectedTaskID = id;
			tpe.setBorder(BorderFactory.createEtchedBorder(Color.CYAN, Color.CYAN.darker()));
		}

		// add to layout
		mHolder.add(tpe);
		mHolder.revalidate();
	}

	public void selectTask(String taskID) {
		mSelectedTaskID = taskID;
		for (Map.Entry<String, TaskPanelElement> tpe : mTaskPanels.entrySet()) {
			if (tpe.getValue().getID().equals(mSelectedTaskID)) {
				tpe.getValue().setBorder(BorderFactory.createEtchedBorder(Color.CYAN, Color.CYAN.darker()));
			}
			else {
				tpe.getValue().setBorder(BorderFactory.createEtchedBorder());
			}
		}
	}

	public String getSelectedTaskID() {
		return mSelectedTaskID;
	}

	/**
	 *
	 */
	private class TaskElementClickListener extends MouseAdapter {

		public TaskElementClickListener() {
		}

		@Override
		public void mousePressed(MouseEvent evt) {
			// context menu
			if (evt.isPopupTrigger()) {
				JPopupMenu popup = new JPopupMenu();
				popup.add(new JMenuItem(new DeleteAction(((TaskPanelElement) evt.getComponent()).getID())));
				popup.show(evt.getComponent(),
									 evt.getX(), evt.getY());
			}
		}

		@Override
		public void mouseReleased(MouseEvent evt) {
			// selection click
			if (evt.isPopupTrigger()) {
				JPopupMenu popup = new JPopupMenu();
				popup.show(evt.getComponent(),
									 evt.getX(), evt.getY());
			}
			else {
				// get id of selected task
				selectTask(((TaskPanelElement) evt.getComponent()).getID());
			}
		}
	}

	/**
	 *
	 */
	private class DeleteAction extends AbstractAction {

		private String mID;

		public DeleteAction(String id) {
			super("Delete");
			mID = id;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {

//			JOptionPane.showMessageDialog(mHolder, "clicked: " + evt.getActionCommand() + " from ID " + mID);
			mHolder.remove(mTaskPanels.get(mID));
			mTaskPanels.remove(mID);
			mHolder.revalidate();
		}

		@Override
		public boolean isEnabled() {
			return true;
		}
	}

	private class JobDistListener implements JobDistributionIOHandler {

		@Override
		public void onEvent(Event event) {
			switch (event) {
				case JOB_DONE:
//					mTaskPanels.get(mJobDistIO.getCurrentTaskID()).setResultCounter(mJobDistIO.getCurrentScheduler().getResults().size());
					break;
				case JOB_SENT:
					mTaskPanels.get(mJobDistIO.getCurrentTaskID()).setParameterCounter(mJobDistIO.getControl().getParametersLeft());
					break;
				case SCHEDULER_DONE:
					break;
			}
		}
	}
}
