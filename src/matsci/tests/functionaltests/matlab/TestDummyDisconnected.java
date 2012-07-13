package functionaltests.matlab;

/**
 * TestDummyDisconnected
 *
 * @author ProActive team
 */
public class TestDummyDisconnected extends AbstractMatlabTest {
    @org.junit.Test
    public void run() throws Throwable {

        runCommand("TestObjectArguments", 10);

    }
}
