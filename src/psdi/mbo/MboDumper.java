package psdi.mbo;

import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import psdi.util.MXException;
import psdi.util.logging.MXLogger;

/**
 * Dump the in-memory mbo (key and modified fields only) and its children.
 *
 * @author Martin Nichol
 *
 */
public class MboDumper {

    /**
     * Compares two mbos based on mbo name and unique id value.
     * @author Martin Nichol
     *
     */
    private class MboComparator implements Comparator<MboRemote> {

        @Override
        public int compare(MboRemote object1, MboRemote object2) {
            if ((object1 == null) && (object2 == null)) {
                return 0;
            } else if ((object1 == null) && (object2 != null)) {
                return -1;
            } else if ((object1 != null) && (object2 == null)) {
                return 1;
            } else {
                try {
                    int rc = object1.getName().compareTo(object2.getName());
                    if (rc == 0) {
                        long d = object1.getUniqueIDValue() - object2.getUniqueIDValue();
                        if (d < 0) {
                            rc = -1;
                        } else if (d > 0) {
                            rc = 1;
                        } else {
                            rc = 0;
                        }
                    }
                    return rc;
                } catch (RemoteException e) {
                    throw new RuntimeException("Caught Remote Exception", e);
                } catch (MXException e) {
                    throw new RuntimeException("Caught MXException", e);
                }
            }
        }

    }

    /** a comparator for comparing mbos. */
    private MboComparator comparator = new MboComparator();

    /** the set of mbos seen. */
    private TreeSet<MboRemote> seenMbos = new TreeSet<MboRemote>(comparator);

    /** the set of mbos seen more than once. */
    private TreeSet<MboRemote> dupMbos = new TreeSet<MboRemote>(comparator);

    /**
     * Private constructor to prevent instatiation.
     */
    private MboDumper() {
        super();
    }

    /**
     * Dump the mbo and all its children.
     *
     * @param logger the logger to use.
     * @param mbo the mbo to dump.
     * @throws MXException if a Maximo problem occurs.
     * @throws RemoteException if an RMI problem occurs.
     */
    public static void dump(MXLogger logger, MboRemote mbo) throws MXException, RemoteException {
        MboDumper dumper = new MboDumper();
        dumper.findDupMbos((Mbo)mbo);
        logger.info("*** MBO Data Dump Start");
        dumper.dumpMbo(logger, "", (Mbo) mbo);
        logger.info("*** MBO Data Dump Done");
    }

    /**
     * Find any duplicate mbos.
     *
     * @param mbo the mbo and its children to test.
     * @throws RemoteException if an RMI problem occurs.
     */
    private void findDupMbos(Mbo mbo) throws RemoteException {
        if (mbo.toBeSaved()) {
            if (seenMbos.contains(mbo)) {
                dupMbos.add(mbo);
            }
            seenMbos.add(mbo);
        }

        Map<String, MboSetRemote> mbosets = mbo.getRelatedSets();
        for (Map.Entry<String, MboSetRemote> me : mbosets.entrySet()) {
            MboSet children = (MboSet) me.getValue();
            Iterator<Mbo> i = children.mboVec.iterator();
            while (i.hasNext()) {
                Mbo child = i.next();
                findDupMbos(child);
            }
        }
    }

    /**
     * Dump the mbo and all its children.
     *
     * @param logger the logger to use.
     * @param indent the indentation level to use.
     * @param mbo the mbo to dump.
     * @throws MXException if a Maximo problem occurs.
     * @throws RemoteException if an RMI problem occurs.
     */
    private void dumpMbo(MXLogger logger, String indent, Mbo mbo) throws MXException, RemoteException {
        String dup = dupMbos.contains(mbo) ? "<=== DUPLICATE MBO" : "";
        String flags = determineFlags(mbo);
        logger.info(String.format("%sMbo: %s [%s] %s", indent, mbo.getName(), flags, dup));
        dumpKeys(logger, indent, mbo);
        dumpModifiedAttributes(logger, indent, mbo);
        dumpRelatedMboSets(logger, indent, mbo);
    }

    /**
     * Determine any flags set on the mbo.
     * @param mbo the mbo to test.
     * @return a string representing the flags set.
     * @throws RemoteException if an RMI problem occurs.
     */
    private String determineFlags(Mbo mbo) throws RemoteException {
        StringBuilder sb = new StringBuilder();
        if (mbo.toBeAdded()) {
            sb.append("ToBeAdded ");
        }
        if (mbo.toBeDeleted()) {
            sb.append("ToBeDeleted ");
        }
        if (mbo.toBeSaved()) {
            sb.append("ToBeSaved ");
        }
        if (mbo.toBeUpdated()) {
            sb.append("ToBeUpdated ");
        }
        if (mbo.isFlagSet(MboConstants.READONLY)) {
            sb.append("READONLY ");
        }
        if (mbo.isFlagSet(MboConstants.DISCARDABLE)) {
            sb.append("DISCARDABLE ");
        }
        if (mbo.isFlagSet(MboConstants.NOSAVE)) {
            sb.append("NOSAVE ");
        }
        return sb.toString();
    }

    /**
     * Dump the mbo's key field and its values.
     *
     * @param logger the logger to use.
     * @param indent the indentation level to use.
     * @param mbo the mbo to dump.
     * @throws MXException if a Maximo problem occurs.
     * @throws RemoteException if an RMI problem occurs.
     */
    private void dumpKeys(MXLogger logger, String indent, Mbo mbo) throws MXException, RemoteException {
        MboSetInfo msi = mbo.getMboSetInfo();
        String[] keys = msi.getKeyAttributes();
        for (String key : keys) {
            logger.info(String.format("%sKey %s: %s", indent, key, mbo.getString(key)));
        }
    }

    /**
     * Dump the modified attributes of an mbo.
     *
     * @param logger the logger to use.
     * @param indent the indentation level to use.
     * @param mbo the mbo to dump.
     * @throws MXException if a Maximo problem occurs.
     * @throws RemoteException if an RMI problem occurs.
     */
    @SuppressWarnings("unchecked")
    private void dumpModifiedAttributes(MXLogger logger, String indent, Mbo mbo) throws MXException, RemoteException {
        MboSetInfo msi = mbo.getMboSetInfo();
        Iterator<MboValueInfo> i = msi.getAttributes();
        while (i.hasNext()) {
            MboValueInfo mvi = i.next();
            String attributeName = mvi.getAttributeName();
            if (!mvi.isKey() && mbo.isModified(attributeName)) {
                logger.info(String.format("%sModified %s: %s", indent, attributeName, mbo.getString(attributeName)));
            }
        }
    }

    /**
     * Dump the instantiated related mbosets and any instantiated mbos.
     *
     * @param logger the logger to use.
     * @param indent the indentation level to use.
     * @param mbo the mbo to dump.
     * @throws MXException if a Maximo problem occurs.
     * @throws RemoteException if an RMI problem occurs.
     */
    private void dumpRelatedMboSets(MXLogger logger, String indent, Mbo mbo) throws MXException, RemoteException {
        Map<String, MboSetRemote> mbosets = mbo.getRelatedSets();
        for (Map.Entry<String, MboSetRemote> me : mbosets.entrySet()) {
            String relationship = me.getKey();
            MboSet children = (MboSet) me.getValue();
            logger.info(String.format("%sRelationship: %s %s", indent, relationship, children.getRelationship()));
            Iterator<Mbo> i = children.mboVec.iterator();
            while (i.hasNext()) {
                String newIndent = indent + "   ";
                Mbo child = i.next();
                dumpMbo(logger, newIndent, child);
            }
        }
    }
}
