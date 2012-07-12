package functionaltests.matlab;

/**
 * TestPATask
 *
 * @author ProActive team
 */
public class TestPATask extends AbstractMatlabTest {
    @org.junit.Test
    public void run() throws Throwable {

        runCommand("TestPATask", 10);

    }
}