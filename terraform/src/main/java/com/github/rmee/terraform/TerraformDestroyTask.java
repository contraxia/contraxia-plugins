package com.github.rmee.terraform;

public class TerraformDestroyTask extends TerraformExec {

	public TerraformDestroyTask() {
		retrieveSpec().setAddVariables(false);
		retrieveSpec().setAddConfigDirectory(false);
		setCommandLine("destroy -auto-approve");
	}
}
