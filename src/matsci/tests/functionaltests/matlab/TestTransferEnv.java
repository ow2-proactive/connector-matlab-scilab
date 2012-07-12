package functionaltests.matlab;

/**
 * TestTransferEnv
 *
 * @author ProActive team
 */
public class TestTransferEnv extends AbstractMatlabTest {
    @org.junit.Test
    public void run() throws Throwable {

        runCommand("TestTransferEnv", 10);

    }
}
