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
package org.netbeans.modules.git.config;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import org.ini4j.Ini;
import org.netbeans.modules.git.Git;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

/**
 *
 * Handles the Git <b>hgrc</b> configuration file.</br>
 *
 * @author Padraig O'Briain
 */
public class GitConfigFiles {
   
    public static final String HG_EXTENSIONS = "extensions";  // NOI18N
    public static final String HG_EXTENSIONS_HGK = "hgext.hgk";  // NOI18N
    public static final String HG_EXTENSIONS_FETCH = "fetch";  // NOI18N
    public static final String HG_UI_SECTION = "ui";  // NOI18N
    public static final String HG_USERNAME = "username";  // NOI18N
    public static final String HG_PATHS_SECTION = "paths";  // NOI18N
    public static final String HG_DEFAULT_PUSH = "default-push";  // NOI18N
    public static final String HG_DEFAULT_PUSH_VALUE = "default-push";  // NOI18N
    public static final String HG_DEFAULT_PULL = "default-pull";  // NOI18N
    public static final String HG_DEFAULT_PULL_VALUE = "default";  // NOI18N

    /** The HgConfigFiles instance for user and system defaults */
    private static GitConfigFiles instance;

    /** the Ini instance holding the configuration values stored in the <b>hgrc</b>
     * file used by the Mercurial module */    
    private Ini hgrc = null;
    
    /** The repository directory if this instance is for a repository */
    private File dir;
    private static final String WINDOWS_USER_APPDATA = getAPPDATA();
    private static final String WINDOWS_CONFIG_DIR = WINDOWS_USER_APPDATA + "\\Mercurial";                                      // NOI18N
    private static final String WINDOWS_GLOBAL_CONFIG_DIR = getGlobalAPPDATA() + "\\Mercurial";                                 // NOI18N
    public static final String HG_RC_FILE = "hgrc";                                                                       // NOI18N
    public static final String HG_REPO_DIR = ".hg";                                                                       // NOI18N
    
    /**
     * Creates a new instance
     */
    private GitConfigFiles() {      
        // get the system hgrc file 
        hgrc = loadFile(HG_RC_FILE);                                           
    }
    
    /**
     * Returns a singleton instance
     *
     * @return the HgConfiles instance
     */
    public static GitConfigFiles getInstance() {
        if (instance == null) {
            instance = new GitConfigFiles();
        }
        return instance;
    }

    public GitConfigFiles(File file) {
        dir = file;
        hgrc = loadFile(file, HG_RC_FILE);
    }
 
    public void setProperty(String name, String value) {
        if (name.equals(HG_USERNAME)) { 
            setProperty(HG_UI_SECTION, HG_USERNAME, value); 
        } else if (name.equals(HG_DEFAULT_PUSH)) { 
            setProperty(HG_PATHS_SECTION, HG_DEFAULT_PUSH_VALUE, value); 
        } else if (name.equals(HG_DEFAULT_PULL)) { 
            setProperty(HG_PATHS_SECTION, HG_DEFAULT_PULL_VALUE, value); 
        } else if (name.equals(HG_EXTENSIONS_HGK)) { 
            // Allow hgext.hgk to be set to some other user defined value if required
            if(getProperty(HG_EXTENSIONS, HG_EXTENSIONS_HGK).equals("")){
                setProperty(HG_EXTENSIONS, HG_EXTENSIONS_HGK, value, true); 
            }
        } else if (name.equals(HG_EXTENSIONS_FETCH)) { 
            // Allow fetch to be set to some other user defined value if required
            if(getProperty(HG_EXTENSIONS, HG_EXTENSIONS_FETCH).equals("")){ 
                setProperty(HG_EXTENSIONS, HG_EXTENSIONS_FETCH, value, true);
            }
        }

    }
 
    public void setProperty(String section, String name, String value, boolean allowEmpty) {
        if (!allowEmpty) {
            if (value.length() == 0) {
                removeProperty(section, name);
            } else {
                Ini.Section inisection = getSection(hgrc, section, true);
                inisection.put(name, value);
            }
        } else {
            Ini.Section inisection = getSection(hgrc, section, true);
            inisection.put(name, value);
        }
        storeIni(hgrc, HG_RC_FILE); 
    }

    public void setProperty(String section, String name, String value) {
        setProperty(section, name,value, false);
    }

    public void setUserName(String value) {
        setProperty(HG_UI_SECTION, HG_USERNAME, value); 
    }

    public String getUserName() {
        return getUserName(true);
    }

    public Properties getProperties(String section) {
        Ini.Section inisection = getSection(hgrc, section, false);
        Properties props = new Properties();
        if (inisection != null) {
            Set<String> keys = inisection.keySet();
            for (String key : keys) {
                props.setProperty(key, inisection.get(key));
            }
        }
        return props;
    }

    public void clearProperties(String section) {
        Ini.Section inisection = getSection(hgrc, section, false);
        if (inisection != null) {
             inisection.clear();
             storeIni(hgrc, HG_RC_FILE); 
         }
    }

    public void removeProperty(String section, String name) {
        Ini.Section inisection = getSection(hgrc, section, false);
        if (inisection != null) {
             inisection.remove(name);
             storeIni(hgrc, HG_RC_FILE); 
         }
    }

    public String getDefaultPull(Boolean reload) {
        if (reload) {
            doReload();
        }
        return getProperty(HG_PATHS_SECTION, HG_DEFAULT_PULL_VALUE); 
    }

    public String getDefaultPush(Boolean reload) {
        if (reload) {
            doReload();
        }
        String value = getProperty(HG_PATHS_SECTION, HG_DEFAULT_PUSH); 
        if (value.length() == 0) {
            value = getProperty(HG_PATHS_SECTION, HG_DEFAULT_PULL_VALUE); 
        }
        return value;
    }

    public String getUserName(Boolean reload) {
        if (reload) {
            doReload();
        }
        return getProperty(HG_UI_SECTION, HG_USERNAME);                                              
    }

    public String getProperty(String section, String name) {
        Ini.Section inisection = getSection(hgrc, section, true);
        String value = inisection.get(name);
        return value != null ? value : "";        // NOI18N 
    }
    
    public boolean containsProperty(String section, String name) {
        Ini.Section inisection = getSection(hgrc, section, true);
        return inisection.containsKey(name);
    }

    private void doReload () {
        if (dir == null) {
            hgrc = loadFile(HG_RC_FILE);                                            
        } else {
            hgrc = loadFile(dir, HG_RC_FILE);                                      
        }
    }

    private Ini.Section getSection(Ini ini, String key, boolean create) {
        Ini.Section section = ini.get(key);
        if(section == null && create) {
            return ini.add(key);
        }
        return section;
    }
    
    private void storeIni(Ini ini, String iniFile) {
        try {
            String filePath;
            if (dir != null) {
                filePath = dir.getAbsolutePath() + File.separator + HG_REPO_DIR + File.separator + iniFile; // NOI18N 
            } else {
                filePath =  getUserConfigPath() + iniFile;
            }
            File file = FileUtil.normalizeFile(new File(filePath));
            file.getParentFile().mkdirs();
            ini.store(new BufferedOutputStream(new FileOutputStream(file)));
        } catch (IOException ex) {
            Git.LOG.log(Level.INFO, null, ex);
        }
    }    

    /**
     * Returns the path for the Mercurial configuration directory
     *
     * @return the path
     *
     */ 
    public static String getUserConfigPath() {        
        if(Utilities.isUnix()) {
            String path = System.getProperty("user.home") ;                     // NOI18N
            return path + "/.";                                // NOI18N
        } else if (Utilities.isWindows()){
            return WINDOWS_CONFIG_DIR + "/"; // NOI18N 
        } 
        return "";                                                              // NOI18N
    }

    private Ini loadFile(File dir, String fileName) {
        String filePath = dir.getAbsolutePath() + File.separator + HG_REPO_DIR + File.separator + fileName; // NOI18N 
        File file = FileUtil.normalizeFile(new File(filePath));
        Ini system = null;
        try {            
            system = new Ini(new FileReader(file));
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            Git.LOG.log(Level.INFO, null, ex); 
        }

        if(system == null) {
            system = new Ini();
            Git.LOG.log(Level.WARNING, "Could not load the file " + filePath + ". Falling back on hg defaults."); // NOI18N
        }
        return system;
    }
    /**
     * Loads the configuration file  
     * The settings are loaded and merged together in the folowing order:
     * <ol>
     *  <li> The per-user configuration file, i.e ~/.hgrc
     *  <li> The system-wide file, i.e. /etc/mercurial/hgrc
     * </ol> 
     *
     * @param fileName the file name
     * @return an Ini instance holding the configuration file. 
     */       
    private Ini loadFile(String fileName) {
        // config files from userdir
        String filePath = getUserConfigPath() + fileName;
        File file = FileUtil.normalizeFile(new File(filePath));
        Ini system = null;
        try {            
            system = new Ini(new FileReader(file));
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            Git.LOG.log(Level.INFO, null, ex); 
        }

        if(system == null) {
            system = new Ini();
            Git.LOG.log(Level.WARNING, "Could not load the file " + filePath + ". Falling back on hg defaults."); // NOI18N
        }
        
        Ini global = null;      
        try {
            global = new Ini(new FileReader(getGlobalConfigPath() + File.separator + fileName));   // NOI18N
        } catch (FileNotFoundException ex) {
            // just doesn't exist - ignore
        } catch (IOException ex) {
            Git.LOG.log(Level.INFO, null, ex); 
        }
         
        if(global != null) {
            merge(global, system);
        }                
        return system;
    }

    /**
     * Merges only sections/keys/values into target which are not already present in source
     * 
     * @param source the source ini file
     * @param target the target ini file in which the values from the source file are going to be merged
     */
    private void merge(Ini source, Ini target) {
        for (Iterator<String> itSections = source.keySet().iterator(); itSections.hasNext();) {
            String sectionName = itSections.next();
            Ini.Section sourceSection = source.get( sectionName );
            Ini.Section targetSection = target.get( sectionName );

            if(targetSection == null) {
                targetSection = target.add(sectionName);
            }

            for (Iterator<String> itVariables = sourceSection.keySet().iterator(); itVariables.hasNext();) {
                String key = itVariables.next();

                if(!targetSection.containsKey(key)) {
                    targetSection.put(key, sourceSection.get(key));
                }
            }            
        }
    }
   
    /**
     * Return the path for the systemwide command lines configuration directory 
     */
    private static String getGlobalConfigPath () {
        if(Utilities.isUnix()) {
            return "/etc/mercurial";               // NOI18N
        } else if (Utilities.isWindows()){
            return WINDOWS_GLOBAL_CONFIG_DIR;
        } 
        return "";                                  // NOI18N
    }

    /**
     * Returns the value for the %APPDATA% env variable on windows
     *
     */
    private static String getAPPDATA() {
        String appdata = ""; // NOI18N
        if(Utilities.isWindows()) {
            appdata = System.getenv("APPDATA");// NOI18N
        }
        return appdata!= null? appdata: ""; // NOI18N
    }

    /**
     * Returns the value for the %ALLUSERSPROFILE% + the last foder segment from %APPDATA% env variables on windows
     *
     */
    private static String getGlobalAPPDATA() {
        if(Utilities.isWindows()) {
            String globalProfile = System.getenv("ALLUSERSPROFILE");                                // NOI18N
            if(globalProfile == null || globalProfile.trim().equals("")) {                          // NOI18N
                globalProfile = "";                                                                 // NOI18N
            }
            String appdataPath = WINDOWS_USER_APPDATA;
            if(appdataPath == null || appdataPath.equals("")) {                                     // NOI18N
                return "";                                                                          // NOI18N
            }
            String appdata = "";                                                                    // NOI18N
            int idx = appdataPath.lastIndexOf("\\");                                                // NOI18N
            if(idx > -1) {
                appdata = appdataPath.substring(idx + 1);
                if(appdata.trim().equals("")) {                                                     // NOI18N
                    int previdx = appdataPath.lastIndexOf("\\", idx);                               // NOI18N
                    if(idx > -1) {
                        appdata = appdataPath.substring(previdx + 1, idx);
                    }
                }
            } else {
                return "";                                                                          // NOI18N
            }
            return globalProfile + "/" + appdata;                                                   // NOI18N
        }
        return "";                                                                                  // NOI18N
    }
    
}