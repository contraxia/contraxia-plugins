package com.github.rmee.az;

import java.io.File;

import com.github.rmee.az.aks.AzGetKubernetesCredentialsTask;
import com.github.rmee.common.Client;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AzPlugin implements Plugin<Project> {

	public void apply(Project project) {
		File azureConfigDir = new File(project.getBuildDir(), ".azure");

		AzExtension extension = project.getExtensions().create("az", AzExtension.class);
		extension.setProject(project);
		// TODO azure-cli image insufficient by default: https://github.com/Azure/AKS/issues/469
		//extension.getClient().setImageName("microsoft/azure-cli");
		extension.getClient().setImageName("remmeier/azure-cli-kubectl");
		extension.getClient().setVersion("2.0.38");

		AzLoginTask login = project.getTasks().create("azLogin", AzLoginTask.class);
		AzGetKubernetesCredentialsTask getCredentials =
				project.getTasks().create("azGetKubernetesCredentials", AzGetKubernetesCredentialsTask.class);
		getCredentials.dependsOn(login);

		extension.setSubscriptionId(System.getenv("AZ_SUBSCRIPTION_ID"));
		extension.setServicePrincipal(Boolean.parseBoolean(System.getenv("AZ_SERVICE_PRINCIPLE")));
		extension.setUserName(System.getenv("AZ_USER"));
		extension.setPassword(System.getenv("AZ_PASS"));
		extension.setTenantId(System.getenv("AZ_TENANT_ID"));
		extension.getAks().setKubeDir(new File(project.getRootProject().getProjectDir(), "build/.kube"));

		project.afterEvaluate(project1 -> {
			Client client = extension.getClient();
			client.getVolumeMappings().put("/root/.azure/", azureConfigDir);
			client.getVolumeMappings().put("/root/.kube/", extension.getAks().getKubeDir());
			client.setupWrapper(project);
		});
	}
}




