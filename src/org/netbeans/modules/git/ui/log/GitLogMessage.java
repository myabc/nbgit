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
import java.util.Date;
import java.util.List;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileStatus;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.OutputLogger;


/**
 *
 * @author jr140578
 */
public class GitLogMessage {
    private char mod = 'M';
    private char add = 'A';
    private char del = 'R';
    private char copy = 'C';
    
    private List<GitLogMessageChangedPath> mpaths;
    private List<GitLogMessageChangedPath> apaths;
    private List<GitLogMessageChangedPath> dpaths;
    private List<GitLogMessageChangedPath> cpaths;
    private String rev;
    private String author;
    private String desc;
    private Date date;
    private String id;
    
    public GitLogMessage(String changeset){
    }
    
    public GitLogMessage( String rev, String auth, String desc, String date, String id, 
            String fm, String fa, String fd, String fc){
        String splits[];

        this.rev = rev;
        this.author = auth;
        this.desc = desc;
        splits = date.split(" ");
        this.date = new Date(Long.parseLong(splits[0]) * 1000); // UTC in miliseconds       
        this.id = id;
        this.mpaths = new ArrayList<GitLogMessageChangedPath>();
        this.apaths = new ArrayList<GitLogMessageChangedPath>();
        this.dpaths = new ArrayList<GitLogMessageChangedPath>();
        this.cpaths = new ArrayList<GitLogMessageChangedPath>();
        
        if( fm != null && !fm.equals("")){
            splits = fm.split(" ");
            for(String s: splits){
                this.mpaths.add(new GitLogMessageChangedPath(s, mod));             
                logCopied(s);
            }
        }
        if( fa != null && !fa.equals("")){
            splits = fa.split(" ");
            for(String s: splits){
                this.apaths.add(new GitLogMessageChangedPath(s, add));                
                logCopied(s);
            }
        }
        if( fd != null && !fd.equals("")){
            splits = fd.split(" ");
            for(String s: splits){
                this.dpaths.add(new GitLogMessageChangedPath(s, del));                
                logCopied(s);
            }
        }
        if( fc != null && !fc.equals("")){
            splits = fc.split(" ");
            for(String s: splits){
                this.cpaths.add(new GitLogMessageChangedPath(s, copy));                
                logCopied(s);
            }
        }
    }

    private void logCopied(String s){
        File file = new File(s);
        FileInformation fi = Git.getInstance().getFileStatusCache().getStatus(file);
        FileStatus fs = fi != null? fi.getStatus(file): null;
        if (fs != null && fs.isCopied()) {
            OutputLogger logger = OutputLogger.getLogger(Git.GIT_OUTPUT_TAB_TITLE);
            
            logger.outputInRed("*** Copied: " + s + " : " + fs.getFile() != null ? fs.getFile().getAbsolutePath() : "no filepath");
            logger.closeLog();
        }
    }
    
    public GitLogMessageChangedPath [] getChangedPaths(){
        List<GitLogMessageChangedPath> paths = new ArrayList<GitLogMessageChangedPath>();
        if(!mpaths.isEmpty()) paths.addAll(mpaths);
        if(!apaths.isEmpty()) paths.addAll(apaths);
        if(!dpaths.isEmpty()) paths.addAll(dpaths);
        if(!cpaths.isEmpty()) paths.addAll(cpaths);
        return paths.toArray(new GitLogMessageChangedPath[0]);
    }
    public String getRevision() {
        return rev;
    }
    public Date getDate() {
        return date;
    }
    public String getAuthor() {
        return author;
    }
    public String getCSetShortID() {
        return id;
    }
    public String getMessage() {
        return desc;
    }
    
    @Override
    public String toString(){
        String s = null;

        s = "rev: " + this.rev +
            "\nauthor: " + this.author +
            "\ndesc: " + this.desc +
            "\ndate: " + this.date +
            "\nid: " + this.id +
            "\nfm: " + this.mpaths +
            "\nfa: " + this.apaths +
            "\nfd: " + this.dpaths +
            "\nfc: " + this.cpaths;

        return s;
    }
}