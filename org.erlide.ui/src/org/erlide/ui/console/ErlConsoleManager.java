package org.erlide.ui.console;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.erlide.core.ErlangCore;
import org.erlide.core.backend.Backend;
import org.erlide.core.backend.BackendListener;
import org.erlide.core.backend.RpcCallSite;
import org.erlide.core.backend.runtimeinfo.RuntimeInfo;
import org.erlide.core.common.IDisposable;
import org.erlide.jinterface.ErlLogger;

public class ErlConsoleManager implements IDisposable, BackendListener {
    private final Map<Backend, IConsole> consoles;
    private final IConsoleManager conMan;

    public ErlConsoleManager() {
        consoles = new HashMap<Backend, IConsole>();

        final ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
        conMan = consolePlugin.getConsoleManager();

        ErlangCore.getBackendManager().addBackendListener(this);
    }

    public void runtimeAdded(final Backend b) {
        if (b == null || !b.getRuntimeInfo().hasConsole()) {
            return;
        }
        final RuntimeInfo info = b.getRuntimeInfo();
        ErlLogger.debug("console ADDED " + b + " " + info);
        final ErlangConsole console = new ErlangConsole(b);
        conMan.addConsoles(new IConsole[] { console });
        consoles.put(b, console);
    }

    public void runtimeRemoved(final Backend b) {
        ErlLogger.debug("console REMOVED " + b + " " + b.getRuntimeInfo());
        final IConsole console = consoles.get(b);
        if (console == null) {
            return;
        }
        conMan.removeConsoles(new IConsole[] { console });
    }

    public void dispose() {
        ErlangCore.getBackendManager().removeBackendListener(this);
    }

    public void moduleLoaded(final RpcCallSite backend, final IProject project,
            final String moduleName) {
    }
}
