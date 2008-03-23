/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 * Portions Copyright 2008 Alexander Coles (Ikonoklastik Productions).
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
package org.netbeans.modules.git.ui.wizards;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitModuleConfig;
import org.netbeans.modules.git.ui.repository.Repository;
import org.netbeans.modules.git.ui.repository.RepositoryConnection;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

public class CloneRepositoryWizardPanel implements WizardDescriptor.AsynchronousValidatingPanel, PropertyChangeListener {
    
    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private CloneRepositoryPanel component;
    private Repository repository;
    private int repositoryModeMask;
    private boolean valid;
    private String errorMessage;
    private WizardStepProgressSupport support;
    
    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    public Component getComponent() {
        if (component == null) {
            component = new CloneRepositoryPanel();
            if (repository == null) {
                repositoryModeMask = repositoryModeMask | Repository.FLAG_URL_EDITABLE | Repository.FLAG_URL_ENABLED | Repository.FLAG_SHOW_HINTS | Repository.FLAG_SHOW_PROXY;
                String title = org.openide.util.NbBundle.getMessage(CloneRepositoryWizardPanel.class, "CTL_Repository_Location");       // NOI18N
                repository = new Repository(repositoryModeMask, title);
                repository.addPropertyChangeListener(this);
                CloneRepositoryPanel panel = (CloneRepositoryPanel)component;
                panel.repositoryPanel.setLayout(new BorderLayout());
                panel.repositoryPanel.add(repository.getPanel());
                valid();
            }
        }
        return component;
    }
    
    public HelpCtx getHelp() {
        return new HelpCtx(CloneRepositoryWizardPanel.class);
    }
    
    //public boolean isValid() {
        // If it is always OK to press Next or Finish, then:
    //    return true;
        // If it depends on some condition (form filled out...), then:
        // return someCondition();
        // and when this condition changes (last form field filled in...) then:
        // fireChangeEvent();
        // and uncomment the complicated stuff below.
    //}
    
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals(Repository.PROP_VALID)) {
            if(repository.isValid()) {
                valid(repository.getMessage());
            } else {
                invalid(repository.getMessage());
            }
        }
    }

    private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1); // or can use ChangeSupport in NB 6.0
    public final void addChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }
    public final void removeChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }
    protected final void fireChangeEvent() {
        Iterator<ChangeListener> it;
        synchronized (listeners) {
            it = new HashSet<ChangeListener>(listeners).iterator();
        }
        ChangeEvent ev = new ChangeEvent(this);
        while (it.hasNext()) {
            it.next().stateChanged(ev);
        }
    }
    
    protected final void valid() {
        setValid(true, null);
    }

    protected final void valid(String extErrorMessage) {
        setValid(true, extErrorMessage);
    }

    protected final void invalid(String message) {
        setValid(false, message);
    }

    public final boolean isValid() {
        return valid;
    }

    public final String getErrorMessage() {
        return errorMessage;
    }

    private void setValid(boolean valid, String errorMessage) {
        boolean fire = this.valid != valid;
        fire |= errorMessage != null && (errorMessage.equals(this.errorMessage) == false);
        this.valid = valid;
        this.errorMessage = errorMessage;
        if (fire) {
            fireChangeEvent();
        }
    }

    protected void validateBeforeNext() {
        try {
            support = new RepositoryStepProgressSupport(component.progressPanel);

            String url = getUrl();
            support.setRepositoryRoot(url);
            RequestProcessor rp = Git.getInstance().getRequestProcessor(url);
            RequestProcessor.Task task = support.start(rp, url, NbBundle.getMessage(CloneRepositoryWizardPanel.class, "BK2012"));
            task.waitFinished();
        } finally {
            support = null;
        }

    }

    // comes on next or finish
    public final void validate () throws WizardValidationException {
        validateBeforeNext();
        if (isValid() == false || errorMessage != null) {
            throw new WizardValidationException (
                (javax.swing.JComponent) component,
                errorMessage,
                errorMessage
            );
        }
    }

    // You can use a settings object to keep track of state. Normally the
    // settings object will be the WizardDescriptor, so you can use
    // WizardDescriptor.getProperty & putProperty to store information entered
    // by the user.
    public void readSettings(Object settings) {}
    public void storeSettings(Object settings) {
        if (settings instanceof WizardDescriptor) {
            ((WizardDescriptor) settings).putProperty("repository", repository.getSelectedRC().getUrl()); // NOI18N
            ((WizardDescriptor) settings).putProperty("username", repository.getSelectedRC().getUsername()); // NOI18N
            ((WizardDescriptor) settings).putProperty("password", repository.getSelectedRC().getPassword()); // NOI18N
        }
    }

    public void prepareValidation() {
    }

    private String getUrl() {
        return getSelectedRepositoryConnection().getUrl();
    }

    private void storeHistory() {
        RepositoryConnection rc = getSelectedRepositoryConnection();
        if(rc != null) {
            GitModuleConfig.getDefault().insertRecentUrl(rc);
        }
    }

    private RepositoryConnection getSelectedRepositoryConnection() {
        try {
            return repository.getSelectedRC();
        } catch (Exception ex) {
            invalid(ex.getLocalizedMessage());
            return null;
        }
    }

    public void stop() {
        if(support != null) {
            support.cancel();
        }
    }

    private class RepositoryStepProgressSupport extends WizardStepProgressSupport {

        public RepositoryStepProgressSupport(JPanel panel) {
            super(panel);
        }

        public void perform() {
            final RepositoryConnection rc = getSelectedRepositoryConnection();
            if (rc == null) {
                return;
            }
            String invalidMsg = null;
            try {
                invalid(null);

                // This command validates the url
                rc.getGitUrl();
                String urlStr = rc.getUrl();
                URI uri = new URI(urlStr);
                String uriSch = uri.getScheme();
                if(uriSch.equals("file")){
                    File f = new File(urlStr.substring("file://".length()));
                    if(!f.exists() || !f.canRead()){
                        invalidMsg = NbBundle.getMessage(CloneRepositoryWizardPanel.class,
                               "MSG_Progress_Clone_CannotAccess_Err");
                        return;
                    }
                }else if(uriSch.equals("http") || uriSch.equals("https")) {
                    URL url = new URL(urlStr);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    // Note: valid repository returns con.getContentLength() = -1
                    // so no way to reliably test if this url exists, without using git
                    if (con != null){
                        String userInfo = uri.getUserInfo();
                        boolean bNoUserAndOrPasswordInURL = userInfo == null;
                        // If username or username:password is in the URL the con.getResponseCode() returns -1 and this check would fail
                        if (bNoUserAndOrPasswordInURL && con.getResponseCode() != HttpURLConnection.HTTP_OK){
                            invalidMsg = NbBundle.getMessage(CloneRepositoryWizardPanel.class,
                                    "MSG_Progress_Clone_CannotAccess_Err");
                            con.disconnect();
                            return;
                        }else if (userInfo != null){
                            Git.LOG.log(Level.FINE, 
                                "RepositoryStepProgressSupport.perform(): UserInfo - {0}", new Object[]{userInfo}); // NOI18N
                        }
                        con.disconnect();
                    }
                 }
            } catch (java.lang.IllegalArgumentException ex) {
                 invalidMsg = NbBundle.getMessage(CloneRepositoryWizardPanel.class,
                                  "MSG_Progress_Clone_InvalidURL_Err");
                 return;
            } catch (IOException ex) {
                 invalidMsg = NbBundle.getMessage(CloneRepositoryWizardPanel.class,
                                  "MSG_Progress_Clone_CannotAccess_Err");
                return;
            } catch (URISyntaxException ex) {
                 invalidMsg = NbBundle.getMessage(CloneRepositoryWizardPanel.class,
                                  "MSG_Progress_Clone_InvalidURL_Err");
                return;
            } finally {
                if(isCanceled()) {
                    valid(org.openide.util.NbBundle.getMessage(CloneRepositoryWizardPanel.class, "CTL_Repository_Canceled")); // NOI18N
                } else if(invalidMsg == null) {
                  valid();
                  storeHistory();
                } else {
                  invalid(invalidMsg);
                }
            }
        }

        public void setEditable(boolean editable) {
            repository.setEditable(editable);
        }
    };



}
