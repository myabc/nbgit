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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.netbeans.modules.git.VersionsCache;
import org.openide.ErrorManager;
import org.openide.text.CloneableEditorSupport;


/**
 * Defines numb read-only File environment.
 *
 * @author Maros Sandor
 */
public abstract class FileEnvironment implements CloneableEditorSupport.Env {

    /** Serial Version UID */
    private static final long serialVersionUID = 1L;
    
    private String mime = "text/plain";  // NOI18N
    
    private final File peer;

    private final String revision;

    private transient Date modified;
        
    /** Creates new StreamEnvironment */
    public FileEnvironment(File baseFile, String revision, String mime) {
        if (baseFile == null) throw new NullPointerException();
        peer = baseFile;
        modified = new Date();
        this.revision = revision;
        if (mime != null) {
            this.mime = mime;
        }
    }
        
    public void markModified() throws java.io.IOException {
        throw new IOException("r/o"); // NOI18N
    }    
    
    public void unmarkModified() {
    }    

    public void removePropertyChangeListener(java.beans.PropertyChangeListener propertyChangeListener) {
    }
    
    public boolean isModified() {
        return false;
    }
    
    public java.util.Date getTime() {
        return modified;
    }
    
    public void removeVetoableChangeListener(java.beans.VetoableChangeListener vetoableChangeListener) {
    }
    
    public boolean isValid() {
        return true;
    }
    
    public java.io.OutputStream outputStream() throws java.io.IOException {
        throw new IOException("r/o"); // NOI18N
    }

    public java.lang.String getMimeType() {
        return mime;
    }

    /**
     * Always return fresh stream.
     */
    public java.io.InputStream inputStream() throws java.io.IOException {
        return new LazyInputStream();
    }

    private class LazyInputStream extends InputStream {

        private InputStream in;

        public LazyInputStream() {
        }

        private InputStream peer() throws IOException {
            try {
                if (in == null) {
                    File remoteFile = VersionsCache.getInstance().getFileRevision(peer, revision);
                    in = new FileInputStream(remoteFile);                    
                }
                return in;
            } catch (IOException ex) {
                ErrorManager err = ErrorManager.getDefault();
                IOException ioex = new IOException();
                err.annotate(ioex, ex);
                err.annotate(ioex, ErrorManager.USER, null, null, null, null);
                throw ioex;
            }
        }

        @Override
        public int available() throws IOException {
            return peer().available();
        }

        @Override
        public void close() throws IOException {
            peer().close();
        }

        @Override
        public void mark(int readlimit) {
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        public int read() throws IOException {
            return peer().read();
        }

        @Override
        public int read(byte b[]) throws IOException {
            return peer().read(b);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            return peer().read(b, off, len);
        }

        @Override
        public void reset() throws IOException {
            peer().reset();
        }

        @Override
        public long skip(long n) throws IOException {
            return peer().skip(n);
        }

    }

    public void addVetoableChangeListener(java.beans.VetoableChangeListener vetoableChangeListener) {
    }
    
    public void addPropertyChangeListener(java.beans.PropertyChangeListener propertyChangeListener) {
    }
            
}