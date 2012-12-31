/**
 * Copyright 2012 Nick Wilson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.svcdelivery.liquibase.eclipse.internal.ui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.jface.wizard.Wizard;

import com.arjuna.ats.jta.UserTransaction;

/**
 * @author nick
 */
public class RollbackChangeSetWizard extends Wizard {

	/**
	 * The list of files to attempt to roll back.
	 */
	private ChangeSetTreeItem item;

	/**
	 * Page showing currently installed scripts.
	 */
	private RollbackSummaryPage rollbackPage;

	/**
	 * @param changeSet
	 *            A list of selected script files.
	 */
	public RollbackChangeSetWizard(final ChangeSetTreeItem changeSet) {
		item = changeSet;
	}

	@Override
	public final void addPages() {
		rollbackPage = new RollbackSummaryPage(item);
		addPage(rollbackPage);
	}

	@Override
	public final boolean performFinish() {
		boolean ok = true;
		final Job job = new Job("Rollback Liquibase Script") {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				List<ChangeSetTreeItem> rollbackList = rollbackPage
						.getRollbackList();
				monitor.beginTask("rollback", rollbackList.size());
				if (!monitor.isCanceled()) {
					String filename = null;
					IFile file = null;
					int count = 0;
					for (ChangeSetTreeItem rollback : rollbackList) {
						file = rollback.getChangeLogFile();
						String itemName = file.getName();
						if (filename == null) {
							filename = itemName;
							count++;
						} else if (!filename.equals(itemName)) {
							runScript(file, count);
							count = 0;
							filename = null;
						} else {
							count++;
						}
						monitor.worked(1);
					}
					runScript(file, count);
				}
				monitor.done();
				return Status.OK_STATUS;
			}

			private void runScript(final IFile changeLogFile, final int count) {
				final LiquibaseResult result = new LiquibaseResult();
				result.setStatus(LiquibaseResultStatus.RUNNING);
				result.setTimestamp(System.currentTimeMillis());
				result.setScript(changeLogFile.getName());
				Activator.getDefault().getResults().add(result);
				final ResourceAccessor resourceAccessor = new FileSystemResourceAccessor(
						changeLogFile.getParent().getLocation().toString());
				IConnectionProfile profile = item.getProfile();
				final Connection connection = ConnectionUtil
						.getConnection(profile);
				if (connection != null) {
					try {
						final javax.transaction.UserTransaction ut = UserTransaction
								.userTransaction();
						ut.begin();
						try {
							final DatabaseConnection database = new JdbcConnection(
									connection);
							final Liquibase lb = new Liquibase(
									changeLogFile.getName(), resourceAccessor,
									database);
							lb.rollback(count, null);
							ut.commit();
							result.setStatus(LiquibaseResultStatus.SUCCESS);
						} catch (final LiquibaseException e) {
							e.printStackTrace();
							ut.rollback();
							result.setStatus(LiquibaseResultStatus.FAILURE);
						}
					} catch (final Exception e) {
						e.printStackTrace();
						result.setStatus(LiquibaseResultStatus.FAILURE);
					} finally {
						try {
							connection.close();
						} catch (final SQLException e) {
							e.printStackTrace();
						}
					}
					// Notify change to database.
					DatabaseUpdateEvent event = new DatabaseUpdateEvent(item);
					Activator.getDefault().notifyDatabaseUpdateListeners(event);
				} else {
					System.out.println("Failed to get connection.");
					result.setStatus(LiquibaseResultStatus.FAILURE);
				}
			}

		};
		job.schedule();
		return ok;
	}
}