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
package org.netbeans.modules.git.util;

import java.util.prefs.Preferences;

/**
 *
 * @author Tomas Stupka
 */
public class GitProxySettings {
    
    private static final String PROXY_HTTP_HOST                 = "proxyHttpHost";
    private static final String PROXY_HTTP_PORT                 = "proxyHttpPort";
    private static final String PROXY_HTTPS_HOST                = "proxyHttpsHost";
    private static final String PROXY_HTTPS_PORT                = "proxyHttpsPort";
    private static final String NOT_PROXY_HOSTS                 = "proxyNonProxyHosts";
    private static final String USE_PROXY_AUTHENTICATION        = "useProxyAuthentication";
    private static final String PROXY_AUTHENTICATION_USERNAME   = "proxyAuthenticationUsername";
    private static final String PROXY_AUTHENTICATION_PASSWORD   = "proxyAuthenticationPassword";
    
    private static final String PROXY_TYPE                      = "proxyType";
    private static final String DIRECT_CONNECTION               = "0";            
    private static final String AUTO_DETECT_PROXY               = "1"; // as default
    private static final String MANUAL_SET_PROXY                = "2";
            
    private String username;
    private String password;
    private String notProxyHosts;
    private boolean useAuth;
    private String httpHost;
    private String httpPort;
    private String httpsHost;
    private String httpsPort;
    private String proxyType;
      
    private String toString = null;
    
    public GitProxySettings() {
        init();
    };
        
    private void init() {
        Preferences prefs = org.openide.util.NbPreferences.root ().node ("org/netbeans/core");                              // NOI18N    
        proxyType           = prefs.get        ( PROXY_TYPE,                     ""    );                                   // NOI18N                                        

        if(proxyType.equals(DIRECT_CONNECTION)) {
            useAuth             = false;
            username            = "";                                                                                       // NOI18N
            password            = "";                                                                                       // NOI18N            
        
            notProxyHosts       = "";                                                                                       // NOI18N
            httpHost            = "";                                                                                       // NOI18N
            httpPort            = "";                                                                                       // NOI18N
            httpsHost           = "";                                                                                       // NOI18N
            httpsPort           = "";                                                                                       // NOI18N
        } else if(isManualSetProxy()) {
            useAuth             = prefs.getBoolean ( USE_PROXY_AUTHENTICATION,       false );                               // NOI18N            
            username            = prefs.get        ( PROXY_AUTHENTICATION_USERNAME,  ""    );                               // NOI18N
            password            = prefs.get        ( PROXY_AUTHENTICATION_PASSWORD,  ""    );                               // NOI18N                

            notProxyHosts       = prefs.get        ( NOT_PROXY_HOSTS,                ""    ).replace("|", " ,");            // NOI18N                
            httpHost            = prefs.get        ( PROXY_HTTP_HOST,                ""    );                               // NOI18N                
            httpPort            = prefs.get        ( PROXY_HTTP_PORT,                ""    );                               // NOI18N                
            httpsHost           = prefs.get        ( PROXY_HTTPS_HOST,               ""    );                               // NOI18N                
            httpsPort           = prefs.get        ( PROXY_HTTPS_PORT,               ""    );                               // NOI18N                                            
        } else { // AUTO_DETECT_PROXY or DEFAULT
            useAuth             = false; // no way known yet!
            username            = "";                                                                                       // NOI18N
            password            = "";                                                                                       // NOI18N            
        
            notProxyHosts       = System.getProperty("http.nonProxyHosts", "");                                             // NOI18N            
            httpHost            = System.getProperty("http.proxyHost",     "");                                             // NOI18N            
            httpPort            = System.getProperty("http.proxyPort",     "");                                             // NOI18N                
            httpsHost           = System.getProperty("https.proxyHost",    "");                                             // NOI18N            
            httpsPort           = System.getProperty("https.proxyPort",    "");                                             // NOI18N            
        }
    }
    
    public boolean isDirect() {
        return proxyType.equals(DIRECT_CONNECTION);
    }

    public boolean isManualSetProxy() {
        return proxyType.equals(MANUAL_SET_PROXY);
    }
    
    public boolean hasAuth() {
        return useAuth;
    }
    
    public String getHttpHost() {
        return httpHost;
    }
    
    public int getHttpPort() {
        if(httpPort.equals("")) {
            return 8080;
        }
        return Integer.parseInt(httpPort);
    }

    public String getHttpsHost() {
        return httpsHost;
    }
    
    public int getHttpsPort() {
        if(httpsPort.equals("")) {
            return 443; 
        }
        return Integer.parseInt(httpsPort);
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }    
    
    public String getNotProxyHosts() {
        return notProxyHosts;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(! (obj instanceof GitProxySettings) ) {
            return false;
        } 
        GitProxySettings ps = (GitProxySettings) obj;        
        return ps.httpHost.equals(httpHost) &&
               ps.httpPort.equals(httpPort) &&
               ps.httpsHost.equals(httpsHost) &&
               ps.httpsPort.equals(httpsPort) &&
               ps.notProxyHosts.equals(notProxyHosts) &&
               ps.password.equals(password) &&
               ps.proxyType.equals(proxyType) &&
               ps.username.equals(username) &&
               ps.useAuth == useAuth;                   
    }
    
    @Override
    public String toString() {
        if(toString == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            sb.append(httpHost);
            sb.append(",");        
            sb.append(httpPort);
            sb.append(",");        
            sb.append(httpsHost);
            sb.append(",");        
            sb.append(httpsPort);
            sb.append(",");        
            sb.append(notProxyHosts);
            sb.append(",");        
            sb.append(password);
            sb.append(",");        
            sb.append(proxyType);
            sb.append(",");        
            sb.append(username);
            sb.append(",");        
            sb.append(useAuth);                
            sb.append("]");
            toString = sb.toString();
        }        
        return toString;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
    
}