/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
 * Microsystems, Inc. All Rights Reserved.
 * Portions Copyright 2009 Alexander Coles (Ikonoklastik Productions).
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.nbgit.ui.repository;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.nbgit.GitModuleConfig;
import org.netbeans.api.options.OptionsDisplayer;

import org.netbeans.modules.versioning.util.DialogBoundsPreserver;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;

/**
 * @author Tomas Stupka
 * @author Marian Petras
 */
public class GitRepositoryUI implements ActionListener, FocusListener, ItemListener {

    public final static int FLAG_URL_ENABLED            = 4;
    public final static int FLAG_ACCEPT_REVISION        = 8;
    public final static int FLAG_SHOW_HINTS             = 32;
    public final static int FLAG_SHOW_PROXY             = 64;

    private final static String LOCAL_URL_HELP          = "file:///repository_path";              // NOI18N
    private final static String HTTP_URL_HELP           = Utilities.isWindows()?
        "http://[DOMAIN%5C]hostname/repository_path":      // NOI18N
        "http://hostname/repository_path";      // NOI18N
    private final static String HTTPS_URL_HELP          = Utilities.isWindows()?
        "https://[DOMAIN%5C]hostname/repository_path":     // NOI18N
        "https://hostname/repository_path";     // NOI18N
    private final static String GIT_URL_HELP            = "git://hostname/repository_path"; // NOI18N
    private final static String GIT_SSH_URL_HELP        = "git@hostname:repository_path"; // NOI18N
    private final static String SSH_URL_HELP            = "ssh://hostname/repository_path";   // NOI18N

    private RepositoryPanel repositoryPanel;
    private boolean valid = true;
    private List<ChangeListener> listeners;
    private final ChangeEvent changeEvent = new ChangeEvent(this);

    private Transport repositoryConnection;
    private URIish url;

    public static final String PROP_VALID = "valid";                                                    // NOI18N

    private String message;
    private int modeMask;
    private Dimension maxNeededSize;
    private boolean bPushPull;
    private static int GIT_PUSH_PULL_VERT_PADDING = 30;

    private JTextComponent urlComboEditor;
    private Document urlDoc, usernameDoc, passwordDoc, tunnelCmdDoc;
    private boolean urlBeingSelectedFromPopup = false;

    public GitRepositoryUI(int modeMask, String titleLabel, boolean bPushPull) {

        this.modeMask = modeMask;

        initPanel();

        repositoryPanel.titleLabel.setText(titleLabel);

        repositoryPanel.urlComboBox.setEnabled(isSet(FLAG_URL_ENABLED));
        repositoryPanel.tunnelHelpLabel.setVisible(isSet(FLAG_SHOW_HINTS));
        repositoryPanel.tipLabel.setVisible(isSet(FLAG_SHOW_HINTS));

        //repositoryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));

        // retrieve the dialog size for the largest configuration
        if(bPushPull)
            updateVisibility("foo:"); // NOI18N
        else
            updateVisibility("https:"); // NOI18N
        maxNeededSize = repositoryPanel.getPreferredSize();

        //TODO: implement this
        //repositoryPanel.savePasswordCheckBox.setSelected(GitModuleConfig.getDefault().getSavePassword());
        repositoryPanel.schedulePostInitRoutine(new Runnable() {
                    public void run() {
                        refreshUrlHistory();
                    }
        });
    }

    public void actionPerformed(ActionEvent e) {
        assert e.getSource() == repositoryPanel.proxySettingsButton;

        onProxyConfiguration();
    }

    private void onProxyConfiguration() {
        OptionsDisplayer.getDefault().open("General");              // NOI18N
    }

    private void initPanel() {
        repositoryPanel = new RepositoryPanel();

        urlComboEditor = (JTextComponent) repositoryPanel.urlComboBox
                                          .getEditor().getEditorComponent();
        urlDoc = urlComboEditor.getDocument();
        usernameDoc = repositoryPanel.userTextField.getDocument();
        passwordDoc = repositoryPanel.userPasswordField.getDocument();
        tunnelCmdDoc = repositoryPanel.tunnelCommandTextField.getDocument();

        DocumentListener documentListener = new DocumentChangeHandler();
        urlDoc.addDocumentListener(documentListener);
        passwordDoc.addDocumentListener(documentListener);
        usernameDoc.addDocumentListener(documentListener);
        tunnelCmdDoc.addDocumentListener(documentListener);

        repositoryPanel.savePasswordCheckBox.addItemListener(this);
        repositoryPanel.urlComboBox.addItemListener(this);

        repositoryPanel.proxySettingsButton.addActionListener(this);

        repositoryPanel.userPasswordField.addFocusListener(this);

        tweakComboBoxEditor();
    }

    private void tweakComboBoxEditor() {
        final ComboBoxEditor origEditor = repositoryPanel.urlComboBox.getEditor();

        if (origEditor.getClass() == UrlComboBoxEditor.class) {
            /* attempt to tweak the combo-box multiple times */
            assert false;
            return;
        }

        repositoryPanel.urlComboBox.setEditor(new UrlComboBoxEditor(origEditor));
    }

    /**
     * Customized combo-box editor for displaying/modification of URL
     * of a Git repository.
     * It is customized in the following aspects:
     * <ul>
     *     <li>When a RepositoryConnection is selected, displays its URL
     *         without user data (name and password).</li>
     *     <li>If a {@code RepositoryConnection} is set via method
     *         {@code setItem}, it holds a reference to it until another item
     *         is set via method {@code setItem()} or until the user modifies
     *         the text. This allows method {@code getItem()} to return
     *         the same item ({@code RepositoryConnection}).
     *         The allows the combo-box to correctly detect whether the item
     *         has been changed (since the last call of {@code setItem()}
     *         or not.</li>
     * </ul>
     */
    private final class UrlComboBoxEditor implements ComboBoxEditor,
                                                         DocumentListener {

        private final ComboBoxEditor origEditor;
        private Reference<Transport> repoConnRef;

        private UrlComboBoxEditor(ComboBoxEditor originalEditor) {
            this.origEditor = originalEditor;
            ((JTextComponent) originalEditor.getEditorComponent())
                    .getDocument().addDocumentListener(this);
        }

        public void setItem(Object anObject) {
            urlBeingSelectedFromPopup = true;
            try {
                setItemImpl(anObject);
            } finally {
                urlBeingSelectedFromPopup = false;
            }
        }

        private void setItemImpl(Object anObject) {
            assert urlBeingSelectedFromPopup;

            if (anObject instanceof Transport) {
                Transport repoConn = (Transport) anObject;
                repoConnRef = new WeakReference<Transport>(repoConn);
                origEditor.setItem(repoConn.getURI().toString());
            } else {
                clearRepoConnRef();
                origEditor.setItem(anObject);
            }
        }

        public Component getEditorComponent() {
            return origEditor.getEditorComponent();
        }
        public Object getItem() {
            Transport repoConn = getRepoConn();
            if (repoConn != null) {
                return repoConn;
            }

            return origEditor.getItem();
        }
        public void selectAll() {
            origEditor.selectAll();
        }
        public void addActionListener(ActionListener l) {
            origEditor.addActionListener(l);
        }
        public void removeActionListener(ActionListener l) {
            origEditor.removeActionListener(l);
        }

        public void insertUpdate(DocumentEvent e) {
            textChanged();
        }
        public void removeUpdate(DocumentEvent e) {
            textChanged();
        }
        public void changedUpdate(DocumentEvent e) {
            textChanged();
        }
        private void textChanged() {
            if (urlBeingSelectedFromPopup) {
                return;
            }
            clearRepoConnRef();
        }

        private Transport getRepoConn() {
            if (repoConnRef != null) {
                Transport repoConn = repoConnRef.get();
                if (repoConn != null) {
                    return repoConn;
                }
            }
            return null;
        }

        private void clearRepoConnRef() {
            if (repoConnRef != null) {
                repoConnRef.clear();
            }
        }

    }

    public void refreshUrlHistory() {
        repositoryPanel.urlComboBox.setModel(
                new DefaultComboBoxModel(createPresetComboEntries()));

        urlComboEditor.selectAll();
    }

    private Vector<?> createPresetComboEntries() {
        assert repositoryPanel.urlComboBox.isEditable();

        Vector<Object> result;

        List<Transport> recentUrls = new ArrayList<Transport>(); // TODO: implement GitModuleConfig.getDefault().getRecentUrls();
        GitURIScheme[] schemes = GitURIScheme.values();

        result = new Vector<Object>(recentUrls.size() + schemes.length);
        result.addAll(recentUrls);
        for (GitURIScheme scheme : schemes) {
            result.add(createURIPrefixForScheme(scheme));
        }

        return result;
    }

    private static String createURIPrefixForScheme(GitURIScheme scheme) {
        if (scheme == GitURIScheme.FILE) {
            return scheme + ":/";                                       //NOI18N
        } else {
            return scheme + "://";                                      //NOI18N
        }
    }

    private final class DocumentChangeHandler implements DocumentListener {

        DocumentChangeHandler() { }

        public void insertUpdate(DocumentEvent e) {
            textChanged(e);
        }
        public void removeUpdate(DocumentEvent e) {
            textChanged(e);
        }
        public void changedUpdate(DocumentEvent e) {
            textChanged(e);
        }

        private void textChanged(final DocumentEvent e) {
            assert EventQueue.isDispatchThread();

            Document modifiedDocument = e.getDocument();

            assert modifiedDocument != null;
            assert (modifiedDocument == urlDoc) || !urlBeingSelectedFromPopup;

            if (modifiedDocument == urlDoc) {
                onUrlChange();
            } else if (modifiedDocument == usernameDoc) {
                onUsernameChange();
            } else if (modifiedDocument == passwordDoc) {
                onPasswordChange();
            } else if (modifiedDocument == tunnelCmdDoc) {
                onTunnelCommandChange();
            }
        }

    }

    /**
     * Always updates UI fields visibility.
     */
    private void onUrlChange() {
        if (!urlBeingSelectedFromPopup) {
            repositoryConnection = null;
            url = null;

            repositoryPanel.userTextField.setText(null);
            repositoryPanel.userPasswordField.setText(null);
            repositoryPanel.tunnelCommandTextField.setText(null);
            repositoryPanel.savePasswordCheckBox.setSelected(false);
        }
        // TODO: implementation of validation quickValidateUrl();
        updateVisibility();
    }

    private void updateVisibility() {
        updateVisibility(getUrlString());
    }

    /** Shows proper fields depending on Git connection method. */
    private void updateVisibility(String selectedUrlString) {

        boolean authFields = false;
        boolean proxyFields = false;
        boolean sshFields = false;
        if(selectedUrlString.startsWith("http:")) {                             // NOI18N
            repositoryPanel.tipLabel.setText(HTTP_URL_HELP);
            authFields = true;
            proxyFields = true;
        } else if(selectedUrlString.startsWith("https:")) {                     // NOI18N
            repositoryPanel.tipLabel.setText(HTTPS_URL_HELP);
            //authFields = true;
            proxyFields = true;
        } else if (selectedUrlString.startsWith("git:")) {                      // NOI18N
            repositoryPanel.tipLabel.setText(GIT_URL_HELP);
        } else if(selectedUrlString.startsWith("git@")) {                       // NOI18N
            repositoryPanel.tipLabel.setText(GIT_SSH_URL_HELP);
            authFields = true;
            proxyFields = true;
        } else if(selectedUrlString.startsWith("ssh")) {                        // NOI18N
            repositoryPanel.tipLabel.setText(SSH_URL_HELP);
            sshFields = true;
        } else if(selectedUrlString.startsWith("file:")) {                      // NOI18N
            repositoryPanel.tipLabel.setText(LOCAL_URL_HELP);
        } else {
            repositoryPanel.tipLabel.setText(NbBundle.getMessage(GitRepositoryUI.class, "MSG_Repository_Url_Help", new Object [] { // NOI18N
                LOCAL_URL_HELP, HTTP_URL_HELP, HTTPS_URL_HELP, GIT_URL_HELP, GIT_SSH_URL_HELP, SSH_URL_HELP
            }));
        }

        repositoryPanel.userPasswordField.setVisible(authFields);
        repositoryPanel.passwordLabel.setVisible(authFields);
        repositoryPanel.userTextField.setVisible(authFields);
        repositoryPanel.leaveBlankLabel.setVisible(authFields);
        repositoryPanel.userLabel.setVisible(authFields);
        //repositoryPanel.savePasswordCheckBox.setVisible(authFields);
        repositoryPanel.savePasswordCheckBox.setVisible(false);
        repositoryPanel.proxySettingsButton.setVisible(proxyFields && ((modeMask & FLAG_SHOW_PROXY) != 0));

        repositoryPanel.savePasswordCheckBox.setVisible(false);
        repositoryPanel.tunnelCommandTextField.setVisible(false);
        repositoryPanel.tunnelCommandLabel.setVisible(false);
        repositoryPanel.tunnelLabel.setVisible(false);
        repositoryPanel.tunnelHelpLabel.setVisible(false);
    }

    public void setEditable(boolean editable) {
        assert EventQueue.isDispatchThread();

        repositoryPanel.urlComboBox.setEnabled(editable && isSet(FLAG_URL_ENABLED));
        repositoryPanel.userTextField.setEnabled(editable && valid);
        repositoryPanel.userPasswordField.setEnabled(editable && valid);
        repositoryPanel.savePasswordCheckBox.setEnabled(editable && valid);
        repositoryPanel.tunnelCommandTextField.setEnabled(editable && valid);
        repositoryPanel.proxySettingsButton.setEnabled(editable && valid);
    }

    /**
     * Load selected root from Swing structures (from arbitrary thread).
     * @return null on failure
     */
    public String getUrlString() {
        return urlComboEditor.getText().trim();
    }

    private String getUsername() {
        return repositoryPanel.userTextField.getText().trim();
    }

    private String getPassword() {
        char[] password = repositoryPanel.userPasswordField.getPassword();
        String result = new String(password);
        Arrays.fill(password, (char) 0);
        return result;
    }

    private String getExternalCommand() {
        return repositoryPanel.tunnelCommandTextField.getText();
    }

    private boolean isSavePassword() {
        return repositoryPanel.savePasswordCheckBox.isSelected();
    }

    public URIish getUrl() throws URISyntaxException, MalformedURLException {
        prepareUrl();
        assert (url != null);
        return url;
    }

    public Transport getRepositoryConnection() throws URISyntaxException {
        prepareRepositoryConnection();
        assert (repositoryConnection != null);
        return repositoryConnection;
    }

    private void prepareUrl() throws URISyntaxException, MalformedURLException {
        if (url != null) {
            return;
        }

        String urlString = getUrlString();
        String username = getUsername();

        if (username.length() == 0) {
            url = new URIish(urlString);
        } else {
            // Parse the URL, create a URI and convert back to URL just to
            // add the user + password.
            URL jurl = new URL(urlString);

            String protocol = jurl.getProtocol();
            String host = jurl.getHost();
            int port = jurl.getPort();
            String path = jurl.getPath();
            String userInfo = username + ":" + getPassword();

            URI juri = new URI(protocol,  userInfo,  host,  port,  path, null, null);
            url = new URIish(juri.toURL());
        }
    }

    private void prepareRepositoryConnection() {
        if (repositoryConnection != null) {
            return;
        }
        String extCommand = getExternalCommand();
        boolean savePassword = isSavePassword();

        //repositoryConnection = new Transport(url, extCommand, savePassword);
        // FIXME: we need the local repository
    }

    private void onUsernameChange() {
        repositoryConnection = null;
        url = null;
    }

    private void onPasswordChange() {
        repositoryConnection = null;
        url = null;
    }

    private void onTunnelCommandChange() {
        repositoryConnection = null;
    }

    private void onSavePasswordChange() {
        repositoryConnection = null;
    }

    public RepositoryPanel getPanel() {
        return repositoryPanel;
    }

    public boolean isValid() {
        return valid;
    }

    private void setValid() {
        setValid(true, "");                                             //NOI18N
    }

    public void setInvalid() {
        setValid(false, "");
    }

    private void setValid(boolean valid, String message) {
        if ((valid == this.valid) && message.equals(this.message)) {
            return;
        }

        if (valid != this.valid) {
            repositoryPanel.proxySettingsButton.setEnabled(valid);
            repositoryPanel.userPasswordField.setEnabled(valid);
            repositoryPanel.userTextField.setEnabled(valid);
            //repositoryPanel.savePasswordCheckBox.setEnabled(valid);
        }

        this.valid = valid;
        this.message = message;

        fireStateChanged();
    }

    private void fireStateChanged() {
        if ((listeners != null) && !listeners.isEmpty()) {
            for (ChangeListener l : listeners) {
                l.stateChanged(changeEvent);
            }
        }
    }

    public void addChangeListener(ChangeListener l) {
        if(listeners==null) {
            listeners = new ArrayList<ChangeListener>(4);
        }
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        if(listeners==null) {
            return;
        }
        listeners.remove(l);
    }

    public String getMessage() {
        return message;
    }

    public void focusGained(FocusEvent focusEvent) {
        if(focusEvent.getSource()==repositoryPanel.userPasswordField) {
            repositoryPanel.userPasswordField.selectAll();
        }
    }

    public void focusLost(FocusEvent focusEvent) {
        // do nothing
    }

    public void itemStateChanged(ItemEvent evt) {
        Object source = evt.getSource();

        if (source == repositoryPanel.urlComboBox) {
            if(evt.getStateChange() == ItemEvent.SELECTED) {
                comboBoxItemSelected(evt.getItem());
            }
        } else if (source == repositoryPanel.savePasswordCheckBox) {
            onSavePasswordChange();
        } else {
            assert false;
        }
    }

    private void comboBoxItemSelected(Object selectedItem) {
        if (selectedItem.getClass() == String.class) {
            urlPrefixSelected();
        } else if (selectedItem instanceof Transport) {
            repositoryConnectionSelected((Transport) selectedItem);
        } else {
            assert false;
        }
    }

    private void urlPrefixSelected() {
        repositoryPanel.userTextField.setText(null);
        repositoryPanel.userPasswordField.setText(null);
        repositoryPanel.tunnelCommandTextField.setText(null);
        repositoryPanel.savePasswordCheckBox.setSelected(false);

        url = null;
        repositoryConnection = null;
    }

    private void repositoryConnectionSelected(Transport rc) {
        url = rc.getURI();
        repositoryPanel.userTextField.setText(url.getUser());
        repositoryPanel.userPasswordField.setText(url.getPass());

        repositoryConnection = rc;
    }

    public void setTipVisible(Boolean flag) {
        repositoryPanel.tipLabel.setVisible(flag);
    }

    public boolean show(String title, HelpCtx helpCtx, boolean setMaxNeddedSize) {
        RepositoryDialogPanel corectPanel = new RepositoryDialogPanel();
        corectPanel.panel.setLayout(new BorderLayout());
        JPanel p = getPanel();
        if(setMaxNeddedSize) {
            if(bPushPull){
                maxNeededSize.setSize(maxNeededSize.width, maxNeededSize.height + GIT_PUSH_PULL_VERT_PADDING);
            }
            p.setPreferredSize(maxNeededSize);
        }
        corectPanel.panel.add(p, BorderLayout.NORTH);
        DialogDescriptor dialogDescriptor = new DialogDescriptor(corectPanel, title); // NOI18N
        showDialog(dialogDescriptor, helpCtx, null);
        return dialogDescriptor.getValue() == DialogDescriptor.OK_OPTION;
    }

    public Object show(String title, HelpCtx helpCtx, Object[] options, boolean setMaxNeededSize, String name) {
        RepositoryDialogPanel corectPanel = new RepositoryDialogPanel();
        corectPanel.panel.setLayout(new BorderLayout());
        corectPanel.panel.add(getPanel(), BorderLayout.NORTH);
        DialogDescriptor dialogDescriptor = new DialogDescriptor(corectPanel, title); // NOI18N
        JPanel p = getPanel();
        if(setMaxNeededSize) {
            if(bPushPull){
                maxNeededSize.setSize(maxNeededSize.width, maxNeededSize.height + GIT_PUSH_PULL_VERT_PADDING);
            }
            p.setPreferredSize(maxNeededSize);
        }
        if(options!= null) {
            dialogDescriptor.setOptions(options); // NOI18N
        }
        showDialog(dialogDescriptor, helpCtx, name);
        return dialogDescriptor.getValue();
    }

    private void showDialog(DialogDescriptor dialogDescriptor, HelpCtx helpCtx, String name) {
        dialogDescriptor.setModal(true);
        dialogDescriptor.setHelpCtx(helpCtx);

        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        if (name != null) {
            dialog.addWindowListener(new DialogBoundsPreserver(GitModuleConfig.getDefault().getPreferences(), name)); // NOI18N
        }
        dialog.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(GitRepositoryUI.class, "ACSD_RepositoryPanel"));

        dialog.setVisible(true);
    }

    private boolean isSet(int flag) {
        return (modeMask & flag) != 0;
    }

}
