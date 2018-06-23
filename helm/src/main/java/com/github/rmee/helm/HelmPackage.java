package com.github.rmee.helm;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

public class HelmPackage extends DefaultTask {

	@TaskAction
	public void exec() {
		HelmExtension extension = getProject().getExtensions().getByType(HelmExtension.class);
		File outputDir = extension.getOutputDir();
		outputDir.mkdirs();

		Project project = getProject();

		Set<String> packageNames = extension.getPackageNames();
		for (String packageName : packageNames) {
			HelmExecSpec spec = new HelmExecSpec();
			File sourceDir = extension.getPackageSourceDir(packageName);
			spec.setCommandLine("helm package " + sourceDir.getAbsolutePath() + " --destination " + outputDir.getAbsolutePath()
					+ " --version " + project.getVersion());

			extension.exec(spec);
		}
	}

	@InputDirectory
	public File getSourceDir() {
		HelmExtension extension = getProject().getExtensions().getByType(HelmExtension.class);
		return extension.getSourceDir();
	}

	@OutputFiles
	public Set<File> getOutputFiles() {
		HelmExtension extension = getProject().getExtensions().getByType(HelmExtension.class);
		Set<String> packageNames = extension.getPackageNames();
		return packageNames.stream()
				.map(name -> extension.getOutputFile(name))
				.collect(Collectors.toSet());
	}
}
