package org.ttrssreader.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import android.net.Uri;
import android.text.TextUtils;

// contains code from the Apache Software foundation
public class StringSupport {
    
    /**
     * Turns a camel case string into an underscored one, e.g. "HelloWorld"
     * becomes "hello_world".
     * 
     * @param camelCaseString
     *            the string to underscore
     * @return the underscored string
     */
    protected static String underscore(String camelCaseString) {
        String[] words = splitByCharacterTypeCamelCase(camelCaseString);
        return TextUtils.join("_", words).toLowerCase();
    }
    
    /**
     * <p>
     * Splits a String by Character type as returned by <code>java.lang.Character.getType(char)</code>. Groups of
     * contiguous characters of the same type are returned as complete tokens, with the following exception: the
     * character of type <code>Character.UPPERCASE_LETTER</code>, if any, immediately preceding a token of type
     * <code>Character.LOWERCASE_LETTER</code> will belong to the following token rather than to the preceding, if any,
     * <code>Character.UPPERCASE_LETTER</code> token.
     * 
     * <pre>
     * StringUtils.splitByCharacterTypeCamelCase(null)         = null
     * StringUtils.splitByCharacterTypeCamelCase("")           = []
     * StringUtils.splitByCharacterTypeCamelCase("ab de fg")   = ["ab", " ", "de", " ", "fg"]
     * StringUtils.splitByCharacterTypeCamelCase("ab   de fg") = ["ab", "   ", "de", " ", "fg"]
     * StringUtils.splitByCharacterTypeCamelCase("ab:cd:ef")   = ["ab", ":", "cd", ":", "ef"]
     * StringUtils.splitByCharacterTypeCamelCase("number5")    = ["number", "5"]
     * StringUtils.splitByCharacterTypeCamelCase("fooBar")     = ["foo", "Bar"]
     * StringUtils.splitByCharacterTypeCamelCase("foo200Bar")  = ["foo", "200", "Bar"]
     * StringUtils.splitByCharacterTypeCamelCase("ASFRules")   = ["ASF", "Rules"]
     * </pre>
     * 
     * @param str
     *            the String to split, may be <code>null</code>
     * @return an array of parsed Strings, <code>null</code> if null String
     *         input
     * @since 2.4
     */
    private static String[] splitByCharacterTypeCamelCase(String str) {
        return splitByCharacterType(str, true);
    }
    
    /**
     * <p>
     * Splits a String by Character type as returned by <code>java.lang.Character.getType(char)</code>. Groups of
     * contiguous characters of the same type are returned as complete tokens, with the following exception: if
     * <code>camelCase</code> is <code>true</code>, the character of type <code>Character.UPPERCASE_LETTER</code>, if
     * any, immediately preceding a token of type <code>Character.LOWERCASE_LETTER</code> will belong to the following
     * token rather than to the preceding, if any, <code>Character.UPPERCASE_LETTER</code> token.
     * 
     * @param str
     *            the String to split, may be <code>null</code>
     * @param camelCase
     *            whether to use so-called "camel-case" for letter types
     * @return an array of parsed Strings, <code>null</code> if null String
     *         input
     * @since 2.4
     */
    private static String[] splitByCharacterType(String str, boolean camelCase) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new String[0];
        }
        char[] c = str.toCharArray();
        ArrayList<String> list = new ArrayList<String>();
        int tokenStart = 0;
        int currentType = Character.getType(c[tokenStart]);
        for (int pos = tokenStart + 1; pos < c.length; pos++) {
            int type = Character.getType(c[pos]);
            if (type == currentType) {
                continue;
            }
            if (camelCase && type == Character.LOWERCASE_LETTER && currentType == Character.UPPERCASE_LETTER) {
                int newTokenStart = pos - 1;
                if (newTokenStart != tokenStart) {
                    list.add(new String(c, tokenStart, newTokenStart - tokenStart));
                    tokenStart = newTokenStart;
                }
            } else {
                list.add(new String(c, tokenStart, pos - tokenStart));
                tokenStart = pos;
            }
            currentType = type;
        }
        list.add(new String(c, tokenStart, c.length - tokenStart));
        return (String[]) list.toArray(new String[list.size()]);
    }
    
    /**
     * Splits the ids into Sets of Strings with maxCount ids each if configured in preferences, else only splits on 500 to
     * avoid extremely large requests.
     * 
     * @param ids
     *            the set of ids to be split
     * @param maxCount
     *            the maximum length of each list
     * @return a set of Strings with comma-separated ids
     */
    public static Set<String> convertListToString(Set<Integer> ids, int maxCount) {
        Set<String> ret = new HashSet<String>();
        Iterator<Integer> it = ids.iterator();
        StringBuilder idList = new StringBuilder();
        
        int count = 0;
        while (it.hasNext()) {
            
            Integer next = it.next();
            if (next == null)
                continue;
            
            idList.append(next);
            
            if (count == maxCount) {
                ret.add(idList.toString());
                idList = new StringBuilder();
                count = 0;
            } else {
                count++;
                if (it.hasNext())
                    idList.append(",");
            }
        }
        
        String list = idList.toString();
        
        // Should not happen, but happens anyway: Cut of leading and trailing commas
        if (list.startsWith(","))
            list = list.substring(1);
        if (list.endsWith(","))
            list = list.substring(0, list.length() - 1);
        
        ret.add(list);
        return ret;
    }
    
    public static String[] setToArray(Set<String> set) {
        String[] ret = new String[set.size()];
        int i = 0;
        for (String s : set) {
            ret[i++] = s;
        }
        return ret;
    }
    
    public static String getBaseURL(String url) {
        Uri uri = Uri.parse(url);
        if (uri != null) {
            return uri.getScheme() + "://" + uri.getAuthority();
        }
        return null;
    }
    
    public static String convertStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new BufferedInputStream(inputStream, (int)Utils.KB);
        byte[] buffer = new byte[(int)Utils.KB];
        int n = 0;
        try {
            while (-1 != (n = in.read(buffer))) {
                out.write(buffer, 0, n);
            }
        } finally {
            out.close();
            in.close();
        }
        return out.toString("UTF-8");
    }
    
}
