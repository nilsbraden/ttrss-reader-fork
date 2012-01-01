package org.ttrssreader.preferences;

import java.io.File;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;

/**
 * Copied from https://code.google.com/p/k9mail/
 */
public class FileBrowserHelper {
    /**
     * A string array that specifies the name of the intent to use, and the scheme to use with it
     * when setting the data for the intent.
     */
    private static final String[][] PICK_DIRECTORY_INTENTS = { { "org.openintents.action.PICK_DIRECTORY", "file://" }, // OI
                                                                                                                       // File
                                                                                                                       // Manager
            { "com.estrongs.action.PICK_DIRECTORY", "file://" }, // ES File Explorer
            { Intent.ACTION_PICK, "folder://" }, // Blackmoon File Browser (maybe others)
            { "com.androidworkz.action.PICK_DIRECTORY", "file://" } }; // SystemExplorer
    
    private static FileBrowserHelper instance;
    
    /**
     * callbackDownloadPath class to provide the result of the fallback textedit path dialog
     */
    public interface FileBrowserFailOverCallback {
        /**
         * the user has entered a path
         * 
         * @param path
         *            the path as String
         */
        public void onPathEntered(String path);
        
        /**
         * the user has cancel the inputtext dialog
         */
        public void onCancel();
    }
    
    // Singleton
    private FileBrowserHelper() {
    }
    
    public synchronized static FileBrowserHelper getInstance() {
        if (instance == null) {
            instance = new FileBrowserHelper();
        }
        return instance;
    }
    
    /**
     * tries to open known filebrowsers.
     * If no filebrowser is found and fallback textdialog is shown
     * 
     * @param c
     *            the context as activity
     * @param startPath
     *            : the default value, where the filebrowser will start.
     *            if startPath = null => the default path is used
     * @param requestcode
     *            : the int you will get as requestcode in onActivityResult
     *            (only used if there is a filebrowser installed)
     * @param callbackDownloadPath
     *            : the callbackDownloadPath (only used when no filebrowser is installed.
     *            if a filebrowser is installed => override the onActivtyResult Method
     * 
     * @return true: if a filebrowser has been found (the result will be in the onActivityResult
     *         false: a fallback textinput has been shown. The Result will be sent with the callbackDownloadPath method
     * 
     * 
     */
    public boolean showFileBrowserActivity(Activity c, File startPath, int requestcode, FileBrowserFailOverCallback callback) {
        boolean success = false;
        
        if (startPath == null) {
            startPath = new File(Controller.getInstance().saveAttachmentPath());
        }
        
        int listIndex = 0;
        do {
            String intentAction = PICK_DIRECTORY_INTENTS[listIndex][0];
            String uriPrefix = PICK_DIRECTORY_INTENTS[listIndex][1];
            Intent intent = new Intent(intentAction);
            intent.setData(Uri.parse(uriPrefix + startPath.getPath()));
            
            try {
                c.startActivityForResult(intent, requestcode);
                success = true;
            } catch (ActivityNotFoundException e) {
                // Try the next intent in the list
                listIndex++;
            };
        } while (!success && (listIndex < PICK_DIRECTORY_INTENTS.length));
        
        if (listIndex == PICK_DIRECTORY_INTENTS.length) {
            // No Filebrowser is installed => show a fallback textdialog
            showPathTextInput(c, startPath, callback);
            success = false;
        }
        
        return success;
    }
    
    private void showPathTextInput(final Activity c, final File startPath, final FileBrowserFailOverCallback callback) {
        AlertDialog.Builder alert = new AlertDialog.Builder(c);
        
        alert.setTitle(c.getString(R.string.Utils_FileSaveTitle));
        alert.setMessage(c.getString(R.string.Utils_FileSaveMessage));
        final EditText input = new EditText(c);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (startPath != null)
            input.setText(startPath.toString());
        alert.setView(input);
        
        alert.setPositiveButton(c.getString(R.string.Utils_OkayAction), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String path = input.getText().toString();
                callback.onPathEntered(path);
            }
        });
        
        alert.setNegativeButton(c.getString(R.string.Utils_CancelAction), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                callback.onCancel();
            }
        });
        
        alert.show();
    }
}
