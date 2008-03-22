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
import org.openide.util.NbBundle;

/**
 * We could have used URL with custom protocol handler for ssh 
 * (see http://java.sun.com/developer/onlineTraining/protocolhandlers/index.html)
 * but that is overkill for what we want which is a string to represent the URL.
 *
 * @author Padraig O'Briain
 */
class GitURL {

  private static final char SEGMENT_SEPARATOR = '/';
    
    private String protocol;
    private String host;
    private String password;
    private int port;
    
    public GitURL(String gitUrl)  throws MalformedURLException {
        if (gitUrl == null) 
            throw new MalformedURLException(NbBundle.getMessage(GitURL.class, "MSG_URL_NULL")); // NOI18N
        parseUrl(gitUrl);
    }

    /**
     * verifies that url is correct
     * @throws malformedURLException
     */
    private void parseUrl(String gitUrl) throws MalformedURLException {
        String parsed = gitUrl;

        int hostIdx = parsed.indexOf("://");                         // NOI18N
        if (hostIdx == -1)
            throw new MalformedURLException(NbBundle.getMessage(GitURL.class, "MSG_INVALID_URL", gitUrl)); // NOI18N
        protocol = parsed.substring(0, hostIdx).toLowerCase();

        if ((!protocol.equalsIgnoreCase("http")) && // NOI18N
            (!protocol.equalsIgnoreCase("https")) && // NOI18N
            (!protocol.equalsIgnoreCase("file")) && // NOI18N
            (!protocol.equalsIgnoreCase("static-http")) && // NOI18N
            (!protocol.equalsIgnoreCase("ssh")) ) { // NOI18N
                throw new MalformedURLException(NbBundle.getMessage(GitURL.class, "MSG_INVALID_URL", gitUrl)); // NOI18N
        }
        parsed = parsed.substring(hostIdx + 3);
        if (parsed.length() == 0) {
            throw new MalformedURLException(NbBundle.getMessage(GitURL.class, "MSG_INVALID_URL", gitUrl)); // NOI18N
        }
    }

    /**
     * get the protocol
     * @return either http, https, file, static-http, ssh
     */ 
    public String getProtocol() {
        return protocol;
    }
    
}