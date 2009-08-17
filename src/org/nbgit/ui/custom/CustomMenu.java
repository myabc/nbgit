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
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
import org.nbgit.Git;
import org.nbgit.GitModuleConfig;
import org.nbgit.ui.ContextMenu;
import org.nbgit.ui.custom.CustomActionBuilder.Option;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.util.NbBundle;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryConfig;

/**
 * Menu for custom actions.
 */
public class CustomMenu extends ContextMenu {

    private static final String preferencesPrefix = "action";

    public CustomMenu(VCSContext ctx, boolean mainMenu) {
        super(NbBundle.getMessage(CustomMenu.class, "CustomMenu"), ctx, mainMenu);
    }

    protected JMenu createMenu(VCSContext context, boolean mainMenu) {
        CustomActionBuilder builder = CustomActionBuilder.newBuilder(context);
        JMenu menu = new JMenu(this);

        if (addMainActions(menu, builder)) {
            menu.addSeparator();
        }
        if (addRepositoryActions(menu, builder)) {
            menu.addSeparator();
        }

        builder = new CustomWizardActionBuilder(context);
        builder.setRepoSpecific(!mainMenu);
        String name = mainMenu
            ? NbBundle.getMessage(CustomMenu.class, "NewMainAction") // NOI18N
            : NbBundle.getMessage(CustomMenu.class, "NewRepoAction"); // NOI18N
        menu.add(new CustomWizardAction(name, builder));
        org.openide.awt.Mnemonics.setLocalizedText(menu, NbBundle.getMessage(CustomMenu.class, "CustomMenu")); // NOI18N

        return menu;
    }

    private RepositoryConfig getRepositoryConfig(VCSContext context) {
        File root = GitUtils.getRootFile(context);
        Repository repo = Git.getInstance().getRepository(root);

        return repo == null ? null : repo.getConfig();
    }

    public boolean addRepositoryActions(JMenu menu, CustomActionBuilder builder) {
        RepositoryConfig config = getRepositoryConfig(builder.getContext());
        int actions = 0;

        for (String action : config.getSubsections("nbgit")) {
            if (action.startsWith("action") && load(builder, config, action)) {
                menu.add(builder.build());
                actions++;
            }
        }

        return actions > 0;
    }

    public boolean addMainActions(JMenu menu, CustomActionBuilder builder) {
        Preferences prefs = GitModuleConfig.getDefault().getPreferences();
        HashSet<String> seen = new HashSet<String>();
        String[] keys;

        try {
            keys = prefs.keys();
        } catch (BackingStoreException ex) {
            return false;
        }

        for (String key : keys) {
            int cutOffset = key.indexOf(".");
            if (key.startsWith(preferencesPrefix) && cutOffset != -1) {
                key = key.substring(0, cutOffset);
                if (!seen.contains(key) && load(builder, prefs, key)) {
                    menu.add(builder.build());
                    seen.add(key);
                }
            }
        }

        return !seen.isEmpty();
    }

    private String getPreferencesName(Option option, int i) {
        return preferencesPrefix + i + "." + option.name();
    }

    public boolean load(CustomActionBuilder builder, RepositoryConfig config, String subsection) {
        for (Option option : Option.values()) {
            String value = config.getString("nbgit", subsection, option.name());
            builder.setOption(option, value);
        }
        return builder.isValid();
    }

    public boolean load(CustomActionBuilder builder, Preferences prefs, String key) {
        for (Option option : Option.values()) {
            String value = prefs.get(key + "." + option.name(), null);
            builder.setOption(option, value);
        }
        return builder.isValid();
    }

    private class CustomWizardActionBuilder extends CustomActionBuilder {

        CustomWizardActionBuilder(VCSContext context) {
            super(context);
        }

        @Override
        public CustomAction build() {
            if (!isValid()) {
                return null;
            }

            int i = 0;
            if (isRepoSpecific()) {
                RepositoryConfig config = getRepositoryConfig(getContext());
                Collection<String> subsections = config.getSubsections("nbgit");

                while (subsections.contains("action" + i)) {
                    i++;
                }

                for (Option option : Option.values()) {
                    String value = this.getValue(option);
                    config.setString("nbgit", "action" + i, option.name(), value);
                }

                try {
                    config.save();
                } catch (IOException ex) {
                }
            } else {
                Preferences prefs = GitModuleConfig.getDefault().getPreferences();

                while (prefs.get(getPreferencesName(Option.name, i), null) != null) {
                    i++;
                }

                for (Option option : Option.values()) {
                    prefs.put(getPreferencesName(option, i), getValue(option));
                }

                try {
                    prefs.sync();
                } catch (BackingStoreException ex) {
                }
            }

            return super.build();
        }

    }

}
