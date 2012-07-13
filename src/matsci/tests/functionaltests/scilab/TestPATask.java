package functionaltests.scilab;

/**
 * TestPATask
 *
 * @author ProActive team
 */
public class TestPATask extends AbstractScilabTest {
    @org.junit.Test
    public void run() throws Throwable {

        runCommand("TestPATask", 10);

    }
}