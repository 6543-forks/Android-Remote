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

package de.qspool.clementineremote.ui.fragments;

import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import de.qspool.clementineremote.App;
import de.qspool.clementineremote.R;
import de.qspool.clementineremote.backend.Clementine;
import de.qspool.clementineremote.backend.player.MySong;
import de.qspool.clementineremote.backend.requests.RequestControl;
import de.qspool.clementineremote.backend.requests.RequestControl.Request;
import de.qspool.clementineremote.utils.Utilities;


public class PlayerFragment extends SherlockFragment {
	private TextView mTvArtist;
	private TextView mTvTitle;
	private TextView mTvAlbum;
	
	private TextView mTvGenre;
	private TextView mTvYear;
	private TextView mTvLength;
	
	private ImageButton mBtnNext;
	private ImageButton mBtnPrev;
	private ImageButton mBtnPlayPause;
	
	private ImageView mImgArt;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
		      Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.player_fragment,
				container, false);
    	
	    // Get the Views
	    mTvArtist = (TextView) view.findViewById(R.id.tvArtist);
	    mTvTitle  = (TextView) view.findViewById(R.id.tvTitle);
	    mTvAlbum  = (TextView) view.findViewById(R.id.tvAlbum);
	    
	    mTvGenre  = (TextView) view.findViewById(R.id.tvGenre);
	    mTvYear   = (TextView) view.findViewById(R.id.tvYear);
	    mTvLength = (TextView) view.findViewById(R.id.tvLength);
	    
	    mBtnNext  = (ImageButton) view.findViewById(R.id.btnNext);
	    mBtnPrev  = (ImageButton) view.findViewById(R.id.btnPrev);
	    mBtnPlayPause  = (ImageButton) view.findViewById(R.id.btnPlaypause);
	    
	    mImgArt = (ImageView) view.findViewById(R.id.imgArt);

	    // Set the onclicklistener for the buttons
	    mBtnNext.setOnClickListener(oclControl);
	    mBtnPrev.setOnClickListener(oclControl);
	    mBtnPlayPause.setOnClickListener(oclControl);
	    
	    reloadInfo();
	    
	    return view;
	}
    
	/**
	 * Reload the player ui
	 */
	public void reloadInfo() {
    	// display play / pause image
    	if (App.mClementine.getState() == Clementine.State.PLAY) {
    		mBtnPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_pause));
    	} else {
    		mBtnPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_play));
    	}
    	
    	// Get the currently played song
    	MySong currentSong = App.mClementine.getCurrentSong();
    	if (currentSong == null) {
    		// If none is played right now, show a text and the clementine icon
    		mTvArtist.setText(getString(R.string.player_nosong));
	    	mTvTitle. setText("");
	    	mTvAlbum. setText("");
	    	
	    	mTvGenre. setText("");
	    	mTvYear.  setText("");
	    	mTvLength.setText("");
	    	
    		mImgArt.setImageResource(R.drawable.icon_large);
    	} else {
	    	mTvArtist.setText(currentSong.getArtist());
	    	mTvTitle. setText(currentSong.getTitle());
	    	mTvAlbum. setText(currentSong.getAlbum());
	    	
	    	mTvGenre. setText(currentSong.getGenre());
	    	mTvYear.  setText(currentSong.getYear());
	    	mTvLength.setText(buildTrackPosition());
	    	
	    	// Check if a coverart is valid
	    	if (currentSong.getArt() == null) {
	    		mImgArt.setImageResource(R.drawable.icon_large);
	    	} else {
	    		mImgArt.setImageBitmap(currentSong.getArt());
	    	}
    	}
    }
    
    private String buildTrackPosition() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(Utilities.PrettyTime(App.mClementine.getSongPosition()));
    	sb.append("/");
    	sb.append(Utilities.PrettyTime(App.mClementine.getCurrentSong().getLength()));
    	
    	return sb.toString();
    }
	
	private OnClickListener oclControl = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Message msg = Message.obtain();
			
			switch(v.getId()) {
			case R.id.btnNext: msg.obj = new RequestControl(Request.NEXT);
							   break;
			case R.id.btnPrev: msg.obj = new RequestControl(Request.PREV);
							   break;
			case R.id.btnPlaypause: msg.obj = new RequestControl(Request.PLAYPAUSE);
								break;
		    default: break;
			}
			// Send the request to the thread
			App.mClementineConnection.mHandler.sendMessage(msg);
		}
	};
}
