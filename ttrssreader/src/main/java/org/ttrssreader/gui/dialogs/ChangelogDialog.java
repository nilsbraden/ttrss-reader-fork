/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.gui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import org.stringtemplate.v4.ST;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;

import androidx.annotation.NonNull;

public class ChangelogDialog extends MyDialogFragment {

	public static ChangelogDialog getInstance() {
		return new ChangelogDialog();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		View view = View.inflate(getActivity(), R.layout.changelog, null);
		WebView webView = (WebView) view.findViewById(R.id.changelog);
		webView.getSettings().setTextZoom(Controller.getInstance().textZoom());

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, null);
		builder.setNeutralButton(getString(R.string.CategoryActivity_Donate), (d, which) -> {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.DonateUrl))));
			d.dismiss();
		});

		final String[] changes = getResources().getStringArray(R.array.updates);
		final String changelogUrl = getResources().getString(R.string.ChangelogUrl);
		final StringBuilder htmlBuf = new StringBuilder();

		htmlBuf.append(getString(R.string.Changelog_Prefix, changelogUrl));
		for (String change : changes) {
			htmlBuf.append("<div><h2>");
			change = change.replaceFirst(" \\* ", "</h2><ul><li>");
			change = change.replaceAll(" \\* ", "</li><li>");
			change = change.replaceAll("___B___", "</b>");
			change = change.replaceAll("__B__", "<b>");
			htmlBuf.append(change);
			htmlBuf.append("</li></ul></div>");
		}
		htmlBuf.append(getString(R.string.Changelog_Suffix, changelogUrl));

		final ST htmlTmpl = new ST(getString(R.string.HTML_TEMPLATE_CHANGELOG), '$', '$');
		htmlTmpl.add("STYLE", getResources().getString(R.string.STYLE_TEMPLATE));
		htmlTmpl.add("TEXT_ALIGN", getString(R.string.ALIGN_JUSTIFY));
		htmlTmpl.add("THEME", getResources().getString(Controller.getInstance().getThemeHTML(getActivity())));
		htmlTmpl.add("TITLE", getResources().getString(R.string.Changelog_Title));
		htmlTmpl.add("CONTENT", htmlBuf.toString());
		webView.loadDataWithBaseURL("file:///android_asset/", htmlTmpl.render(), "text/html", "utf-8", null);

		return builder.create();
	}

}
