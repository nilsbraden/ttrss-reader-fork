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

import org.ttrssreader.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ChangelogDialog extends MyDialogFragment {

    public static ChangelogDialog getInstance() {
        return new ChangelogDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setTitle(getResources().getString(R.string.Changelog_Title));
        final String[] changes = getResources().getStringArray(R.array.updates);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < changes.length; i++) {
            sb.append("\n\n");
            sb.append(changes[i]);
            if (sb.length() > 4000) // Don't include all messages, nobody reads the old stuff anyway
                break;
        }
        builder.setMessage(sb.toString().trim());
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton((String) getText(R.string.CategoryActivity_Donate),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface d, final int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(
                                R.string.DonateUrl))));
                        d.dismiss();
                    }
                });

        return builder.create();
    }

}
