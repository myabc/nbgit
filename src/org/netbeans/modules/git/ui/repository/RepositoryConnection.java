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
package org.netbeans.modules.git.ui.repository;

import java.net.MalformedURLException;


/**
 *
 * @author Tomas Stupka
 */
public class RepositoryConnection {
    
    private static final String RC_DELIMITER = "~=~"; // NOI18N
    
    private String url;   
    private String username;
    private String password;
    private String externalCommand;
    private boolean savePassword;
    
    private GitURL hgUrl;
    
    public RepositoryConnection(RepositoryConnection rc) {
        this(rc.url, rc.username, rc.password, rc.externalCommand, rc.savePassword);
    }
    
    public RepositoryConnection(String url) {
        this(url, null, null, null, false);
    }
            
    public RepositoryConnection(String url, String username, String password, String externalCommand, boolean savePassword) {
        this.setUrl(url);
        this.setUsername(username);
        this.setPassword(password);
        this.setExternalCommand(externalCommand);
        this.savePassword = savePassword;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username == null ? "" : username; // NOI18N
    }

    public String getPassword() {
        return password == null ? "" : password ; // NOI18N
    }

    public String getExternalCommand() {
        return externalCommand == null ? "" : externalCommand; // NOI18N
    }

    public boolean getSavePassword() {
        return savePassword;
    }

    public GitURL getHgUrl() throws MalformedURLException {
        if(hgUrl == null) {
            parseUrlString(url);
        }
        return hgUrl;
    }
    
    public boolean equals(Object o) {
        if (o == null) {
            return false;   
        }            
        if (getClass() != o.getClass()) {
            return false;
        }            
        
        final RepositoryConnection test = (RepositoryConnection) o;

        if (this.url != test.url && this.url != null && !this.url.equals(test.url)) {
            return false;
        }        
        return true;
    }
    
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + (this.url != null ? this.url.hashCode() : 0);        
        return hash;
    }

    void setUrl(String url) {
        this.url = url;
        hgUrl = null; 
    }

    void setUsername(String username) {
        this.username = username;
    }

    void setPassword(String password) {
        this.password = password;
    }

    void setExternalCommand(String externalCommand) {
        this.externalCommand = externalCommand;
    }

    void setSavePassword(boolean savePassword) {
        this.savePassword = savePassword;
    }

    @Override
    public String toString() {
        return url;
    }

    private void parseUrlString(String urlString) throws MalformedURLException {
        int hostIdx = urlString.indexOf("://");                         // NOI18N
    //    int firstSlashIdx = urlString.indexOf("/", hostIdx + 3);        // NOI18N
    //    if(idx < 0 || firstSlashIdx < 0 || idx < firstSlashIdx) {
    //        svnRevision = SVNRevision.HEAD;
    //    } else /*if (acceptRevision)*/ {
    //        if( idx + 1 < urlString.length()) {
    //            String revisionString = "";                             // NOI18N
    //            try {
    //                revisionString = urlString.substring(idx + 1);
    //                svnRevision = SvnUtils.getSVNRevision(revisionString);
    //            } catch (NumberFormatException ex) {
    //                throw new MalformedURLException(NbBundle.getMessage(Repository.class, "MSG_Repository_WrongRevision", revisionString));     // NOI18N
    //            }
    //        } else {
    //            svnRevision = SVNRevision.HEAD;
    //        }
    //        urlString = urlString.substring(0, idx);
    //    }    
        //urlO = removeEmptyPathSegments(new URL(urlString));
        hgUrl = new GitURL(urlString);
    }
    
    //private URL removeEmptyPathSegments(URL url) throws MalformedURLException {
    //    String[] pathSegments = url.getPathSegments();
    //    StringBuffer urlString = new StringBuffer();
    //    urlString.append(url.getProtocol());
    //    urlString.append("://");                                                // NOI18N
    //    urlString.append(HgUtils.ripUserFromHost(url.getHost()));
    //    if(url.getPort() > 0) {
    //        urlString.append(":");                                              // NOI18N
    //        urlString.append(url.getPort());
    //    }
    //    boolean gotSegments = false;
    //    for (int i = 0; i < pathSegments.length; i++) {
    //        if(!pathSegments[i].trim().equals("")) {                            // NOI18N
    //            gotSegments = true;
    //            urlString.append("/");                                          // NOI18N
    //            urlString.append(pathSegments[i]);                
    //        }
    //    }
    //    try {
    //        if(gotSegments) {
    //            return new URL(urlString.toString());
    //        } else {
    //            return url;
    //        }
    //    } catch (MalformedURLException ex) {
    //        throw ex;
    //    }
    //}
    
    public static String getString(RepositoryConnection rc) {
        String url = rc.getUrl();
        StringBuffer sb = new StringBuffer();        
        sb.append(url);
        sb.append(RC_DELIMITER);
        sb.append(rc.getUsername());
        sb.append(RC_DELIMITER);
        //sb.append(Scrambler.getInstance().scramble(rc.getPassword()));
        //sb.append(RC_DELIMITER);
        sb.append(rc.getExternalCommand());
        sb.append(RC_DELIMITER);        
        sb.append(RC_DELIMITER);
        return sb.toString();
    }
    
    public static RepositoryConnection parse(String str) {        
        String[] fields = str.split(RC_DELIMITER);
        int l = fields.length;
        String url          =           fields[0];
        String username     = l > 1 && !fields[1].equals("") ? fields[1] : null; // NOI18N
        //String password     = l > 2 && !fields[2].equals("") ? Scrambler.getInstance().descramble(fields[2]) : null; // NOI18N
        String password     = null;
        String extCmd       = l > 3 && !fields[3].equals("") ? fields[3] : null; // NOI18N
        boolean save        = l > 4 && !fields[4].equals("") ? Boolean.parseBoolean(fields[4]) : true;
        return new RepositoryConnection(url, username, password, extCmd, save);        
    }
}