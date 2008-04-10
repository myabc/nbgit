/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.test.git.main.commit;

import java.io.File;
import java.io.PrintStream;
import javax.swing.table.TableModel;
import junit.textui.TestRunner;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jellytools.OutputOperator;
import org.netbeans.jellytools.OutputTabOperator;
import org.netbeans.jellytools.modules.javacvs.VersioningOperator;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jellytools.nodes.SourcePackagesNode;
import org.netbeans.jemmy.EventTool;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.test.git.utils.TestKit;

/**
 *
 * @author peter pis
 * @author alexbcoles
 */
public class IgnoreTest extends JellyTestCase {
    
    public static final String PROJECT_NAME = "JavaApp";
    public File projectPath;
    public PrintStream stream;
    String os_name;
    
    /** Creates a new instance of IgnoreTest */
    public IgnoreTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {        
        os_name = System.getProperty("os.name");
        //System.out.println(os_name);
        System.out.println("### "+getName()+" ###");
        
    }
    
    protected boolean isUnix() {
        boolean unix = false;
        if (os_name.indexOf("Windows") == -1) {
            unix = true;
        }
        return unix;
    }
    
    public static void main(String[] args) {
        // TODO code application logic here
        TestRunner.run(suite());
    }
    
    public static NbTestSuite suite() {
        NbTestSuite suite = new NbTestSuite();
        suite.addTest(new IgnoreTest("testIgnoreUnignoreFile"));
        //suite.addTest(new IgnoreTest("testFinalRemove"));
        return suite;
    }
    
    public void testIgnoreUnignoreFile() throws Exception {
        //JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentTimeout", 30000);
        //JemmyProperties.setCurrentTimeout("DialogWaiter.WaitDialogTimeout", 30000);    
        try {
            TestKit.showStatusLabels();
            TestKit.closeProject(PROJECT_NAME);
            
            OutputOperator oo = OutputOperator.invoke();
            
            stream = new PrintStream(new File(getWorkDir(), getName() + ".log"));
            TestKit.loadOpenProject(PROJECT_NAME, getDataDir());

            TestKit.createNewElement(PROJECT_NAME, "javaapp", "NewClass");
            Node node = new Node(new SourcePackagesNode(PROJECT_NAME), "javaapp|NewClass");
            node.performPopupAction("Mercurial|Ignore");
            OutputTabOperator oto = new OutputTabOperator("Mercurial");
            oto.waitText("INFO: End of Ignore");
            
            node = new Node(new SourcePackagesNode(PROJECT_NAME), "javaapp|NewClass");
            
            org.openide.nodes.Node nodeIDE = (org.openide.nodes.Node) node.getOpenideNode();
            String color = TestKit.getColor(nodeIDE.getHtmlDisplayName());
            String status = TestKit.getStatus(nodeIDE.getHtmlDisplayName());
            assertEquals("Wrong color of node - file color should be ignored!!!", TestKit.IGNORED_COLOR, color);
            assertEquals("Wrong annotation of node - file status should be ignored!!!", TestKit.IGNORED_STATUS, status);
            
            node = new Node(new SourcePackagesNode(PROJECT_NAME), "javaapp|NewClass");
            TimeoutExpiredException tee = null;
            try {
                node.performPopupAction("Mercurial|Ignore");
            } catch (Exception e) {
                tee = (TimeoutExpiredException) e;
            }
            assertNotNull("Ignore action should be disabled!!!", tee);
            
            //unignore file
            oto = new OutputTabOperator("Mercurial");
            oto.clear();
            node = new Node(new SourcePackagesNode(PROJECT_NAME), "javaapp|NewClass");
            node.performPopupAction("Mercurial|Unignore");
            oto.waitText("INFO: End of Unignore");
            node = new Node(new SourcePackagesNode(PROJECT_NAME), "javaapp|NewClass");
            nodeIDE = (org.openide.nodes.Node) node.getOpenideNode();
            color = TestKit.getColor(nodeIDE.getHtmlDisplayName());
            status = TestKit.getStatus(nodeIDE.getHtmlDisplayName());
            assertEquals("Wrong color of node - file color should be new!!!", TestKit.NEW_COLOR, color);
            assertEquals("Wrong annotation of node - file status should be new!!!", TestKit.NEW_STATUS, status);
            
            //verify content of Versioning view
            node = new Node(new SourcePackagesNode(PROJECT_NAME), "javaapp|NewClass");
            node.performPopupAction("Mercurial|Status");
            new EventTool().waitNoEvent(1000);
            VersioningOperator vo = VersioningOperator.invoke();
            TableModel model = vo.tabFiles().getModel();
            assertEquals("Versioning view should be empty", 1, model.getRowCount());
            assertEquals("File should be listed in Versioning view", "NewClass.java", model.getValueAt(0, 0).toString());
            
            stream.flush();
            stream.close();
            
        } catch (Exception e) {
            throw new Exception("Test failed: " + e);
        } finally {
            TestKit.closeProject(PROJECT_NAME);
        }    
    }
    
    public void testFinalRemove() throws Exception {
        TestKit.finalRemove();
    }
}
