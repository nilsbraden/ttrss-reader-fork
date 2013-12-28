package org.ttrssreader.model;

import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.CategoryCursorHelper.MemoryDBOpenHelper;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class ListContentProvider extends ContentProvider {
    
    private OpenHelper dbOpenHelper;
    
    private static final String AUTHORITY = "org.ttrssreader";
    
    // Uri path segments
    private static final int CATS = 1;
    private static final int FEEDS = 2;
    private static final int HEADLINES = 3;
    
    // Params
    public static final String PARAM_CAT_ID = "categoryId";
    public static final String PARAM_FEED_ID = "feedId";
    public static final String PARAM_SELECT_FOR_CAT = "selectArticlesForCategory";
    
    // Public information:
    private static final String BASE_PATH_CATEGORIES = "categories";
    private static final String BASE_PATH_FEEDS = "feeds";
    private static final String BASE_PATH_HEADLINES = "headlines";
    
    public static final Uri CONTENT_URI_CAT = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_CATEGORIES);
    public static final Uri CONTENT_URI_FEED = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_FEEDS);
    public static final Uri CONTENT_URI_HEAD = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_HEADLINES);
    
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/listitems";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/listitem";
    
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        // Request all items:
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_CATEGORIES, CATS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_FEEDS, FEEDS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_HEADLINES, HEADLINES);
    }
    
    @Override
    public boolean onCreate() {
        dbOpenHelper = new OpenHelper(getContext(), DBHelper.DATABASE_NAME, DBHelper.DATABASE_VERSION);
        return false;
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        
        // Parse parameters:
        int categoryId = -1;
        int feedId = -1;
        boolean selectArticlesForCategory = false;
        
        String paramCat = uri.getQueryParameter(PARAM_CAT_ID);
        if (paramCat != null)
            categoryId = Integer.parseInt(paramCat);
        
        String paramFeedId = uri.getQueryParameter(PARAM_FEED_ID);
        if (paramFeedId != null)
            feedId = Integer.parseInt(paramFeedId);
        
        String paramSelectArticles = uri.getQueryParameter(PARAM_SELECT_FOR_CAT);
        if (paramSelectArticles != null)
            selectArticlesForCategory = ("1".equals(paramSelectArticles));
        
        // Retrieve CursorHelper:
        MainCursorHelper cursorHelper = null;
        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case CATS: {
                MemoryDBOpenHelper memoryDbOpenHelper = new MemoryDBOpenHelper(getContext());
                SQLiteDatabase memoryDb = memoryDbOpenHelper.getWritableDatabase();
                cursorHelper = new CategoryCursorHelper(getContext(), memoryDb);
                break;
            }
            case FEEDS:
                cursorHelper = new FeedCursorHelper(getContext(), categoryId);
                break;
            case HEADLINES:
                cursorHelper = new FeedHeadlineCursorHelper(getContext(), feedId, categoryId, selectArticlesForCategory);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        // Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        Cursor cursor = cursorHelper.makeQuery(db);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        
        return cursor;
    }
    
    @Override
    public String getType(Uri uri) {
        return null;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new NoSuchMethodError(); // Not implemented!
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new NoSuchMethodError(); // Not implemented!
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new NoSuchMethodError(); // Not implemented!
    }
    
    class OpenHelper extends SQLiteOpenHelper {
        public OpenHelper(Context context, String databaseName, int databaseVersion) {
            super(context, databaseName, null, databaseVersion);
        }
        
        @Override
        public void onCreate(SQLiteDatabase database) {
            throw new NoSuchMethodError(); // Not implemented!
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            throw new NoSuchMethodError(); // Not implemented!
        }
    }
    
}
