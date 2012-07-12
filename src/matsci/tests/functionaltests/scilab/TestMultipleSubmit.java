package functionaltests.scilab;

/**
 * TestMultipleSubmit
 *
 * @author ProActive team
 */
public class TestMultipleSubmit extends AbstractScilabTest {
    @org.junit.Test
    public void run() throws Throwable {

        runCommand("TestMultipleSubmit", 10);

    }
}