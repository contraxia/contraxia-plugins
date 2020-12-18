package com.github.rmee.helm;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskDependency;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class HelmPackage extends DefaultTask implements PublishArtifact {

	private String packageName;

	private Map<String, Object> values = new HashMap<>();

	public HelmPackage() {
		getOutputs().cacheIf(task -> true);
	}

	private void cleanOutputDir(File outputDir) {
		if (outputDir.exists()) {
			for (File file : outputDir.listFiles()) {
				if (file.getName().startsWith(packageName) && file.getName().endsWith(".tgz")) {
					file.delete();
				}
			}
		}
	}

	@TaskAction
	public void exec() {
		HelmExtension extension = getProject().getExtensions().getByType(HelmExtension.class);

		String outputDir;
		if (extension.getCli().isDockerized()) {
			outputDir = HelmPlugin.HELM_OUTPUT_DIR;
			cleanOutputDir(new File(getProject().getBuildDir(), "helm"));
		} else {
			File fileOutputDir = extension.getOutputDir();
			fileOutputDir.mkdirs();
			outputDir = fileOutputDir.getAbsolutePath();
			cleanOutputDir(fileOutputDir);
		}

		Project project = getProject();

		File sourceDir = getSourceDir();

		File templatedDir = new File(getProject().getBuildDir(), "tmp/helm/" + sourceDir.getName());
		try {
			FileUtils.deleteDirectory(templatedDir);
			templatedDir.mkdirs();
			FileUtils.copyDirectory(sourceDir, templatedDir);

			applyValues(templatedDir);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}


		// consider start supporting dependencies
		//File requirementsFile = new File(sourceDir, "requirements.yaml");
		//if (requirementsFile.exists() && requirementsFile.length() > 0) {
		//    HelmExecSpec updateSpec = new HelmExecSpec();
		//    updateSpec.setCommandLine("helm dependency update --skip-refresh --debug " + sourceDir.getAbsolutePath());
		//    extension.exec(updateSpec);
		//}


		HelmExecSpec packageSpec = new HelmExecSpec();
		packageSpec.setCommandLine("helm package " + templatedDir.getAbsolutePath() + " --destination " + outputDir
				+ " --version " + project.getVersion());
		extension.exec(packageSpec);
	}

	private void applyValues(File templatedDir) throws IOException {
		File valuesFile = new File(templatedDir, "values.yaml");


		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
		Map data;
		try (FileReader reader = new FileReader(valuesFile)) {
			data = yaml.load(reader);
		}

		for (Map.Entry<String, Object> entry : values.entrySet()) {
			List<String> key = Arrays.asList(entry.getKey().split("\\."));
			putValue(data, key, entry.getValue());
		}

		try (FileWriter writer = new FileWriter(valuesFile)) {
			yaml.dump(data, writer);
		}
	}

	private void putValue(Map data, List<String> key, Object value) {
		String keyElement = key.get(0);
		if (key.size() == 1) {
			data.put(keyElement, value);
		} else {
			Object innerData = data.computeIfAbsent(keyElement, o -> new HashMap<>());
			putValue((Map) innerData, key.subList(1, key.size()), value);
		}
	}


	@InputDirectory
	public File getSourceDir() {
		HelmExtension extension = getProject().getExtensions().getByType(HelmExtension.class);
		return new File(extension.getSourceDir(), packageName);
	}

	@OutputFile
	public File getOutputFile() {
		HelmExtension extension = getProject().getExtensions().getByType(HelmExtension.class);
		return extension.getOutputFile(packageName);
	}

	@Input
	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	@Input
	public Map<String, Object> getValues() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}

	@Override
	public String getExtension() {
		return "tgz";
	}

	@Override
	public String getType() {
		return "tgz";
	}

	@Nullable
	@Override
	public String getClassifier() {
		return "helmPackage";
	}

	@Override
	public File getFile() {
		return getOutputFile();
	}

	@Nullable
	@Override
	public Date getDate() {
		return new Date(getOutputFile().lastModified());
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return task -> new HashSet<>(Arrays.asList(this));
	}
}
