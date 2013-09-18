/* This file is part of the Android Clementine Remote.
 * Copyright (C) 2013, Andreas Muttscheller <asfa194@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.qspool.clementineremote.ui.adapter;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.qspool.clementineremote.App;
import de.qspool.clementineremote.R;
import de.qspool.clementineremote.backend.ClementineSongDownloader;

/**
 * Class is used for displaying the song data
 */
public class DownloadAdapter extends ArrayAdapter<ClementineSongDownloader> implements Filterable {
	private Context mContext;

	public DownloadAdapter(Context context, int resource,
			List<ClementineSongDownloader> data) {
		super(context, resource, data);
		mContext = context;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ClementineSongDownloader downloader = App.downloaders.get(position);
		
		if (convertView == null) {
			convertView = ((Activity)mContext).getLayoutInflater()
							.inflate(R.layout.download_row, parent, false);
		}

		convertView.setBackgroundResource(R.drawable.white_background);
		
		TextView tvDlTitle     = (TextView) convertView.findViewById(R.id.tvDlTitle);
		TextView tvDlSubtitle  = (TextView) convertView.findViewById(R.id.tvDlSubtitle);
		ProgressBar pbProgress = (ProgressBar) convertView.findViewById(R.id.pbDlProgress);
		ImageButton ibCancel   = (ImageButton) convertView.findViewById(R.id.ibCancelDl);
		
		ibCancel.setOnClickListener(oclCancel);
		ibCancel.setTag(downloader);
		
		pbProgress.setMax(100);
		pbProgress.setProgress(downloader.getCurrentProgress());
		tvDlTitle.setText(downloader.getTitle());
		tvDlSubtitle.setText(downloader.getSubtitle());
		
		return convertView;
	}
	
	private OnClickListener oclCancel = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			ClementineSongDownloader downloader = (ClementineSongDownloader) v.getTag();
			
			if (downloader.getStatus() == AsyncTask.Status.RUNNING) {
				downloader.cancel(true);
				Toast.makeText(mContext, R.string.download_noti_canceled, Toast.LENGTH_SHORT).show();
			} else {
				App.downloaders.remove(downloader);
			}
			
			notifyDataSetChanged();
		}
	};
}