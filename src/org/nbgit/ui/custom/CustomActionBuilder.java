/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Jonas Fonseca <fonseca@diku.dk>
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file.
 *
 * This particular file is subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
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
package org.nbgit.ui.custom;

import java.io.File;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.eclipse.jgit.util.FS;

public class CustomActionBuilder {

    public enum Option {
        name,
        path,
        args,
        showOutput,
        showDirty,
        workDirRoot;
    }

    private final VCSContext context;
    private String name;
    private String args;
    private File path;
    private boolean showOutput = true;
    private boolean showDirty;
    private boolean repoSpecific;
    private boolean workDirRoot = true;

    protected CustomActionBuilder(VCSContext context) {
        this.context = context;
    }

    public static CustomActionBuilder newBuilder(VCSContext context) {
        return new CustomActionBuilder(context);
    }

    public CustomAction build() {
        return isValid() ? new CustomAction(this) : null;
    }

    public VCSContext getContext() {
        return context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path != null ? path.getAbsolutePath() : null;
    }

    public void setPath(String path) {
        if (path == null || path.length() == 0) {
            this.path = null;
        } else {
            this.path = new File(path);
        }
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public boolean isShowOutput() {
        return showOutput;
    }

    public void setShowOutput(boolean showOutput) {
        this.showOutput = showOutput;
    }

    public boolean isShowDirty() {
        return showDirty;
    }

    public void setShowDirty(boolean showDirty) {
        this.showDirty = showDirty;
    }

    private boolean canExecute(File file)
    {
        return !FS.INSTANCE.supportsExecute() || FS.INSTANCE.canExecute(file);
    }

    public boolean isValid() {
        return name != null && name.length() != 0 &&
               path != null && path.isFile() && canExecute(path);
    }

    public boolean isRepoSpecific() {
        return repoSpecific;
    }

    public boolean isWorkDirRoot() {
        return workDirRoot;
    }

    public void setRepoSpecific(boolean repoSpecific) {
        this.repoSpecific = repoSpecific;
    }

    public void setWorkDirRoot(boolean workDirRoot) {
        this.workDirRoot = workDirRoot;
    }

    private boolean toBoolean(String value) {
        if (value == null) {
            return false;
        }
        return value.equals("yes") || value.equals("true") || value.equals("1");
    }

    private String fromBoolean(boolean value) {
        return value ? "true" : "false";
    }

    public String getValue(Option option) {
        switch (option) {
        case name:
            return getName();
        case path:
            return getPath();
        case args:
            return getArgs();
        case showOutput:
            return fromBoolean(isShowOutput());
        case showDirty:
            return fromBoolean(isShowDirty());
        case workDirRoot:
            return fromBoolean(isWorkDirRoot());
        default:
            throw new UnsupportedOperationException(option.name() + " unknown!");
        }
    }

    public void setOption(Option option, String value) {
        switch (option) {
        case name:
            setName(value);
            break;
        case path:
            setPath(value);
            break;
        case args:
            setArgs(value);
            break;
        case showOutput:
            setShowOutput(toBoolean(value));
            break;
        case showDirty:
            setShowDirty(toBoolean(value));
            break;
        case workDirRoot:
            setWorkDirRoot(toBoolean(value));
            break;
        default:
            throw new UnsupportedOperationException(option.name() + " unknown!");
        }
    }

}
