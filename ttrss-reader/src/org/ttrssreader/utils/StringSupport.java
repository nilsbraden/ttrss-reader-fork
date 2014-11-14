package org.ttrssreader.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

// contains code from the Apache Software foundation
public class StringSupport {
    
    /**
     * Splits the ids into Sets of Strings with maxCount ids each.
     * 
     * @param ids
     *            the set of ids to be split
     * @param maxCount
     *            the maximum length of each list
     * @return a set of Strings with comma-separated ids
     */
    public static <T> Set<String> convertListToString(Collection<T> values, int maxCount) {
        Set<String> ret = new HashSet<String>();
        if (values == null || values.isEmpty())
            return ret;
        
        StringBuilder sb = new StringBuilder();
        int count = 0;
        
        Iterator<T> it = values.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o == null)
                continue;
            
            sb.append(o);
            
            if (count == maxCount) {
                ret.add(sb.substring(0, sb.length() - 1));
                sb = new StringBuilder();
                count = 0;
            } else {
                sb.append(",");
                count++;
            }
        }
        
        if (sb.length() > 0)
            ret.add(sb.substring(0, sb.length() - 1));
        
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
    
}
