/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.safercript.mygoogledriveeditor.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.safercript.mygoogledriveeditor.R;
import com.safercript.mygoogledriveeditor.entity.FileDataIdAndName;

/**
 * A DataBufferAdapter to display the results of file listing/querying requests.
 */
public class ResultsAdapter extends ArrayAdapter<FileDataIdAndName> {

    private ResultsAdapter.OnClickListenerAdapter onClickListenerAdapter;

    public ResultsAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(getContext(),
                    R.layout.file_list_item, null);
        }
        FileDataIdAndName metadata = getItem(position);
        TextView titleTextView = (TextView) convertView.findViewById(R.id.textView);
        titleTextView.setText(metadata.getNameFile() + "");
        titleTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickListenerAdapter.onClick(getItem(position));
            }
        });

        ImageView imageDelete = (ImageView) convertView.findViewById(R.id.imageDelete);
        imageDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickListenerAdapter.onClickDelete(getItem(position));
            }
        });
        if (metadata.getId().equals("1")){
            imageDelete.setVisibility(View.GONE);
        }
        return convertView;
    }
    // add onClickListener
    public void setOnClickListenerAdapter(ResultsAdapter.OnClickListenerAdapter onClickListenerAdapter) {
        this.onClickListenerAdapter = onClickListenerAdapter;
    }
    // interface for connection with activity
    public interface OnClickListenerAdapter {
        void onClick(FileDataIdAndName metadata);
        void onClickDelete(FileDataIdAndName metadata);
    }
}