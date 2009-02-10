package org.erlide.core;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.erlide.runtime.PreferencesUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class NewErlangProjectProperties {

	private static final String SOURCES = "sources";
	private static final String BACKEND_COOKIE = "backendCookie";
	private static final String BACKEND_NODE_NAME = "backendNodeName";
	private static final String REQUIRED_BACKEND_VERSION = "requiredBackendVersion";
	private static final String OUTPUT = "output";
	private static final String INCLUDES = "includes";

	private List<SourceLocation> sources = new ArrayList<SourceLocation>();
	private List<String> includes = new ArrayList<String>();
	private String output;
	private Map<String, String> compilerOptions = new HashMap<String, String>();
	private List<ProjectLocation> projects = new ArrayList<ProjectLocation>();
	private List<LibraryLocation> libraries = new ArrayList<LibraryLocation>();
	private List<WeakReference<DependencyLocation>> codePathOrder = new ArrayList<WeakReference<DependencyLocation>>();
	private String requiredRuntimeVersion;
	private String backendNodeName;
	private String backendCookie;

	public NewErlangProjectProperties() {
		output = "ebin";
		// TODO fixme
	}

	public NewErlangProjectProperties(ErlangProjectProperties old) {
		requiredRuntimeVersion = old.getRuntimeVersion();
		if (requiredRuntimeVersion == null) {
			requiredRuntimeVersion = old.getRuntimeInfo().getVersion();
		}
		backendCookie = old.getCookie();
		backendNodeName = old.getNodeName();

		sources = mkSources(old.getSourceDirs());
		includes = PreferencesUtils.unpackList(old.getIncludeDirsString());
		output = old.getOutputDir();
		compilerOptions.put("debug_info", "true");

		String exmodf = old.getExternalModulesFile();
		String exmod = PreferencesUtils.readFile(exmodf);
		List<String> externalModules = PreferencesUtils.unpackList(exmod, "\n");
		List<SourceLocation> sloc = makeSourceLocations(externalModules);

		String exincf = old.getExternalModulesFile();
		String exinc = PreferencesUtils.readFile(exincf);
		List<String> externalIncludes = PreferencesUtils.unpackList(exinc);

		LibraryLocation loc = new LibraryLocation(sloc, externalIncludes, null,
				null);
		libraries.add(loc);
	}

	private List<SourceLocation> makeSourceLocations(
			List<String> externalModules) {
		List<SourceLocation> result = new ArrayList<SourceLocation>();

		List<String> modules = new ArrayList<String>();
		for (String mod : externalModules) {
			if (mod.endsWith(".erlidex")) {
				String str = PreferencesUtils.readFile(mod);
				List<String> mods = PreferencesUtils.unpackList(str, "\n");
				modules.addAll(mods);
			} else {
				modules.add(mod);
			}
		}

		Map<String, Map<String, List<String>>> grouped = new HashMap<String, Map<String, List<String>>>();
		for (String mod : modules) {
			int i = mod.indexOf('/');
			i = mod.indexOf('/', i + 1);
			String loc = mod.substring(1, i);
			mod = mod.substring(i + 1);
			i = mod.lastIndexOf('/');
			String path = mod.substring(1, i);
			String file = mod.substring(i + 1);

			System.out.println("FOUND: '" + loc + "' '" + path + "' '" + file
					+ "'");
			Map<String, List<String>> val = grouped.get(loc);
			if (val == null) {
				val = new HashMap<String, List<String>>();
			}
			List<String> pval = val.get(path);
			if (pval == null) {
				pval = new ArrayList<String>();
			}
			pval.add(file);
			val.put(path, pval);
			grouped.put(loc, val);

		}
		System.out.println(grouped);

		return result;
	}

	private List<SourceLocation> mkSources(String[] sourceDirs) {
		List<SourceLocation> result = new ArrayList<SourceLocation>();
		for (String src : sourceDirs) {
			result.add(new SourceLocation(src, null, null, null, null, null));
		}
		return result;
	}

	public Collection<SourceLocation> getSources() {
		return Collections.unmodifiableCollection(sources);
	}

	public Collection<String> getIncludes() {
		return Collections.unmodifiableCollection(includes);
	}

	public String getOutput() {
		return output;
	}

	public Map<String, String> getCompilerOptions() {
		return Collections.unmodifiableMap(compilerOptions);
	}

	public Collection<ProjectLocation> getProjects() {
		return Collections.unmodifiableCollection(projects);
	}

	public Collection<LibraryLocation> getLibraries() {
		return Collections.unmodifiableCollection(libraries);
	}

	public Collection<WeakReference<DependencyLocation>> getCodePathOrder() {
		return Collections.unmodifiableCollection(codePathOrder);
	}

	public String getRequiredRuntimeVersion() {
		return requiredRuntimeVersion;
	}

	public String getBackendNodeName() {
		return backendNodeName;
	}

	public String getBackendCookie() {
		return backendCookie;
	}

	public void load(IEclipsePreferences root) throws BackingStoreException {
		output = root.get(OUTPUT, "ebin");
		requiredRuntimeVersion = root.get(REQUIRED_BACKEND_VERSION, null);
		backendNodeName = root.get(BACKEND_NODE_NAME, null);
		backendCookie = root.get(BACKEND_COOKIE, null);
		includes = PreferencesUtils.unpackList(root.get(INCLUDES, ""));
		Preferences srcNode = root.node(SOURCES);
		sources.clear();
		for (String src : srcNode.childrenNames()) {
			IEclipsePreferences sn = (IEclipsePreferences) srcNode.node(src);
			SourceLocation loc = new SourceLocation(sn);
			sources.add(loc);
		}
	}

	public void store(IEclipsePreferences root) throws BackingStoreException {
		PreferencesUtils.clearAll(root);
		root.put(OUTPUT, output);
		if (requiredRuntimeVersion != null) {
			root.put(REQUIRED_BACKEND_VERSION, requiredRuntimeVersion);
		}
		if (backendNodeName != null) {
			root.put(BACKEND_NODE_NAME, backendNodeName);
		}
		if (backendCookie != null) {
			root.put(BACKEND_COOKIE, backendCookie);
		}
		root.put(INCLUDES, PreferencesUtils.packList(includes));
		Preferences srcNode = root.node(SOURCES);
		for (SourceLocation loc : sources) {
			loc.store((IEclipsePreferences) srcNode.node(Integer.toString(loc
					.getId())));
		}

		root.flush();
	}

}
