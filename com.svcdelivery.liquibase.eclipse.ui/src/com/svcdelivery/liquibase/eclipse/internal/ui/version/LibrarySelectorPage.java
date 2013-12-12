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
package com.svcdelivery.liquibase.eclipse.internal.ui.version;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Version;

import com.svcdelivery.liquibase.eclipse.internal.ui.CollectionContentProvider;

/**
 * @author nick
 */
public class LibrarySelectorPage extends WizardPage {

	private Text url;

	private ListViewer urlList;

	private Text versionText;

	private Set<URL> urls;

	private Version version;

	/**
	 * Constructor.
	 */
	protected LibrarySelectorPage() {
		super("Select Library");
		setTitle("Select Library");
		setMessage("Enter the URL of a Liquibase jar file and specify the API version that it implements.");
		urls = new HashSet<URL>();
	}

	@Override
	public void createControl(Composite parent) {
		final Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(4, false));

		Label urlLabel = new Label(root, SWT.NONE);
		urlLabel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		urlLabel.setText("URL");
		url = new Text(root, SWT.NONE);
		url.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button browse = new Button(root, SWT.PUSH);
		browse.setText("File");
		browse.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		Button add = new Button(root, SWT.PUSH);
		add.setText("Add");
		add.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		Label urlListLabel = new Label(root, SWT.NONE);
		urlListLabel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false,
				false));
		urlListLabel.setText("URLs");
		urlList = new ListViewer(root, SWT.MULTI);
		urlList.getList().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		urlList.setContentProvider(new CollectionContentProvider());
		urlList.setInput(urls);

		Button remove = new Button(root, SWT.PUSH);
		remove.setText("Remove");
		remove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		Label versionLabel = new Label(root, SWT.NONE);
		versionLabel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false,
				false));
		versionLabel.setText("Version");
		versionText = new Text(root, SWT.NONE);
		versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 3, 1));

		add.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				setErrorMessage(null);
				String text = url.getText();
				try {
					URL lib = new URL(text);
					urls.add(lib);
					updateUrlList();
				} catch (MalformedURLException e1) {
					setErrorMessage("Invalid URL.");
				}
			}

		});
		remove.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				ISelection selection = urlList.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection) selection;
					if (ss.size() > 0) {
						urls.removeAll(ss.toList());
						updateUrlList();
					}
				}
			}
		});
		versionText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent event) {
				String txt = versionText.getText();
				if (txt != null) {
					try {
						version = new Version(txt);
					} catch (IllegalArgumentException e) {
						version = null;
					}
				}
			}
		});

		setControl(root);
	}

	public URL[] getURLs() {
		return urls.toArray(new URL[urls.size()]);
	}

	public Version getVersion() {
		return version;
	}

	private void updateUrlList() {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				urlList.refresh();
			}
		});
	}
}
