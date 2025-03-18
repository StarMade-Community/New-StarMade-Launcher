package smlauncher;

import smlauncher.starmade.GameBranch;
import smlauncher.starmade.IndexFileEntry;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

/**
 * Stores all the versions of the game under by branch.
 *
 * @author SlavSquatSuperstar
 */
public class VersionRegistry {

	private final Map<GameBranch, List<IndexFileEntry>> branchVersions;

	public VersionRegistry() {
		branchVersions = new HashMap<>();
	}

	// Read Version Method

	/**
	 * Reads all game versions for each branch into the registry.
	 *
	 * @throws IOException if the URL of versions cannot be read.
	 */
	public void createRegistry() throws IOException {
		for(GameBranch branch : GameBranch.values()) {
			if(branch == GameBranch.ARCHIVE) continue; // don't run archive versions

			List<IndexFileEntry> versions = readVersions(branch);
			branchVersions.put(branch, versions);
		}
	}

	private List<IndexFileEntry> readVersions(GameBranch branch) throws IOException {
		// Open URL connection
		URL url = new URL(branch.url);
		URLConnection openConnection = url.openConnection();
		openConnection.setConnectTimeout(10000);
		openConnection.setReadTimeout(10000);
		openConnection.setRequestProperty("User-Agent", "StarMade-Updater");

		// Read all versions
		List<IndexFileEntry> versions = new ArrayList<>();
		try(BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(openConnection.getInputStream()), StandardCharsets.UTF_8))) {
			String line;
			while((line = in.readLine()) != null) {
				IndexFileEntry entry = IndexFileEntry.create(line, branch);
				versions.add(entry);
			}
			// Sort versions from old to recent
			versions.sort(Collections.reverseOrder());
		} catch(Exception e) {
			System.out.println("Could not read versions list");
			throw new IOException(e);
		}
		openConnection.getInputStream().close();

		if(branch == GameBranch.DEV) { // Remove old dev versions
			versions.removeIf(v -> v.build.startsWith("2017"));
		}
		return versions;
	}

	// Get Version Methods

	/**
	 * Search all versions in the registry matching a given criteria.
	 *
	 * @param query the criteria
	 * @return the version, or null if not found
	 */
	public IndexFileEntry searchForVersion(Predicate<IndexFileEntry> query) {
		for(List<IndexFileEntry> list : branchVersions.values()) {
			for(IndexFileEntry entry : list) {
				if(query.test(entry)) return entry;
			}
		}
		return null;
	}

	/**
	 * Search all versions under a specific branch matching a given criteria.
	 *
	 * @param branch the branch
	 * @param query  the criteria
	 * @return the version, or null if not found
	 */
	public IndexFileEntry searchForVersion(GameBranch branch, Predicate<IndexFileEntry> query) {
		List<IndexFileEntry> versions = branchVersions.get(branch);
		if(versions == null) return null;
		for(IndexFileEntry entry : versions) {
			if(query.test(entry)) return entry;
		}
		return null;
	}

	/**
	 * Get all versions associated with the given branch.
	 *
	 * @param branch a game branch
	 * @return the list of versions
	 */
	public List<IndexFileEntry> getVersions(GameBranch branch) {
		return branchVersions.get(branch);
	}

	/**
	 * Get all versions in the registry by grouped by branch.
	 *
	 * @return the set all version lists
	 */
	public Collection<List<IndexFileEntry>> getAllVersions() {
		return branchVersions.values();
	}

	/**
	 * Get the most recent version associated with the given branch.
	 *
	 * @param branch a game branch
	 * @return the list of versions
	 */
	public IndexFileEntry getLatestVersion(GameBranch branch) {
		List<IndexFileEntry> versions = branchVersions.get(branch);
		if(versions != null) return versions.get(0);
		return null;
	}

}
