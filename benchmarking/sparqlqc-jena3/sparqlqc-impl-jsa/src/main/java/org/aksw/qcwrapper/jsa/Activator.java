package org.aksw.qcwrapper.jsa;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import fr.inrialpes.tyrexmo.testqc.ContainmentSolver;
import fr.inrialpes.tyrexmo.testqc.simple.SimpleContainmentSolver;

public class Activator implements BundleActivator
{
    public void start(BundleContext context)
    {
        {
            Hashtable<String, String> meta = new Hashtable<>();
            meta.put("SHORT_LABEL", "JSAC");

            context.registerService(SimpleContainmentSolver.class, new ContainmentSolverWrapperJsaVarMapper(), meta);
        }

        {
            Hashtable<String, String> meta = new Hashtable<>();
            meta.put("SHORT_LABEL", "JSAI");

            context.registerService(ContainmentSolver.class, new ContainmentSolverWrapperJsaSubGraphIsomorphism(), meta);
        }

        {
            Hashtable<String, String> meta = new Hashtable<>();
            meta.put("SHORT_LABEL", "JSAG");

            context.registerService(ContainmentSolver.class, new ContainmentSolverWrapperJsaSubgraphIsomorphismIndex(), meta);
        }
    }

    public void stop(BundleContext context)
    {
        // NOTE: The service is automatically unregistered.
    }
}
