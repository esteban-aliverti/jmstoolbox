/*
 * Copyright (C) 2015-2017 Denis Forveille titou10.titou10@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.titou10.jtb.dialog;

import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.titou10.jtb.config.ConfigManager;
import org.titou10.jtb.cs.ColumnsSetsManager;
import org.titou10.jtb.cs.gen.ColumnsSet;
import org.titou10.jtb.ui.JTBStatusReporter;
import org.titou10.jtb.ui.part.MessageTab;
import org.titou10.jtb.util.Constants;
import org.titou10.jtb.util.Utils;

/**
 * Display the preference dialog
 * 
 * @author Denis Forveille
 *
 */
public class PreferencesDialog extends PreferenceDialog {

   private static final Logger log = LoggerFactory.getLogger(PreferencesDialog.class);

   private Shell               shell;
   private ColumnsSetsManager  csManager;
   private JTBStatusReporter   jtbStatusReporter;

   private boolean             oldTrustAllCertificates;
   private boolean             needsRestart;

   public PreferencesDialog(Shell parentShell,
                            JTBStatusReporter jtbStatusReporter,
                            PreferenceManager manager,
                            ConfigManager cm,
                            ColumnsSetsManager csManager) {
      super(parentShell, manager);
      setDefaultImage(SWTResourceManager.getImage(this.getClass(), "icons/preferences/cog.png"));

      this.shell = parentShell;
      this.csManager = csManager;
      this.jtbStatusReporter = jtbStatusReporter;

      PreferenceStore preferenceStore = cm.getPreferenceStore();
      setPreferenceStore(preferenceStore);

      PreferenceNode one = new PreferenceNode("P1", new PrefPageGeneral("General", preferenceStore));
      manager.addToRoot(one);

      for (PreferencePage pp : cm.getPluginsPreferencePages()) {
         manager.addToRoot(new PreferenceNode(pp.getTitle(), pp));
      }

      oldTrustAllCertificates = preferenceStore.getBoolean(Constants.PREF_TRUST_ALL_CERTIFICATES);
      needsRestart = false;
   }

   public boolean isNeedsRestart() {
      return needsRestart;
   }

   private final class PrefPageGeneral extends PreferencePage {

      private IPreferenceStore preferenceStore;

      private Spinner          spinnerAutoRefreshDelay;
      private Spinner          spinnerMaxMessages;
      private Button           showSystemObject;
      private Button           showNonBrowsableQueue;
      private Button           trustAllCertificates;
      private Button           clearScriptLogsOnExecution;
      private Spinner          spinnerMaxMessagesTopic;
      private Text             textConnectionClientId;
      private Spinner          spinnerXMLindent;
      private Button           synchronizeSessionBrowser;
      private Combo            comboMessageTabDisplay;
      private ComboViewer      comboCS;

      public PrefPageGeneral(String title, PreferenceStore preferenceStore) {
         super(title);
         this.preferenceStore = preferenceStore;
      }

      @Override
      protected Control createContents(Composite parent) {
         Composite composite = new Composite(parent, SWT.NONE);
         composite.setLayout(new GridLayout(1, false));

         // Message Browsers

         Group gBrowser = new Group(composite, SWT.SHADOW_ETCHED_IN);
         gBrowser.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
         gBrowser.setText("Message Browsers");
         gBrowser.setLayout(new GridLayout(3, false));

         showSystemObject = new Button(gBrowser, SWT.CHECK);
         showSystemObject.setText("Show system destinations (Also show 'temporary' destinations for some Q Providers)");
         showSystemObject.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

         Label lbl1 = new Label(gBrowser, SWT.LEFT);
         lbl1.setText("Limit messages displayed to");
         lbl1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
         spinnerMaxMessages = new Spinner(gBrowser, SWT.BORDER);
         spinnerMaxMessages.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
         spinnerMaxMessages.setMinimum(0);
         spinnerMaxMessages.setMaximum(9999);
         spinnerMaxMessages.setIncrement(1);
         spinnerMaxMessages.setPageIncrement(50);
         spinnerMaxMessages.setTextLimit(4);
         Label lbl2 = new Label(gBrowser, SWT.LEFT);
         lbl2.setText("messages (0 = no limit)");

         Label lbl3 = new Label(gBrowser, SWT.LEFT);
         lbl3.setText("Default 'Auto refresh' frequency to");
         lbl3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
         spinnerAutoRefreshDelay = new Spinner(gBrowser, SWT.BORDER | SWT.RIGHT);
         spinnerAutoRefreshDelay.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
         spinnerAutoRefreshDelay.setMinimum(Constants.MINIMUM_AUTO_REFRESH);
         spinnerAutoRefreshDelay.setMaximum(600);
         spinnerAutoRefreshDelay.setIncrement(1);
         spinnerAutoRefreshDelay.setPageIncrement(5);
         spinnerAutoRefreshDelay.setTextLimit(3);
         Label lbl4 = new Label(gBrowser, SWT.LEFT);
         lbl4.setText("seconds");

         Label lbl9 = new Label(gBrowser, SWT.LEFT);
         lbl9.setText("Limit messages captured per topic subscription to");
         lbl9.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
         spinnerMaxMessagesTopic = new Spinner(gBrowser, SWT.BORDER);
         spinnerMaxMessagesTopic.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
         spinnerMaxMessagesTopic.setMinimum(0);
         spinnerMaxMessagesTopic.setMaximum(9999);
         spinnerMaxMessagesTopic.setIncrement(1);
         spinnerMaxMessagesTopic.setPageIncrement(50);
         spinnerMaxMessagesTopic.setTextLimit(4);
         Label lbl10 = new Label(gBrowser, SWT.LEFT);
         lbl10.setText("messages (0 = no limit)");

         Label lbl24 = new Label(gBrowser, SWT.LEFT);
         lbl24.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
         lbl24.setText("Default Columns Set:");
         comboCS = new ComboViewer(gBrowser, SWT.READ_ONLY);
         comboCS.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
         comboCS.setContentProvider(ArrayContentProvider.getInstance());
         comboCS.getCombo().setToolTipText("Default columns set to use ");
         comboCS.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
               ColumnsSet cs = (ColumnsSet) element;
               return cs.getName();
            }
         });
         comboCS.setInput(csManager.getColumnsSets());

         // Queue Depth Browser

         Group qQDepth = new Group(composite, SWT.SHADOW_ETCHED_IN);
         qQDepth.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
         qQDepth.setText("Queue depth browsers");
         qQDepth.setLayout(new GridLayout(1, false));

         showNonBrowsableQueue = new Button(qQDepth, SWT.CHECK);
         showNonBrowsableQueue.setText("Show 'non browsable' queues in the 'Queue depth' browser");

         // Message Viewer

         Group gMessage = new Group(composite, SWT.SHADOW_ETCHED_IN);
         gMessage.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
         gMessage.setText("Message Viewer");
         gMessage.setLayout(new GridLayout(2, false));

         GridLayout gl = new GridLayout(3, false);
         gl.marginLeft = -5;
         Composite cIdent = new Composite(gMessage, SWT.NONE);
         cIdent.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
         cIdent.setLayout(gl);

         Label lbl12 = new Label(cIdent, SWT.LEFT);
         lbl12.setText("Indent XML tags by ");
         spinnerXMLindent = new Spinner(cIdent, SWT.BORDER);
         spinnerXMLindent.setMinimum(1);
         spinnerXMLindent.setMaximum(16);
         spinnerXMLindent.setIncrement(1);
         spinnerXMLindent.setPageIncrement(3);
         spinnerXMLindent.setTextLimit(2);
         spinnerXMLindent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
         Label lbl13 = new Label(cIdent, SWT.LEFT);
         lbl13.setText("chars");

         Label lbl14 = new Label(gMessage, SWT.LEFT);
         lbl14.setText("Tab selected by default in message viewer:");
         comboMessageTabDisplay = new Combo(gMessage, SWT.DROP_DOWN | SWT.READ_ONLY);
         comboMessageTabDisplay.setToolTipText("Default tab to display");
         comboMessageTabDisplay.setItems(MessageTab.getDisplayTexts());

         // General

         Group gGeneral = new Group(composite, SWT.SHADOW_ETCHED_IN);
         gGeneral.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, true, false, 1, 1));
         gGeneral.setText("General UI");
         gGeneral.setLayout(new GridLayout(1, false));

         synchronizeSessionBrowser = new Button(gGeneral, SWT.CHECK);
         synchronizeSessionBrowser
                  .setText("Synchronize the selected destination in the session tree viewer with the selected destination in the message browser");

         // Scripts

         Group gScripts = new Group(composite, SWT.SHADOW_ETCHED_IN);
         gScripts.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, true, false, 1, 1));
         gScripts.setText("Scripts");
         gScripts.setLayout(new GridLayout(1, false));

         clearScriptLogsOnExecution = new Button(gScripts, SWT.CHECK);
         clearScriptLogsOnExecution.setText("Clear scripts logs before execution/simulation");

         // Connection

         Group gConnection = new Group(composite, SWT.SHADOW_ETCHED_IN);
         gConnection.setText("Connection");
         gConnection.setLayout(new GridLayout(2, false));
         gConnection.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, true, false, 1, 1));

         Label lbl11 = new Label(gConnection, SWT.LEFT);
         lbl11.setText("JMS connection 'Client ID' prefix: ");
         textConnectionClientId = new Text(gConnection, SWT.BORDER);
         textConnectionClientId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

         trustAllCertificates = new Button(gConnection, SWT.CHECK);
         trustAllCertificates.setText("Trust all server certificates. Do not check/validate servers certificate authenticity !!!");
         trustAllCertificates.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

         // Set Values
         spinnerMaxMessages.setSelection(preferenceStore.getInt(Constants.PREF_MAX_MESSAGES));
         spinnerAutoRefreshDelay.setSelection(preferenceStore.getInt(Constants.PREF_AUTO_REFRESH_DELAY));
         showSystemObject.setSelection(preferenceStore.getBoolean(Constants.PREF_SHOW_SYSTEM_OBJECTS));
         showNonBrowsableQueue.setSelection(preferenceStore.getBoolean(Constants.PREF_SHOW_NON_BROWSABLE_Q));
         trustAllCertificates.setSelection(preferenceStore.getBoolean(Constants.PREF_TRUST_ALL_CERTIFICATES));
         clearScriptLogsOnExecution.setSelection(preferenceStore.getBoolean(Constants.PREF_CLEAR_LOGS_EXECUTION));
         spinnerMaxMessagesTopic.setSelection(preferenceStore.getInt(Constants.PREF_MAX_MESSAGES_TOPIC));
         textConnectionClientId.setText(preferenceStore.getString(Constants.PREF_CONN_CLIENT_ID_PREFIX));
         spinnerXMLindent.setSelection(preferenceStore.getInt(Constants.PREF_XML_INDENT));
         synchronizeSessionBrowser.setSelection(preferenceStore.getBoolean(Constants.PREF_SYNCHRONIZE_SESSIONS_MESSAGES));

         String messageTabString = preferenceStore.getString(Constants.PREF_MESSAGE_TAB_DISPLAY);
         comboMessageTabDisplay.select(MessageTab.getIndexFromDisplayTexts(messageTabString));

         String columnsSetName = preferenceStore.getString(Constants.PREF_COLUMNSSET_DEFAULT_NAME);
         ColumnsSet cs = csManager.getColumnsSet(columnsSetName);
         if (cs == null) {
            cs = csManager.getSystemColumnsSet();
         }
         comboCS.setSelection(new StructuredSelection(cs));

         return composite;
      }

      @Override
      public boolean performOk() {
         saveValues();

         // Reboot required, confirm with user
         if (oldTrustAllCertificates == preferenceStore.getBoolean(Constants.PREF_TRUST_ALL_CERTIFICATES)) {
            needsRestart = false;
         } else {
            if (!(MessageDialog
                     .openConfirm(shell,
                                  "Confirmation",
                                  "The 'Trust all certificates' option changed. This requires to restarts the application. Continue?"))) {
               needsRestart = false;
               return false;
            }
            needsRestart = true;
         }

         return true;
      }

      @Override
      protected void performApply() {
         saveValues();
      }

      @Override
      protected void performDefaults() {
         spinnerMaxMessages.setSelection(preferenceStore.getDefaultInt(Constants.PREF_MAX_MESSAGES));
         spinnerAutoRefreshDelay.setSelection(preferenceStore.getDefaultInt(Constants.PREF_AUTO_REFRESH_DELAY));
         showSystemObject.setSelection(preferenceStore.getDefaultBoolean(Constants.PREF_SHOW_SYSTEM_OBJECTS));
         showNonBrowsableQueue.setSelection(preferenceStore.getDefaultBoolean(Constants.PREF_SHOW_NON_BROWSABLE_Q));
         trustAllCertificates.setSelection(preferenceStore.getDefaultBoolean(Constants.PREF_TRUST_ALL_CERTIFICATES));
         clearScriptLogsOnExecution.setSelection(preferenceStore.getDefaultBoolean(Constants.PREF_CLEAR_LOGS_EXECUTION));
         spinnerMaxMessagesTopic.setSelection(preferenceStore.getDefaultInt(Constants.PREF_MAX_MESSAGES_TOPIC));
         textConnectionClientId.setText(preferenceStore.getDefaultString(Constants.PREF_CONN_CLIENT_ID_PREFIX));
         spinnerXMLindent.setSelection(preferenceStore.getDefaultInt(Constants.PREF_XML_INDENT));
         synchronizeSessionBrowser.setSelection(preferenceStore.getDefaultBoolean(Constants.PREF_SYNCHRONIZE_SESSIONS_MESSAGES));

         String messageTabString = preferenceStore.getDefaultString(Constants.PREF_MESSAGE_TAB_DISPLAY);
         comboMessageTabDisplay.select(MessageTab.getIndexFromDisplayTexts(messageTabString));

         comboCS.setSelection(new StructuredSelection(csManager.getSystemColumnsSet()), true);
      }

      // -------
      // Helpers
      // -------
      private void saveValues() {
         preferenceStore.setValue(Constants.PREF_MAX_MESSAGES, spinnerMaxMessages.getSelection());
         preferenceStore.setValue(Constants.PREF_AUTO_REFRESH_DELAY, spinnerAutoRefreshDelay.getSelection());
         preferenceStore.setValue(Constants.PREF_SHOW_SYSTEM_OBJECTS, showSystemObject.getSelection());
         preferenceStore.setValue(Constants.PREF_SHOW_NON_BROWSABLE_Q, showNonBrowsableQueue.getSelection());
         preferenceStore.setValue(Constants.PREF_TRUST_ALL_CERTIFICATES, trustAllCertificates.getSelection());
         preferenceStore.setValue(Constants.PREF_CLEAR_LOGS_EXECUTION, clearScriptLogsOnExecution.getSelection());
         preferenceStore.setValue(Constants.PREF_MAX_MESSAGES_TOPIC, spinnerMaxMessagesTopic.getSelection());
         preferenceStore.setValue(Constants.PREF_CONN_CLIENT_ID_PREFIX, textConnectionClientId.getText());
         preferenceStore.setValue(Constants.PREF_XML_INDENT, spinnerXMLindent.getSelection());
         preferenceStore.setValue(Constants.PREF_SYNCHRONIZE_SESSIONS_MESSAGES, synchronizeSessionBrowser.getSelection());

         int sel = comboMessageTabDisplay.getSelectionIndex();
         MessageTab messageTab = MessageTab.fromText(MessageTab.getDisplayTexts()[sel]);
         preferenceStore.setValue(Constants.PREF_MESSAGE_TAB_DISPLAY, messageTab.name());

         ColumnsSet cs = (ColumnsSet) comboCS.getStructuredSelection().getFirstElement();

         preferenceStore.setValue(Constants.PREF_COLUMNSSET_DEFAULT_NAME, cs.getName());

         // Save the preferences
         try {
            ((PreferenceStore) getPreferenceStore()).save();
         } catch (IOException e) {
            String msg = "Exception occurred when saving preferences";
            log.error(msg, e);
            jtbStatusReporter.showError(msg, Utils.getCause(e), e.getMessage());

         }
      }

   }

}
