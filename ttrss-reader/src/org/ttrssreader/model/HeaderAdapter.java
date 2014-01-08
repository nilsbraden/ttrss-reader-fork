package org.ttrssreader.model;

import java.util.List;
import org.ttrssreader.R;
import android.content.Context;
import android.preference.PreferenceActivity.Header;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * <a href=
 * "http://stackoverflow.com/questions/15551673/android-headers-categories-in-preferenceactivity-with-preferencefragment"
 * >stackoverflow.com</a>
 */
public class HeaderAdapter extends ArrayAdapter<Header> {
    
    static final int HEADER_TYPE_CATEGORY = 0;
    static final int HEADER_TYPE_NORMAL = 1;
    private static final int HEADER_TYPE_COUNT = HEADER_TYPE_NORMAL + 1;
    
    private LayoutInflater mInflater;
    
    private static class HeaderViewHolder {
        ImageView icon;
        TextView title;
        TextView summary;
    }
    
    public HeaderAdapter(Context context, List<Header> objects) {
        
        super(context, 0, objects);
        
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    static int getHeaderType(Header header) {
        
        if (header.fragment == null && header.intent == null)
            return HEADER_TYPE_CATEGORY;
        else
            return HEADER_TYPE_NORMAL;
    }
    
    @Override
    public int getItemViewType(int position) {
        Header header = getItem(position);
        return getHeaderType(header);
    }
    
    @Override
    public boolean areAllItemsEnabled() {
        return false; /* because of categories */
    }
    
    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != HEADER_TYPE_CATEGORY;
    }
    
    @Override
    public int getViewTypeCount() {
        return HEADER_TYPE_COUNT;
    }
    
    @Override
    public boolean hasStableIds() {
        return true;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
        HeaderViewHolder holder;
        Header header = getItem(position);
        int headerType = getHeaderType(header);
        View view = null;
        
        if (convertView == null) {
            
            holder = new HeaderViewHolder();
            
            switch (headerType) {
            
                case HEADER_TYPE_CATEGORY:
                    
                    view = new TextView(getContext(), null, android.R.attr.listSeparatorTextViewStyle);
                    holder.title = (TextView) view;
                    break;
                
                case HEADER_TYPE_NORMAL:
                    
                    view = mInflater.inflate(R.layout.item_preferenceheader, parent, false);
                    holder.icon = (ImageView) view.findViewById(R.id.ph_icon);
                    holder.title = (TextView) view.findViewById(R.id.ph_title);
                    holder.summary = (TextView) view.findViewById(R.id.ph_summary);
                    break;
            }
            
            view.setTag(holder);
        } else {
            
            view = convertView;
            holder = (HeaderViewHolder) view.getTag();
        }
        
        // All view fields must be updated every time, because the view may be recycled
        switch (headerType) {
        
            case HEADER_TYPE_CATEGORY:
                
                holder.title.setText(header.getTitle(getContext().getResources()));
                break;
            
            case HEADER_TYPE_NORMAL:
                
                holder.icon.setImageResource(header.iconRes);
                
                holder.title.setText(header.getTitle(getContext().getResources()));
                CharSequence summary = header.getSummary(getContext().getResources());
                
                if (!TextUtils.isEmpty(summary)) {
                    
                    holder.summary.setVisibility(View.VISIBLE);
                    holder.summary.setText(summary);
                } else {
                    holder.summary.setVisibility(View.GONE);
                }
                break;
        }
        
        return view;
    }
}
