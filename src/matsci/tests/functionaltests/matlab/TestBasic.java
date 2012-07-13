package functionaltests.matlab;

/**
 * TestBasic
 *
 * @author The ProActive Team
 */
public class TestBasic extends AbstractMatlabTest {
    @org.junit.Test
    public void run() throws Throwable {

        runCommand("TestBasic", 10);

    }
}
