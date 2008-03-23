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
package org.netbeans.modules.git.ui.log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes log information for a file. This is the result of doing a
 * cvs log command. The fields in instances of this object are populated
 * by response handlers.
 *
 * @author Maros Sandor
 */
public class RepositoryRevision {

    private GitLogMessage message;

    private String repositoryRootUrl;

    /**
     * List of events associated with the revision.
     */ 
    private final List<Event> events = new ArrayList<Event>(1);

    public RepositoryRevision(GitLogMessage message, String rootUrl) {
        this.message = message;
        this.repositoryRootUrl = rootUrl;
        initEvents();
    }

    public String getRepositoryRootUrl() {
        return repositoryRootUrl;
    }

    private void initEvents() {
        GitLogMessageChangedPath [] paths = message.getChangedPaths();
        if (paths == null) return;
        for (GitLogMessageChangedPath path : paths) {
            events.add(new Event(path));
        }
    }

    public List<Event> getEvents() {
        return events;
    }

    public GitLogMessage getLog() {
        return message;
    }

    @Override
    public String toString() {        
        StringBuffer text = new StringBuffer();
        text.append(getLog().getRevision());
        text.append("\t");
        text.append(getLog().getDate());
        text.append("\t");
        text.append(getLog().getAuthor()); // NOI18N
        text.append("\n"); // NOI18N
        text.append(getLog().getMessage());
        return text.toString();
    }
    
    public class Event {
    
        /**
         * The file or folder that this event is about. It may be null if the File cannot be computed.
         */ 
        private File    file;
    
        private GitLogMessageChangedPath changedPath;

        private String name;
        private String path;

        public Event(GitLogMessageChangedPath changedPath) {
            this.changedPath = changedPath;
            name = changedPath.getPath().substring(changedPath.getPath().lastIndexOf('/') + 1);
            
            int indexPath = changedPath.getPath().lastIndexOf('/');
            if(indexPath > -1)
                path = changedPath.getPath().substring(0, indexPath);
            else
                path = "";
        }

        public RepositoryRevision getLogInfoHeader() {
            return RepositoryRevision.this;
        }

        public GitLogMessageChangedPath getChangedPath() {
            return changedPath;
        }

        /** Getter for property file.
         * @return Value of property file.
         */
        public File getFile() {
            return file;
        }

        /** Setter for property file.
         * @param file New value of property file.
         */
        public void setFile(File file) {
            this.file = file;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }
        
        @Override
        public String toString() {
            StringBuffer text = new StringBuffer();            
            text.append("\t");
            text.append(getPath());
            return text.toString();
        }
        
    }
}