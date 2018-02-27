package psdi.mbo;

import java.rmi.RemoteException;

import org.apache.log4j.Level;
import org.junit.Test;

import com.interlocsolutions.maximo.junit.MaximoTestHarness;

import junitx.framework.StringAssert;
import psdi.util.MXException;
import psdi.util.logging.FixedLoggers;
import psdi.util.logging.MXLogger;

public class MboDumperTest extends MaximoTestHarness {

    @Test
    public void testMboNoUpdateNoDups() throws RemoteException, MXException {
        MXLogger logger = FixedLoggers.MAXIMOLOGGER;
        logger.setLevel(Level.INFO);

        MboSetRemote maxvars1 = getMboSet("MAXVARS");
        MboRemote maxvar1 = maxvars1.getMbo(0);

        MboSetRemote maxvars2 = maxvar1.getMboSet("$alt", "MAXVARS", "maxvarsid = :maxvarsid");
        @SuppressWarnings("unused")
		MboRemote maxvar2 = maxvars2.getMbo(0);

        MboDumper.dump(logger, maxvar1);

        StringAssert.assertNotContains("<===", getLogCapture());
    }

    @Test
    public void testMboUpdatesDups() throws RemoteException, MXException {
        MXLogger logger = FixedLoggers.MAXIMOLOGGER;
        logger.setLevel(Level.INFO);

        MboSetRemote maxvars1 = getMboSet("MAXVARS");
        MboRemote maxvar1 = maxvars1.getMbo(0);
        maxvar1.setValue("VARVALUE", "JUnit");

        MboSetRemote maxvars2 = maxvar1.getMboSet("$alt", "MAXVARS", "maxvarsid = :maxvarsid");
        MboRemote maxvar2 = maxvars2.getMbo(0);
        maxvar2.setValue("VARVALUE", "JUnit");

        MboDumper.dump(logger, maxvar1);

        StringAssert.assertContains("<===", getLogCapture());
    }

    @Test
    public void testMboUpdatesNoDups() throws RemoteException, MXException {
        MXLogger logger = FixedLoggers.MAXIMOLOGGER;
        logger.setLevel(Level.INFO);

        MboSetRemote maxvars1 = getMboSet("MAXVARS");
        MboRemote maxvar1 = maxvars1.getMbo(0);
        maxvar1.setValue("VARVALUE", "JUnit");

        MboSetRemote maxvars2 = maxvar1.getMboSet("$alt", "MAXVARS", "maxvarsid = :maxvarsid");
        maxvars2.setFlag(MboConstants.NOSAVE, true);
        MboRemote maxvar2 = maxvars2.getMbo(0);
        maxvar2.setValue("VARVALUE", "JUnit");

        MboDumper.dump(logger, maxvar1);

        StringAssert.assertNotContains("<===", getLogCapture());
    }

}
