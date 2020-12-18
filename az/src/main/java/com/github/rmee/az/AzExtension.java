package com.github.rmee.az;

import com.github.rmee.az.aks.AksConfiguration;
import com.github.rmee.cli.base.Cli;
import com.github.rmee.cli.base.ClientExtensionBase;
import com.github.rmee.cli.base.internal.CliDownloadStrategy;
import groovy.lang.Closure;
import org.gradle.api.Project;

public class AzExtension extends ClientExtensionBase {

	private AksConfiguration aks = new AksConfiguration();

	private String subscriptionId;

	private String userName;

	private String password;

	private String tenantId;

	private String resourceGroup;

	private boolean servicePrincipal;

	public AzExtension() {
		CliDownloadStrategy downloadStrategy = new CliDownloadStrategy() {
			@Override
			public String computeDownloadFileName(Cli cli) {
				throw new UnsupportedOperationException("download not supported, make use of docker");
			}

			@Override
			public String computeDownloadUrl(Cli cli, String repository, String downloadFileName) {
				throw new UnsupportedOperationException("download not supported, make use of docker");
			}
		};
		cli = new Cli("az", downloadStrategy, () -> project);
	}

	public void exec(AzExecSpec spec) {
		cli.exec(spec);
	}

	public void exec(Closure<AzExecSpec> closure) {
		AzExecSpec spec = new AzExecSpec();
		project.configure(spec, closure);
		exec(spec);
	}

	public void aks(Closure<AksConfiguration> closure) {
		project.configure(aks, closure);
	}

	public AksConfiguration getAks() {
		return aks;
	}

	public void setAks(AksConfiguration aks) {
		this.aks = aks;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String clientId) {
		this.userName = clientId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public void setResourceGroup(String resourceGroup) {
		this.resourceGroup = resourceGroup;
	}

	public String getResourceGroup() {
		return resourceGroup;
	}

	public boolean isServicePrincipal() {
		return servicePrincipal;
	}

	public void setServicePrincipal(boolean servicePrincipal) {
		this.servicePrincipal = servicePrincipal;
	}

	protected void setProject(Project project) {
		super.project = project;
	}
}
