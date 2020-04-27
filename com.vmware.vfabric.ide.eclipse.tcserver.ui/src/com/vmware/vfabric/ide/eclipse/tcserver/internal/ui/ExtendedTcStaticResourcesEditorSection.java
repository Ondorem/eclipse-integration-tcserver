/*******************************************************************************
 * Copyright (c) 2012, 2020 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package com.vmware.vfabric.ide.eclipse.tcserver.internal.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jst.server.tomcat.core.internal.WebModule;
import org.eclipse.jst.server.tomcat.core.internal.command.ModifyWebModuleCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.vmware.vfabric.ide.eclipse.tcserver.internal.core.TcServer;

/**
 * Allows to configure filename patterns to control reloading of web
 * applications.
 * @author Christian Dupuis
 */
@SuppressWarnings("restriction")
public class ExtendedTcStaticResourcesEditorSection extends ServerEditorSection {

	class FilenameLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return WorkbenchImages.getImage(ISharedImages.IMG_OBJ_FILE);
		}

		@Override
		public String getText(Object element) {
			return element.toString();
		}

	}

	class StaticFilenamesContentProvider implements ITreeContentProvider {

		public void dispose() {
		}

		public Object[] getChildren(Object parentElement) {
			return getElements(parentElement);
		}

		public String[] getElements(Object inputElement) {
			if (inputElement instanceof IServer) {
				IServer server = (IServer) inputElement;
				TcServer tcServer = (TcServer) server.loadAdapter(TcServer.class, null);
				String[] filenames = tcServer.getStaticFilenamePatterns().split(",");
				return filenames;

			}
			return new String[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

	}

	protected TcServer serverWorkingCopy;

	private Table filenameTable;

	private TableViewer filenamesTableViewer;

	private Button addButton;

	private Button deleteButton;

	private Button upButton;

	private Button downButton;

	protected boolean updating;

	protected PropertyChangeListener listener;

	private StaticFilenamesContentProvider contentProvider;

	private Button enableButton;

	private Button enableAgentButton;

	private Text agentOptionsText;

	protected void addConfigurationChangeListener() {
		listener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (TcServer.PROPERTY_ENHANCED_REDEPLOY.equals(event.getPropertyName())) {
					updateEnablement();
				}
				else if (TcServer.PROPERTY_AGENT_REDEPLOY.equals(event.getPropertyName())) {
					updateEnablement();
				}
				else if (TcServer.PROPERTY_STATIC_FILENAMES.equals(event.getPropertyName())) {
					filenamesTableViewer.setInput(server);
				}
				else if (TcServer.PROPERTY_AGENT_OPTIONS.equals(event.getPropertyName())) {
					if (!updating) {
						updating = true;
						agentOptionsText.setText(serverWorkingCopy.getAgentOptions());
						agentOptionsText.setSelection(agentOptionsText.getText().length());
						updating = false;
					}
				}
			}

		};
		serverWorkingCopy.getServerWorkingCopy().addPropertyChangeListener(listener);
	}

	@Override
	public void createSection(Composite parent) {
		super.createSection(parent);
		FormToolkit toolkit = getFormToolkit(parent.getDisplay());

		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
				| ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE);
		section.setText("Application Reload Behavior");
		section.setDescription("Configure application reload behavior on changes to project resources.");

		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

		Composite composite = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 5;
		layout.marginWidth = 1;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 10;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		toolkit.paintBordersFor(composite);
		section.setClient(composite);

		enableAgentButton = toolkit.createButton(composite, "Enable Java Agent-based reloading (experimental)",
				SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(enableAgentButton);
		enableAgentButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (updating) {
					return;
				}
				try {
					updating = true;
					boolean enabled = enableAgentButton.getSelection();
					execute(new ModifyAgentRedeployCommand(serverWorkingCopy, enabled));
					try {
						List<?> list = serverWorkingCopy.getTomcatConfiguration().getWebModules();
						int i = 0;
						Iterator<?> iterator = list.iterator();
						while (iterator.hasNext()) {
							WebModule module = (WebModule) iterator.next();
							WebModule newModule = new WebModule(module.getPath(), module.getDocumentBase(), module
									.getMemento(), !enabled);
							execute(new ModifyWebModuleCommand(serverWorkingCopy.getTomcatConfiguration(), i, newModule));
							i++;
						}
					}
					catch (Exception e) {
						return;
					}
				}
				finally {
					updating = false;
				}
			}
		});

		Composite optionsComposite = new Composite(composite, SWT.NONE);
		GridLayout optionsLayout = new GridLayout(2, false);
		optionsLayout.marginWidth = 1;
		optionsLayout.marginHeight = 2;
		optionsComposite.setLayout(optionsLayout);
		GridDataFactory.fillDefaults().applyTo(optionsComposite);
		toolkit.paintBordersFor(optionsComposite);

		toolkit.createLabel(optionsComposite, "Options:");
		agentOptionsText = toolkit.createText(optionsComposite, "");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(agentOptionsText);
		agentOptionsText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				if (updating) {
					return;
				}
				try {
					updating = true;
					String value = agentOptionsText.getText();
					execute(new ModifyAgentOptionsCommand(serverWorkingCopy, value));
				}
				finally {
					updating = false;
				}
			}
		});

		enableButton = toolkit.createButton(composite, "Enable JMX-based reloading", SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(enableButton);
		enableButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (updating) {
					return;
				}
				try {
					updating = true;
					boolean enabled = enableButton.getSelection();
					execute(new ModifyEnhancedRedeployCommand(serverWorkingCopy, enabled));
					try {
						List<?> list = serverWorkingCopy.getTomcatConfiguration().getWebModules();
						int i = 0;
						Iterator<?> iterator = list.iterator();
						while (iterator.hasNext()) {
							WebModule module = (WebModule) iterator.next();
							WebModule newModule = new WebModule(module.getPath(), module.getDocumentBase(), module
									.getMemento(), !enabled);
							execute(new ModifyWebModuleCommand(serverWorkingCopy.getTomcatConfiguration(), i, newModule));
							i++;
						}
					}
					catch (Exception e) {
						return;
					}
				}
				finally {
					updating = false;
				}
			}
		});

		Label enhancedRedeploymentLabel = toolkit
				.createLabel(
						composite,
						"Define patterns for files that should be copied to deployed applications without triggereing a reload.\nSpring XML configuration files are handled separately.");
		GridDataFactory.fillDefaults().span(2, 1).applyTo(enhancedRedeploymentLabel);

		filenameTable = toolkit.createTable(composite, SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		data.heightHint = 130;
		filenameTable.setLayoutData(data);
		filenamesTableViewer = new TableViewer(filenameTable);
		contentProvider = new StaticFilenamesContentProvider();
		filenamesTableViewer.setContentProvider(contentProvider);
		filenamesTableViewer.setLabelProvider(new FilenameLabelProvider());

		filenamesTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();
				deleteButton.setEnabled(obj != null);
				updateButtons(obj);
			}
		});

		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridLayout buttonLayout = new GridLayout(1, true);
		buttonLayout.marginWidth = 0;
		buttonLayout.marginHeight = 0;
		buttonComposite.setLayout(buttonLayout);
		GridDataFactory.fillDefaults().applyTo(buttonComposite);

		addButton = toolkit.createButton(buttonComposite, "Add...", SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(addButton);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (updating) {
					return;
				}

				InputDialog dialog = new InputDialog(
						getShell(),
						"New filename pattern",
						"Enter a new filename pattern for static resources (wildcards such as '*' and '?' are supported):",
						"", new IInputValidator() {

							public String isValid(String newText) {
								if (StringUtils.isBlank(newText)) {
									return "Pattern can't be empty";
								}
								return null;
							}
						});
				if (dialog.open() == Dialog.OK) {
					updating = true;
					List<String> filenames = new ArrayList<>(Arrays.asList(contentProvider.getElements(server)));
					filenames.add(dialog.getValue());
					execute(new ModifyStaticResourcesCommand(serverWorkingCopy, String.join(",", filenames)));
					filenamesTableViewer.setInput(server);
					filenamesTableViewer.setSelection(new StructuredSelection(dialog.getValue()));
					// update buttons
					updating = false;
				}
			}
		});

		deleteButton = toolkit.createButton(buttonComposite, "Delete", SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(deleteButton);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object selectedArtefact = ((IStructuredSelection) filenamesTableViewer.getSelection())
						.getFirstElement();
				if (updating) {
					return;
				}
				updating = true;
				List<String> filenames = new ArrayList<>(Arrays.asList(contentProvider.getElements(server)));
				filenames.remove(selectedArtefact);
				execute(new ModifyStaticResourcesCommand(serverWorkingCopy, String.join(",", filenames)));
				filenamesTableViewer.setInput(server);
				// update buttons
				updating = false;
			}
		});

		upButton = toolkit.createButton(buttonComposite, "Up", SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(upButton);
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object selectedArtefact = ((IStructuredSelection) filenamesTableViewer.getSelection())
						.getFirstElement();
				List<String> modules = new ArrayList<>();
				modules.addAll(Arrays.asList(contentProvider.getElements(server)));
				int index = modules.indexOf(selectedArtefact);
				modules.remove(selectedArtefact);
				modules.add(index - 1, selectedArtefact.toString());
				if (updating) {
					return;
				}
				updating = true;
				execute(new ModifyStaticResourcesCommand(serverWorkingCopy, String.join(",", modules)));
				filenamesTableViewer.setInput(server);
				updateButtons(selectedArtefact);
				updating = false;
			}
		});

		downButton = toolkit.createButton(buttonComposite, "Down", SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(downButton);
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object selectedArtefact = ((IStructuredSelection) filenamesTableViewer.getSelection())
						.getFirstElement();
				List<String> modules = new ArrayList<>();
				modules.addAll(Arrays.asList(contentProvider.getElements(server)));
				int index = modules.indexOf(selectedArtefact);
				modules.remove(selectedArtefact);
				modules.add(index + 1, selectedArtefact.toString());
				if (updating) {
					return;
				}
				updating = true;
				execute(new ModifyStaticResourcesCommand(serverWorkingCopy, String.join(",", modules)));
				filenamesTableViewer.setInput(server);
				updateButtons(selectedArtefact);
				updating = false;
			}
		});

		FormText restoreDefault = toolkit.createFormText(composite, true);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(restoreDefault);
		restoreDefault.setText("<form><p><a href=\"exportbundle\">Restore default</a> filename patterns</p></form>",
				true, false);
		restoreDefault.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				updating = true;
				execute(new ModifyStaticResourcesCommand(serverWorkingCopy, TcServer.DEFAULT_STATIC_FILENAMES));
				filenamesTableViewer.setInput(server);
				updating = false;
			}
		});
		updateEnablement();
		initialize();
	}

	/**
	 * @see ServerEditorSection#dispose()
	 */
	@Override
	public void dispose() {
		if (server != null) {
			server.removePropertyChangeListener(listener);
		}
	}

	/**
	 * @see ServerEditorSection#init(IEditorSite, IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		serverWorkingCopy = (TcServer) server.loadAdapter(TcServer.class, null);
		addConfigurationChangeListener();
	}

	/**
	 * Initialize the fields in this editor.
	 */
	protected void initialize() {
		updating = true;
		enableButton.setSelection(serverWorkingCopy.isEnhancedRedeployEnabled());
		enableAgentButton.setSelection(serverWorkingCopy.isAgentRedeployEnabled());
		agentOptionsText.setText(serverWorkingCopy.getAgentOptions());
		filenamesTableViewer.setInput(server);
		deleteButton.setEnabled(false);
		updating = false;
	}

	private void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) filenamesTableViewer.getSelection();
		Object selectedArtefact = selection.getFirstElement();
		updateButtons(selectedArtefact);
	}

	private void updateButtons(Object obj) {
		if (obj instanceof String) {
			List<Object> modules = Arrays.asList(contentProvider.getElements(server));
			int index = modules.indexOf(obj);
			upButton.setEnabled(index > 0);
			downButton.setEnabled(index < modules.size() - 1);
			deleteButton.setEnabled(true);
		}
		else {
			upButton.setEnabled(false);
			downButton.setEnabled(false);
			deleteButton.setEnabled(false);
		}
	}

	private void updateEnablement() {
		boolean agentEnabled = serverWorkingCopy.isAgentRedeployEnabled();
		boolean jmxEnabled = serverWorkingCopy.isEnhancedRedeployEnabled();

		if (!jmxEnabled) {
			filenamesTableViewer.setSelection(StructuredSelection.EMPTY);
		}
		filenameTable.setEnabled(jmxEnabled);
		addButton.setEnabled(jmxEnabled);

		agentOptionsText.setEnabled(agentEnabled);

		enableButton.setEnabled(!agentEnabled);
		enableAgentButton.setEnabled(!jmxEnabled);

		updateButtons();
	}
}
