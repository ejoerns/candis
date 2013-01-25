package candis.server.gui;

import candis.distributed.parameter.IntegerUserParameter;
import candis.distributed.parameter.UserParameter;
import candis.distributed.parameter.UserParameterCtrl;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class ParameterContainer {

	public final UserParameter mUserParameter;
	public JComponent mJComponent;
	public final JLabel mNameLabel;
	//public final JLabel mDescriptionLabel;
	public final JLabel mErrorLabel;

	protected ParameterContainer(UserParameter mUserParameter) {
		this.mUserParameter = mUserParameter;
		this.mNameLabel = new JLabel(mUserParameter.getTitle());
		//this.mDescriptionLabel = new JLabel(mUserParameter.getDescription());
		this.mErrorLabel = new JLabel(" ");
	}

	public abstract Object getValue();

	public boolean validate() {
		mUserParameter.SetValue(this.getValue());
		boolean valid = mUserParameter.validate();
		mErrorLabel.setText(mUserParameter.getValidatorMessage() + " ");
		return valid;
	}

	public static ParameterContainer getContainer(UserParameter param) {
		UserParameterCtrl ctrl = param.getInputCtrl();

		switch (ctrl.getInputTupe()) {
			case FILE:
				return new FileParameterContainer(param);
			case INTEGER:
				return new IntegerParameterContainer(param);
			case STRING:
				return new StringParameterContainer(param);
			case STRING_LIST:
				return new StringListParameterContainer(param);
			case BOOELAN:
				return new BooleanParameterContainer(param);
		}
		return null;
	}

	public void addToDialog(JPanel panel, int position) {
		GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = position * 2;
		gridBagConstraints.anchor = gridBagConstraints.LINE_START;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		if (mUserParameter.getDescription().length() > 0) {
			mNameLabel.setToolTipText(mUserParameter.getDescription());
		}
		panel.add(mNameLabel, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = position * 2;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		if (mUserParameter.getDescription().length() > 0) {
			mJComponent.setToolTipText(mUserParameter.getDescription());
		}
		panel.add(mJComponent, gridBagConstraints);

		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = position * 2 + 1;
		gridBagConstraints.weightx = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
		gridBagConstraints.anchor = gridBagConstraints.LINE_START;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		if (mUserParameter.getDescription().length() > 0) {
			mErrorLabel.setToolTipText(mUserParameter.getDescription());
		}
		mErrorLabel.setForeground(Color.red);
		panel.add(mErrorLabel, gridBagConstraints);

	}

	public static class StringListParameterContainer extends ParameterContainer {

		public StringListParameterContainer(UserParameter userParameter) {
			super(userParameter);
			JComboBox box = new JComboBox();
			box.setModel(new javax.swing.DefaultComboBoxModel(userParameter.getInputCtrl().getListElements()));
			box.setSelectedItem(userParameter.getValue());
			box.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent ie) {
					validate();
				}
			});
			mJComponent = box;

		}

		@Override
		public Object getValue() {
			return ((JComboBox) mJComponent).getSelectedItem();
		}
	}

	public static class BooleanParameterContainer extends ParameterContainer {

		public BooleanParameterContainer(UserParameter userParameter) {
			super(userParameter);
			JCheckBox checkBox = new JCheckBox();
			checkBox.setSelected(Boolean.parseBoolean(userParameter.getValue().toString()));
			checkBox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent ce) {
					validate();
				}
			});
			mJComponent = checkBox;
		}

		@Override
		public Object getValue() {
			return ((JCheckBox) mJComponent).isSelected();
		}
	}

	public static class IntegerParameterContainer extends ParameterContainer {

		public IntegerParameterContainer(UserParameter userParameter) {
			super(userParameter);

			JSpinner spinner = new JSpinner();
			if (IntegerUserParameter.class.isInstance(userParameter)) {
				IntegerUserParameter integerParemeter = (IntegerUserParameter) userParameter;
				spinner.setModel(new SpinnerNumberModel(integerParemeter.getIngegerValue(),
																								integerParemeter.getMin(),
																								integerParemeter.getMax(),
																								integerParemeter.getStep()));
			}
			else {
				spinner.setModel(new SpinnerNumberModel());
			}
			spinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent ce) {
					validate();
				}
			});
			mJComponent = spinner;
		}

		@Override
		public Object getValue() {
			return ((JSpinner) mJComponent).getValue();
		}
	}

	public static class StringParameterContainer extends ParameterContainer {

		public StringParameterContainer(UserParameter userParameter) {
			super(userParameter);
			JTextField text = new JTextField(userParameter.getValue().toString());
			mJComponent = text;
			Dimension d = mJComponent.getMinimumSize();
			text.addVetoableChangeListener(null);
			text.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent de) {
					validate();
				}

				@Override
				public void removeUpdate(DocumentEvent de) {
					validate();
				}

				@Override
				public void changedUpdate(DocumentEvent de) {
					validate();
				}
			});
			d.width = 300;
			mJComponent.setPreferredSize(d);

		}

		@Override
		public Object getValue() {
			return ((JTextField) mJComponent).getText();
		}
	}

	public static class FileParameterContainer extends ParameterContainer {

		private final JLabel mFileLabel;

		public FileParameterContainer(UserParameter userParameter) {
			super(userParameter);

			JPanel panel = new JPanel();
			mFileLabel = new JLabel(userParameter.getValue().toString());

			panel.setLayout(new java.awt.GridBagLayout());
			GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
			gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
			gridBagConstraints.weightx = 1;
			panel.add(mFileLabel, gridBagConstraints);

			JButton fileButton = new JButton("Select File");
			gridBagConstraints.gridx = 1;
			gridBagConstraints.weightx = 0;
			panel.add(fileButton, gridBagConstraints);
			fileButton.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					JFileChooser fileChooser = new JFileChooser();
					int returnVal = fileChooser.showOpenDialog(mJComponent.getParent());

					if (returnVal == JFileChooser.APPROVE_OPTION) {

						mFileLabel.setText(fileChooser.getSelectedFile().getPath());
						validate();
					}
				}
			});
			mJComponent = panel;

		}

		@Override
		public Object getValue() {
			return mFileLabel.getText();
		}
	}
}
