package com.github.rmee.helm;

import com.github.rmee.cli.base.CliExecSpec;
import com.github.rmee.cli.base.internal.CliExecBase;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public class HelmExec extends CliExecBase {

	private HelmExecSpec spec = new HelmExecSpec();

	public HelmExec() {
		setGroup("kubernetes");

		dependsOn("helmBootstrap");
	}

	@TaskAction
	public void exec() {
		HelmExtension extension = getProject().getExtensions().getByType(HelmExtension.class);
		extension.exec(spec);
	}

	@Input
	@Override
	protected CliExecSpec retrieveSpec() {
		return spec;
	}
}
