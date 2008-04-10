/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.test.git.main.commit;

import org.netbeans.junit.NbTestSuite;
import org.netbeans.jellytools.JellyTestCase;

/**
 * A Test based on JellyTestCase. JellyTestCase redirects Jemmy output 
 * to a log file provided by NbTestCase. It can be inspected in results. 
 * It also sets timeouts necessary for NetBeans GUI testing.
 *
 * Any JemmyException (which is normally thrown as a result of an unsuccessful
 * operation in Jemmy) going from a test is treated by JellyTestCase as a test
 * failure; any other exception - as a test error. 
 *
 * Additionally it:
 *    - closes all modal dialogs at the end of the test case (property jemmy.close.modal - default true)
 *    - generates component dump (XML file containing components information) in case of test failure (property jemmy.screen.xmldump - default false)
 *    - captures screen into a PNG file in case of test failure (property jemmy.screen.capture - default true)
 *    - waits at least 1000 ms between test cases (property jelly.wait.no.event - default true)
 *
 * @author alexbcoles
 */
public class CommitDataTest extends JellyTestCase {
    
    /** Default constructor.
     * @param testName name of particular test case
    */
    public CommitDataTest(String name) {
        super(name);
    }

    /** Creates suite from particular test cases. You can define order of testcases here. */
    public static NbTestSuite suite() {
        NbTestSuite suite = new NbTestSuite();
        suite.addTest(new CommitDataTest("test1"));
        suite.addTest(new CommitDataTest("test2"));
        return suite;
    }
    
    /* Method allowing test execution directly from the IDE. */
    public static void main(java.lang.String[] args) {
        // run whole suite
        junit.textui.TestRunner.run(suite());
        // run only selected test case
        //junit.textui.TestRunner.run(new CommitDataTest("test1"));
    }
    
    /** Called before every test case. */
    @Override
    public void setUp() {
        System.out.println("########  "+getName()+"  #######");
    }
    
    /** Called after every test case. */
    @Override
    public void tearDown() {
    }

    // Add test methods here, they have to start with 'test'.
    
    /** Test case 1. */
    public void test1() {
    }

    /** Test case 2. */
    public void test2() {
    }
}
