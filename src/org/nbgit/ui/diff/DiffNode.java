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
package org.nbgit.ui.diff;

import java.lang.reflect.InvocationTargetException;
import javax.swing.Action;
import org.nbgit.StatusInfo;
import org.nbgit.util.GitUtils;
import org.nbgit.util.HtmlFormatter;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.Lookups;

/**
 * Visible in the Search History Diff view.
 * 
 * @author Maros Sandor
 * @author alexbcoles
 */
class DiffNode extends AbstractNode {

	static final String COLUMN_NAME_NAME = "name";
	static final String COLUMN_NAME_PROPERTY = "property";
	static final String COLUMN_NAME_STATUS = "status";
	static final String COLUMN_NAME_LOCATION = "location";
	private final Setup setup;
	private String htmlDisplayName;

	public DiffNode(Setup setup)
	{
		super(Children.LEAF, Lookups.singleton(setup));
		this.setup = setup;
		setName(setup.getBaseFile().getName());
		initProperties();
		refreshHtmlDisplayName();
	}

	private void refreshHtmlDisplayName()
	{
		StatusInfo info = setup.getInfo();
		int status = info.getStatus();
		// Special treatment: Mergeable status should be annotated as Conflict in Versioning view according to UI spec
		if (status == StatusInfo.STATUS_VERSIONED_MERGE)
			status = StatusInfo.STATUS_VERSIONED_CONFLICT;
		htmlDisplayName = HtmlFormatter.getInstance().annotateNameHtml(setup.getBaseFile().getName(), info, null);
		fireDisplayNameChange(htmlDisplayName, htmlDisplayName);
	}

	@Override
	public String getHtmlDisplayName()
	{
		return htmlDisplayName;
	}

	public Setup getSetup()
	{
		return setup;
	}

	@Override
	public Action[] getActions(boolean context)
	{
		if (context)
			return null;
		return new Action[0];
	}

	private void initProperties()
	{
		Sheet sheet = Sheet.createDefault();
		Sheet.Set ps = Sheet.createPropertiesSet();

		ps.put(new NameProperty());
		ps.put(new LocationProperty());
		ps.put(new StatusProperty());
		if (setup.getPropertyName() != null)
			ps.put(new PropertyNameProperty());

		sheet.put(ps);
		setSheet(sheet);
	}

	private abstract class DiffNodeProperty extends PropertySupport.ReadOnly {

		@SuppressWarnings("unchecked")
		protected DiffNodeProperty(String name, Class type, String displayName, String shortDescription)
		{
			super(name, type, displayName, shortDescription);
		}

		@Override
		public String toString()
		{
			try {
				return getValue().toString();
			} catch (Exception e) {
				ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
				return e.getLocalizedMessage();
			}
		}

	}

	private class NameProperty extends DiffNodeProperty {

		public NameProperty()
		{
			super(COLUMN_NAME_NAME, String.class, COLUMN_NAME_NAME, COLUMN_NAME_NAME);
		}

		public Object getValue() throws IllegalAccessException, InvocationTargetException
		{
			return DiffNode.this.getName();
		}

	}

	private class PropertyNameProperty extends DiffNodeProperty {

		public PropertyNameProperty()
		{
			super(COLUMN_NAME_PROPERTY, String.class, COLUMN_NAME_PROPERTY, COLUMN_NAME_PROPERTY);
		}

		public Object getValue() throws IllegalAccessException, InvocationTargetException
		{
			return setup.getPropertyName();
		}

	}

	private class LocationProperty extends DiffNodeProperty {

		private String location;

		public LocationProperty()
		{
			super(COLUMN_NAME_LOCATION, String.class, COLUMN_NAME_LOCATION, COLUMN_NAME_LOCATION);
			location = GitUtils.getRelativePath(setup.getBaseFile());
			setValue("sortkey", location + "\t" + DiffNode.this.getName()); // NOI18N
		}

		public Object getValue() throws IllegalAccessException, InvocationTargetException
		{
			return location;
		}

	}

	private static final String[] zeros = new String[]{"", "00", "0", ""}; // NOI18N

	private class StatusProperty extends DiffNodeProperty {

		public StatusProperty()
		{
			super(COLUMN_NAME_STATUS, String.class, COLUMN_NAME_STATUS, COLUMN_NAME_STATUS);
			String shortPath = GitUtils.getRelativePath(setup.getBaseFile());
			String sortable = Integer.toString(GitUtils.getComparableStatus(setup.getInfo().getStatus()));
			setValue("sortkey", zeros[sortable.length()] + sortable + "\t" + shortPath + "\t" + DiffNode.this.getName().toUpperCase()); // NOI18N
		}

		public Object getValue() throws IllegalAccessException, InvocationTargetException
		{
			return setup.getInfo().getStatusText();
		}

	}

}
