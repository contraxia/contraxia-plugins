package com.github.rmee.kubernetes.helm;

import com.github.rmee.kubernetes.common.Client;
import com.github.rmee.kubernetes.common.ClientExtensionBase;
import com.github.rmee.kubernetes.common.internal.KubernetesUtils;
import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class HelmExtension extends ClientExtensionBase {

	private String tillerNamespace;

	private File sourceDir;

	private File outputDir;

	private File kubeConfig;

	public HelmExtension() {
		client = new Client(this, "helm") {

			@Override
			protected String computeDownloadFileName() {
				OperatingSystem operatingSystem = getOperatingSystem();
				String downloadFileName = "helm-v" + getVersion();
				if (operatingSystem.isLinux()) {
					return downloadFileName + "-linux-amd64.tar.gz";
				} else if (operatingSystem.isWindows()) {
					return downloadFileName + "-windows-amd64.zip";
				} else if (operatingSystem.isMacOsX()) {
					return downloadFileName + "-darwin-amd64.tar.gz";
				} else {
					throw new IllegalStateException("unknown operation system: " + operatingSystem.getName());
				}
			}

			@Override
			protected String computeDownloadUrl(String repository, String downloadFileName) {
				String downloadUrl = repository;
				if (!downloadUrl.endsWith("/")) {
					downloadUrl += "/";
				}
				return downloadUrl + downloadFileName;
			}
		};
		client.setImageName("dtzar/helm-kubectl");
		client.setVersion("2.8.2");
		client.setRepository("https://storage.googleapis.com/kubernetes-helm");
		client.setDockerized(true);
	}

	public Set<String> getPackageNames() {
		if (!getSourceDir().exists()) {
			throw new IllegalStateException("helm source directory does not exist: " + getSourceDir().getAbsolutePath());
		}
		Stream<File> stream = Arrays.stream(sourceDir.listFiles());
		return stream.filter(file -> file.isDirectory() && new File(file, "Chart.yaml").exists())
				.map(file -> file.getName())
				.collect(Collectors.toSet());

	}

	public File getPackageSourceDir(String packageName) {
		return new File(getSourceDir(), packageName);
	}

	public File getSourceDir() {
		init();
		return sourceDir;
	}

	public void setSourceDir(File sourceDir) {
		checkNotInitialized();
		this.sourceDir = sourceDir;
	}

	public File getOutputFile(String packageName) {
		File outputDir = getOutputDir();
		return new File(outputDir, packageName + "-" + project.getVersion() + ".tgz");
	}

	public File getOutputDir() {
		init();
		return outputDir;
	}

	public void setOutputDir(File outputDir) {
		checkNotInitialized();
		this.outputDir = outputDir;
	}

	public String getTillerNamespace() {
		init();
		return tillerNamespace;
	}

	public void setTillerNamespace(String tillerNamespace) {
		checkNotInitialized();
		this.tillerNamespace = tillerNamespace;

		client.getEnvironment().put("TILLER_NAMESPACE", tillerNamespace);
	}

	public void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		if (sourceDir == null) {
			sourceDir = new File(project.getProjectDir(), "src/main/helm/");
		}

		if (outputDir == null && !client.isDockerized()) {
			outputDir = new File(project.getBuildDir(), "distributions");
		}

		this.client.init(project);
	}

	public void exec(HelmExecSpec spec) {
		project.getLogger().warn("Executing: " + spec.getCommandLine());

		project.exec(execSpec -> {
			client.configureExec(execSpec, spec);
		});
	}

	protected void setProject(Project project) {
		this.project = project;
	}


	public File getKubeConfig() {
		return kubeConfig;
	}

	public void setKubeConfig(File kubeConfig) {
		this.kubeConfig = kubeConfig;
		KubernetesUtils.setKubeConfig(client, kubeConfig);
	}
}
