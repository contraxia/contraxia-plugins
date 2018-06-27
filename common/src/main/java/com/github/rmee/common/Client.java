package com.github.rmee.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.process.ExecSpec;

public abstract class Client {

	private ClientExtensionBase extension;

	private String version;

	private Map<String, String> environment = new HashMap();

	private boolean download = true;

	private String binPath;

	private boolean dockerized = true;

	private String imageName;

	private String repository;

	private String downloadUrl;

	private String downloadFileName;

	private String binName;

	private File downloadDir;

	private File installDir;

	private OperatingSystem operatingSystem;

	private Map<String, File> volumeMappings = new HashMap<>();

	private Map<Integer, Integer> portMappings = new HashMap<>();

	private boolean useWrapper = true;

	public Client(ClientExtensionBase extension, String binName) {
		this.binName = binName;
		this.extension = extension;

		String proxyHostName = System.getProperty("http.proxyHost");
		String proxyPort = System.getProperty("http.proxyPort");
		String proxyUrl;
		if (proxyHostName == null) {
			proxyUrl = System.getenv("HTTP_PROXY");
		}
		else {
			proxyUrl = "http://" + proxyHostName + ":" + proxyPort;
		}
		if (proxyUrl != null) {
			environment.put("HTTP_PROXY", proxyUrl);
		}
	}

	/**
	 * Mapping from docker path to host path (the other way around compared to docker to ease configuration)
	 */
	public Map<String, File> getVolumeMappings() {
		return volumeMappings;
	}

	public void setVolumeMappings(Map<String, File> volumeMappings) {
		this.volumeMappings = volumeMappings;
	}

	public Map<Integer, Integer> getPortMappings() {
		return portMappings;
	}

	public void setPortMappings(Map<Integer, Integer> portMappings) {
		this.portMappings = portMappings;
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public String getVersion() {
		extension.init();
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isDockerized() {
		return dockerized;
	}

	public void setDockerized(boolean dockerized) {
		this.dockerized = dockerized;
	}

	public OperatingSystem getOperatingSystem() {
		checkNotDockerized();
		return operatingSystem;
	}

	public void setOperationSystem(OperatingSystem operatingSystem) {
		checkNotDockerized();
		this.operatingSystem = operatingSystem;
	}

	public void init(Project project) {
		if (!dockerized) {
			if (downloadDir == null && download) {
				downloadDir = new File(project.getBuildDir(), "tmp/" + binName + "/v" + version);
				downloadDir.mkdirs();
			}
			if (installDir == null) {
				installDir = new File(project.getBuildDir(), "/kubernetes/");
			}
			if (operatingSystem == null) {
				operatingSystem = org.gradle.internal.os.OperatingSystem.current();
			}

			if (binPath == null) {
				String binSuffix = getBinSuffix();
				binPath = new File(installDir, binName + binSuffix).getAbsolutePath();
				if (downloadUrl == null && download) {
					downloadFileName = computeDownloadFileName();
					downloadUrl = computeDownloadUrl(repository, downloadFileName);
				}
			}
		}
	}

	protected abstract String computeDownloadFileName();

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	protected String getBinSuffix() {
		checkNotDockerized();
		return operatingSystem.isWindows() ? ".exe" : "";
	}

	protected abstract String computeDownloadUrl(String repository, String downloadFileName);

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public File getInstallDir() {
		checkNotDockerized();
		extension.init();
		return installDir;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public void setInstallDir(File installDir) {
		this.installDir = installDir;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public File getDownloadDir() {
		extension.init();
		return downloadDir;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public void setDownloadDir(File downloadDir) {
		checkNotDockerized();
		this.downloadDir = downloadDir;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public String getRepository() {
		checkNotDockerized();
		extension.init();
		return repository;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public void setRepository(String repository) {
		this.repository = repository;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public String getDownloadUrl() {
		extension.init();
		return downloadUrl;
	}

	protected String getDownloadFileName() {
		checkNotDockerized();
		extension.init();
		return downloadFileName;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public boolean getDownload() {
		extension.init();
		return download;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public void setDownload(boolean download) {
		checkNotDockerized();
		this.download = download;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public String getBinPath() {
		extension.init();
		return binPath;
	}

	public void setBinPath(String binPath) {
		checkNotDockerized();
		this.binPath = binPath;
	}

	/**
	 * @deprecated move to dockerized version
	 */
	@Deprecated
	public File getDownloadedFile() {
		extension.init();
		return new File(downloadDir, getDownloadFileName());
	}

	public String getBinName() {
		return binName;
	}


	private void checkNotDockerized() {
		if (dockerized) {
			throw new IllegalStateException("not necssary when in dockerized mode");
		}
	}

	public void configureExec(ExecSpec execSpec, ClientExecSpec clientExecSpec) {
		Map<String, String> execEnv = new HashMap<>();
		execEnv.putAll(environment);

		execSpec.setEnvironment(execEnv);
		execSpec.setIgnoreExitValue(clientExecSpec.isIgnoreExitValue());

		List<String> args = clientExecSpec.getCommandLine();
		if (dockerized) {
			List<String> commandLine = new ArrayList<>();
			commandLine.addAll(buildBaseCommandLine());
			commandLine.addAll(args);
			System.out.println("Executing: " + commandLine);
			execSpec.setCommandLine(commandLine);
		}
		else {
			args.set(0, getBinPath());
			execSpec.setCommandLine(args);
		}

		File stdoutFile = clientExecSpec.getStdoutFile();
		if (stdoutFile != null) {
			try {
				if (stdoutFile.exists() && !stdoutFile.delete()) {
					throw new IllegalStateException("failed to delete " + stdoutFile);
				}
				execSpec.setStandardOutput(new FileOutputStream(stdoutFile));
			}
			catch (FileNotFoundException e) {
				throw new IllegalStateException("failed to redirect helm stdout: " + e.getMessage(), e);
			}
		}

	}

	private Collection<String> buildBaseCommandLine() {
		List<String> commandLine = new ArrayList<>();
		commandLine.add("docker");
		commandLine.add("run");
		commandLine.add("-i");

		for (Map.Entry<String, String> entry : environment.entrySet()) {
			commandLine.add("-e");
			commandLine.add(entry.getKey() + "=" + entry.getValue());
		}

		for (Map.Entry<String, File> entry : volumeMappings.entrySet()) {
			String path = entry.getValue().getAbsolutePath();

			// fix path issues with windows
			path = path.replace('\\', '/');

			commandLine.add("-v");
			commandLine.add(path + ":" + entry.getKey());

			File file = entry.getValue();
			file.mkdirs();
		}

		for (Map.Entry<Integer, Integer> entry : portMappings.entrySet()) {
			commandLine.add("-p");
			commandLine.add(entry.getValue() + ":" + entry.getKey());
		}

		if (version == null) {
			throw new IllegalStateException("no version specified");
		}

		commandLine.add(imageName + ":" + version);
		return commandLine;
	}

	public boolean useWrapper() {
		return useWrapper;
	}

	public void setWrapper(boolean useWrapper) {
		this.useWrapper = useWrapper;
	}

	public void setupWrapper(Project project) {
		setupWrapper(project, true);
	}

	public void setupWrapper(Project project, boolean mustIncludeBinary) {
		if (useWrapper()) {
			Project rootProject = project.getRootProject();
			Task wrapper = rootProject.getTasks().getByName("wrapper");
			wrapper.doLast(task -> {
				StringBuilder builder = new StringBuilder();

				builder.append("#!/usr/bin/env sh\n");
				builder.append("exec");
				Collection<String> commandLine = buildBaseCommandLine();

				for (String element : commandLine) {
					builder.append(' ');

					// no relative paths support on windows yet
					/*
					String rootPath = rootProject.getProjectDir().getAbsolutePath();
					String projectPath = project.getProjectDir().getAbsolutePath();
					if (element.startsWith(projectPath)) {
						element = element.substring(projectPath.length() + 1);
					}
					else if (element.startsWith(rootPath)) {
						element = element.substring(rootPath.length() + 1);
						Project p = project;
						while (p != rootProject) {
							element = "../" + element;
							p = p.getParent();
						}
					}
					*/
					builder.append(element);
				}
				if (mustIncludeBinary) {
					builder.append(' ');
					builder.append(binName);
				}
				builder.append(" \"$@\"\n");

				File file = new File(project.getProjectDir(), binName);
				try (FileWriter writer = new FileWriter(file)) {
					writer.write(builder.toString());
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});
		}
	}
}
