/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package com.vmware.vfabric.ide.eclipse.tcserver.internal.core;

/**
 * @author Steffen Pingel
 */
public interface IServicabilityInfo {

	public abstract JmxCredentials getCredentials(TcServer server);

	public abstract String getHost();

	public abstract String getPort();

	public abstract boolean isValid();

}
