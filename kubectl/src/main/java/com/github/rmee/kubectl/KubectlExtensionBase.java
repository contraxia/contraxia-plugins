package com.github.rmee.kubectl;

import com.github.rmee.cli.base.Cli;
import com.github.rmee.cli.base.ClientExtensionBase;
import com.github.rmee.cli.base.Credentials;
import com.github.rmee.cli.base.ExecResult;
import com.github.rmee.cli.base.OutputFormat;
import groovy.lang.Closure;
import org.gradle.api.Project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class KubectlExtensionBase extends ClientExtensionBase {

	private String url;

	protected Credentials credentials;

	private String namespace;

	private boolean insecureSkipTlsVerify = false;


	public KubectlExtensionBase() {
		credentials = new Credentials(this);
		cli = createClient();
	}

	protected abstract Cli createClient();

	public boolean isInsecureSkipTlsVerify() {
		return insecureSkipTlsVerify;
	}

	public void setInsecureSkipTlsVerify(boolean insecureSkipTlsVerify) {
		this.insecureSkipTlsVerify = insecureSkipTlsVerify;
	}


	public Credentials credentials(Closure closure) {
		return (Credentials) project.configure(credentials, closure);
	}

	public String getUrl() {
		init();
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Credentials getCredentials() {
		init();
		return credentials;
	}

	public String getNamespace() {
		init();
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getToken(String serviceAccount) {
		KubectlExecSpec spec = new KubectlExecSpec();
		spec.setCommandLine(getBinName() + " describe serviceaccount " + serviceAccount);
		ExecResult result = exec(spec);
		String tokenName = result.getProperty("tokens");

		spec.setCommandLine(getBinName() + " describe secret " + tokenName);
		result = exec(spec);
		return result.getProperty("token");
	}

	protected abstract String getBinName();

	public ExecResult exec(String command) {
		KubectlExecSpec spec = new KubectlExecSpec();
		spec.setCommandLine(command);
		return exec(spec);
	}

	protected ExecResult exec(KubectlExecSpec execSpec1) {
		// prevent from giving changes back to caller
		final KubectlExecSpec spec = execSpec1.duplicate();

		List<String> commandLine = spec.getCommandLine();
		int pipeIndex = commandLine.indexOf("|");
		if (pipeIndex == -1) {
			if (spec.getOutputFormat() == OutputFormat.JSON) {
				commandLine.add("--output=json");
			}

			if (!commandLine.contains("token") && !commandLine.contains("pass")) {
				project.getLogger().warn("Executing: " + commandLine.stream().collect(Collectors.joining(" ")));
			} else {
				project.getLogger().debug("Executing: " + commandLine.stream().collect(Collectors.joining(" ")));
			}

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				project.exec(execSpec -> {
					cli.configureExec(execSpec, spec);
					if (spec.getInput() != null) {
						execSpec.setStandardInput(new ByteArrayInputStream(spec.getInput().getBytes()));
					}
					if (spec.getOutputFormat() != OutputFormat.CONSOLE) {
						execSpec.setStandardOutput(outputStream);
					}
				});
				String output = outputStream.toString();
				return createResult(output);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		} else {
			KubectlExecSpec leftSpec = spec.duplicate();
			leftSpec.setCommandLine(commandLine.subList(0, pipeIndex));
			leftSpec.setOutputFormat(OutputFormat.TEXT);
			ExecResult leftResult = exec(leftSpec);

			KubectlExecSpec rightSpec = spec.duplicate();
			rightSpec.setCommandLine(commandLine.subList(pipeIndex + 1, commandLine.size()));
			rightSpec.setInput(leftResult.getText());
			return exec(rightSpec);
		}
	}

	protected ExecResult createResult(String output) {
		return new ExecResult(output);
	}

	protected void setProject(Project project) {
		this.project = project;
	}
}
