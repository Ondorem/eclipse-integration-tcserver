/*******************************************************************************
 * Copyright (c) 2013, 2020 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package com.vmware.vfabric.ide.eclipse.tcserver.insight.internal.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.server.tomcat.core.internal.ITomcatServer;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.springsource.ide.eclipse.commons.configurator.ServerHandler;
import org.springsource.ide.eclipse.commons.configurator.ServerHandlerCallback;

import com.vmware.vfabric.ide.eclipse.tcserver.internal.core.TcServer;
import com.vmware.vfabric.ide.eclipse.tcserver.internal.core.TcServerUtil;
import com.vmware.vfabric.ide.eclipse.tcserver.tests.support.TcServerFixture;
import com.vmware.vfabric.ide.eclipse.tcserver.tests.support.TcServerTestPlugin;

/**
 * @author Leo Dos Santos
 */
public class InsightTestFixture extends TcServerFixture {

	public static InsightTestFixture V_2_8 = new InsightTestFixture(TcServerTestPlugin.PLUGIN_ID,
			TcServerFixture.ID_TC_SERVER_2_5, "vfabric-tc-server-developer-2.8.0.RELEASE", V_2_8_URL);

	public static InsightTestFixture V_2_9 = new InsightTestFixture(TcServerTestPlugin.PLUGIN_ID,
			TcServerFixture.ID_TC_SERVER_2_5, "vfabric-tc-server-developer-2.9.3.RELEASE", V_2_9_URL);

	public static InsightTestFixture V_3_0 = new InsightTestFixture(TcServerTestPlugin.PLUGIN_ID,
			TcServerFixture.ID_TC_SERVER_3_0, "pivotal-tc-server-developer-3.0.1.RELEASE", V_3_0_URL);

	public static InsightTestFixture V_3_1 = new InsightTestFixture(TcServerTestPlugin.PLUGIN_ID,
			TcServerFixture.ID_TC_SERVER_3_0, "pivotal-tc-server-developer-3.1.0.RELEASE", V_3_1_URL);
	
	public InsightTestFixture(String testPlugin, String serverType, String stubPath, String downloadUrl) {
		super(testPlugin, serverType, stubPath, downloadUrl);
	}

	@Override
	public IServer createServer(String instance) throws Exception {
		ServerHandler handler = provisionServer();
		return handler.createServer(new NullProgressMonitor(), ServerHandler.ALWAYS_OVERWRITE,
				new ServerHandlerCallback() {
					@Override
					public void configureServer(IServerWorkingCopy wc) throws CoreException {
						String DEFAULT_INSTANCE = "insight-instance";
						// Create a default instance in case that one is missing
						IPath installLocation = wc.getRuntime().getLocation();
						if (!installLocation.append(DEFAULT_INSTANCE).toFile().exists()) {
							String[] arguments = new String[] { "create", DEFAULT_INSTANCE, "-t", "insight", "--force" };
							TcServerUtil.executeInstanceCreation(wc.getRuntime(), DEFAULT_INSTANCE, arguments);
						}
						wc.setAttribute(ITomcatServer.PROPERTY_INSTANCE_DIR, (String) null);
						wc.setAttribute(ITomcatServer.PROPERTY_TEST_ENVIRONMENT, false);
						wc.setAttribute(TcServer.KEY_ASF_LAYOUT, false);
						wc.setAttribute(TcServer.KEY_SERVER_NAME, DEFAULT_INSTANCE);
						TcServerUtil.importRuntimeConfiguration(wc, null);

						TcServer tcServer = (TcServer) ((ServerWorkingCopy) wc).getWorkingCopyDelegate(null);
						new ModifyInsightVmArgsCommand(tcServer, true).execute();
					}
				});
	}

}
