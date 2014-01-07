package org.ttrssreader.utils;

import java.io.Serializable;
import java.util.Comparator;
import org.ttrssreader.model.pojos.Label;

@SuppressWarnings("serial")
public class LabelTitleComparator implements Comparator<Label>, Serializable {
    
    public static final Comparator<Label> LABELTITLE_COMPARATOR = new LabelTitleComparator();
    
    public int compare(Label obj1, Label obj2) {
        if (obj1 == null || obj2 == null)
            throw new NullPointerException();
        return obj1.caption.compareTo(obj2.caption);
    }
    
}
